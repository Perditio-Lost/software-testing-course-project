package com.example.mapper;

import com.example.entity.StudentClazz;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StudentClazzMapper {

    List<StudentClazz> selectByStudentId(Integer studentId);

    List<StudentClazz> selectByClazzId(Integer clazzId);

    int insert(StudentClazz studentClazz);

    void deleteById(Integer id);

    void deleteByStudentAndClazz(Integer studentId, Integer clazzId);

    @Delete("delete from student_clazz where clazz_id = #{clazzId}")
    int deleteByClazzId(Integer clazzId);

    @Delete("delete from student_clazz where clazz_id = #{clazzId} and student_id = #{studentId}")
    int deleteByClazzIdAndStudentId(@Param("clazzId") Integer clazzId, @Param("studentId") Integer studentId);
}
