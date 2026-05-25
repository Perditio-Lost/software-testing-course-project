package com.example.mapper;

import com.example.entity.TestClazz;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TestClazzMapper {

    int insert(TestClazz testClazz);

    List<TestClazz> selectByTestId(Integer testId);

    List<TestClazz> selectByClazzId(Integer clazzId);

    @Delete("DELETE FROM test_clazz WHERE test_id = #{testId}")
    int deleteByTestId(Integer testId);

    @Delete("DELETE FROM test_clazz WHERE test_id = #{testId} AND clazz_id = #{clazzId}")
    int deleteByTestIdAndClazzId(@Param("testId") Integer testId, @Param("clazzId") Integer clazzId);
}
