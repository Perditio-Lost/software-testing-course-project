package com.example.mapper;

import com.example.entity.Teacher;

import java.util.List;

public interface TeacherMapper {

    int insert(Teacher teacher);

    void updateById(Teacher teacher);

    void deleteById(Integer id);

    Teacher selectById(Integer id);

    Teacher selectByUsername(String username);

    List<Teacher> selectAll(Teacher teacher);

}
