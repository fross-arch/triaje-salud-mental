package com.Cesde.triaje;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TriajeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TriajeApplication.class, args);
    }
}
