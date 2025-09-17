package com.example.fasse_back.foodcomposition.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.fasse_back.foodcomposition.model.FoodComposition;

@Mapper
public interface FoodCompositionMapper {
    List<FoodComposition> findAll();
}
