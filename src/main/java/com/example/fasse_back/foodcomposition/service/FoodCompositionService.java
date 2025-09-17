package com.example.fasse_back.foodcomposition.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fasse_back.foodcomposition.model.FoodComposition;
import com.example.fasse_back.foodcomposition.repository.FoodCompositionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FoodCompositionService {
    private final FoodCompositionRepository foodCompositionRepository;

    @Transactional(readOnly = true)
    public List<FoodComposition> findAll() {
        return foodCompositionRepository.findAll();
    }
}
