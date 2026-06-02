package com.nod;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NodApplication {
    public static void main(String[] args) {
        SpringApplication.run(NodApplication.class, args);
    }
}
