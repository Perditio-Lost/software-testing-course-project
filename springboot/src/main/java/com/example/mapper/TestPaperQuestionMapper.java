package com.example.mapper;

import com.example.entity.TestPaperQuestion;

import java.util.List;

public interface TestPaperQuestionMapper {

    List<TestPaperQuestion> selectByTestPaperId(Integer testPaperId);
    
    List<TestPaperQuestion> selectByQuestionId(Integer questionId);

    TestPaperQuestion selectById(Integer id);

    int insert(TestPaperQuestion testPaperQuestion);

    void updateById(TestPaperQuestion testPaperQuestion);

    void deleteByTestPaperId(Integer testPaperId);

    void deleteById(Integer id);
}
