package com.example.fasse_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FasseBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(FasseBackApplication.class, args);
    }

}
