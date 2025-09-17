package com.example.fasse_back.fasse.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fasse_back.foodcomposition.service.FoodCompositionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class FoodCompositionController {
    private static final Logger log = LoggerFactory.getLogger(FoodCompositionController.class);
    private static ObjectMapper objectMapper = new ObjectMapper();
    private final FoodCompositionService foodCompositionService;

    @GetMapping("/foodcompositions")
    public String getFoodcompositions() throws JsonProcessingException {
        log.info(FoodCompositionController.class.toString() + ": getFoodcompositions()");

        return objectMapper.writeValueAsString(foodCompositionService.findAll());
    }
}
