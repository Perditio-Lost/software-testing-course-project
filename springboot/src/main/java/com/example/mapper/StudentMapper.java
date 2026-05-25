package com.example.mapper;

import com.example.entity.Student;

import java.util.List;

public interface StudentMapper {

    int insert(Student student);

    void updateById(Student student);

    void deleteById(Integer id);

    Student selectById(Integer id);

    Student selectByUsername(String username);

    Student selectByEmail(String email);

    List<Student> selectAll(Student student);

    List<Student> selectApproved(Student student);

}
