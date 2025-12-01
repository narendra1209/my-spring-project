package com.iisl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.iisl")
public class IislApplication {

    public static void main(String[] args) {
        SpringApplication.run(IislApplication.class, args);
    }
}
