package com.memoraai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MemoraAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemoraAiApplication.class, args);
    }
}

