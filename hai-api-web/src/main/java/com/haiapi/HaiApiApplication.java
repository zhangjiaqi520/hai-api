package com.haiapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class HaiApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(HaiApiApplication.class, args);
    }
}
