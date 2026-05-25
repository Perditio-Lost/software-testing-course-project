package com.example.mapper;

import com.example.entity.QuestionType;

import java.util.List;

public interface QuestionTypeMapper {

    List<QuestionType> selectAll();

    QuestionType selectById(Integer id);

    int insert(QuestionType questionType);

    void updateById(QuestionType questionType);

    void deleteById(Integer id);
}
