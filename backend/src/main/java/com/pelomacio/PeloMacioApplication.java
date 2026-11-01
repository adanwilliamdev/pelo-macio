package com.pelomacio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PeloMacioApplication {
    public static void main(String[] args) {
        SpringApplication.run(PeloMacioApplication.class, args);
    }
}