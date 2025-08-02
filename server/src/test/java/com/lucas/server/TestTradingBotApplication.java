package com.lucas.server;

import org.springframework.boot.SpringApplication;

public class TestTradingBotApplication {

    public static void main(String[] args) {
        SpringApplication.from(ServerApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
