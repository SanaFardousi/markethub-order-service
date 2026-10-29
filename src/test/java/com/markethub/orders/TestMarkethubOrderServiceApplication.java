package com.markethub.orders;

import org.springframework.boot.SpringApplication;

public class TestMarkethubOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(MarkethubOrderServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
