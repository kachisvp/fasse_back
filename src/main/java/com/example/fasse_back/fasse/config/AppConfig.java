package com.example.fasse_back.fasse.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.example.fasse_back.foodcomposition.mapper")
@MapperScan("com.example.fasse_back.user.mapper")
public class AppConfig {

}
