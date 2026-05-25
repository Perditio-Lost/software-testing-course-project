package com.example.mapper;

import com.example.entity.Answer;

import java.util.List;

public interface AnswerMapper {

    int insert(Answer answer);

    void updateById(Answer answer);

    void deleteByScoreId(Integer scoreId);

    List<Answer> selectByScoreId(Integer scoreId);

    List<Answer> selectBatchByScoreIds(@org.apache.ibatis.annotations.Param("scoreIds") List<Integer> scoreIds);
}
