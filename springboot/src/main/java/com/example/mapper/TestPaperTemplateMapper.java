package com.example.mapper;

import com.example.entity.TestPaperTemplate;

import java.util.List;

public interface TestPaperTemplateMapper {

    int insert(TestPaperTemplate template);

    void updateById(TestPaperTemplate template);

    void deleteById(Integer id);

    TestPaperTemplate selectById(Integer id);

    List<TestPaperTemplate> selectAll(TestPaperTemplate template);
}

