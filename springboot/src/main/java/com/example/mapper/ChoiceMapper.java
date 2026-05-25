package com.example.mapper;

import com.example.entity.Choice;

import java.util.List;

public interface ChoiceMapper {

    int insert(Choice choice);

    void updateById(Choice choice);

    void deleteByQuestionId(Integer questionId);

    List<Choice> selectByQuestionId(Integer questionId);

}
