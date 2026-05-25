package com.example.mapper;

import com.example.entity.TestPaper;

import java.util.List;

public interface TestPaperMapper {

    int insert(TestPaper testPaper);

    void updateById(TestPaper testPaper);

    void deleteById(Integer id);

    TestPaper selectById(Integer id);

    List<TestPaper> selectAll(TestPaper testPaper);

}
