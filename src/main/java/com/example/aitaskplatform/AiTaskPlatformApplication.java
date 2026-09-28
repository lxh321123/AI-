package com.example.aitaskplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.example.aitaskplatform.mapper")
@SpringBootApplication
public class AiTaskPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiTaskPlatformApplication.class, args);
    }
}
