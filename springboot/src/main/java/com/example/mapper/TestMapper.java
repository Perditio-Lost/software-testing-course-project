package com.example.mapper;

import com.example.entity.Test;

import java.util.List;

public interface TestMapper {

    List<Test> selectAll(Test test);

    Test selectById(Integer id);

    int insert(Test test);

    void updateById(Test test);

    void deleteById(Integer id);

    List<Test> selectByTestPaperId(Integer testPaperId);
}
