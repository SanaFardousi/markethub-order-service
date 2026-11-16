# syntax=docker/dockerfile:1

# ---- Stage 1: build the jar and split it into layers ----
FROM maven:3.9.9-eclipse-temurin-21 AS build

# Optional corporate root certificate.
# On a network that inspects HTTPS (a company device), put the network's root certificate in the
# project root as company-root-ca.cer and Maven will trust it. On a normal network, do nothing.
# The [r] pattern lets the build continue when the file is missing; pom.xml is copied along only
# because a COPY must match at least one file. An empty file (e.g. an unset CI secret) is skipped too.
COPY pom.xml company-root-ca.ce[r] /tmp/build-certs/
RUN if [ -s /tmp/build-certs/company-root-ca.cer ]; then \
        cp /tmp/build-certs/company-root-ca.cer /usr/local/share/ca-certificates/company-root-ca.crt \
     && update-ca-certificates \
     && keytool -importcert -noprompt -trustcacerts \
        -alias company-root-ca \
        -file /usr/local/share/ca-certificates/company-root-ca.crt \
        -keystore $JAVA_HOME/lib/security/cacerts \
        -storepass changeit; \
    else \
        echo "No company-root-ca.cer found: using the default trusted certificates"; \
    fi \
 && rm -rf /tmp/build-certs
WORKDIR /workspace
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B dependency:go-offline
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package \
 && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination /workspace/extracted

# ---- Stage 2: runtime image ----
# JDK, not JRE: Drools compiles the rules at startup and needs the JDK's Java compiler.
FROM eclipse-temurin:21-jdk AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/extracted/dependencies/ ./
COPY --from=build --chown=app:app /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /workspace/extracted/application/ ./
USER app
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
