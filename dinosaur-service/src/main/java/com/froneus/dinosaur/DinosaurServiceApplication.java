package com.froneus.dinosaur;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DinosaurServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DinosaurServiceApplication.class, args);
    }
}
