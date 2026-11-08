package com.markethub.orders;

import org.testcontainers.mysql.MySQLContainer;

public final class SharedMySql {

    public static final MySQLContainer INSTANCE = new MySQLContainer("mysql:8.4");

    static {
        INSTANCE.start();
    }

    private SharedMySql() {}
}
