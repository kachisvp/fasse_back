package com.example.fasse_back.fasse.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fasse_back.user.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private static ObjectMapper objectMapper = new ObjectMapper();
    private final UserService userService;

    @GetMapping("/users")
    public String findAll() throws JsonProcessingException {
        log.info(UserController.class.toString() + ": findAll()");

        return objectMapper.writeValueAsString(userService.findAll());
    }
}
