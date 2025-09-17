package com.example.fasse_back.foodcomposition.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.example.fasse_back.foodcomposition.mapper.FoodCompositionMapper;
import com.example.fasse_back.foodcomposition.model.FoodComposition;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FoodCompositionRepository {

    private final FoodCompositionMapper foodCompositionMapper;

    public List<FoodComposition> findAll() {
        return foodCompositionMapper.findAll();
    }
}
