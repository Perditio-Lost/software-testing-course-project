package com.example.mapper;

import com.example.entity.Course;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CourseMapper {

    int insert(Course course);

    void updateById(Course course);

    void deleteById(Integer id);

    @Select("select * from `course` where id = #{id}")
    Course selectById(Integer id);

    @Select("select id from `course` where name = #{name} limit 1")
    Integer selectIdByName(String name);

    List<Course> selectAll(Course course);

    /**
     * 查询教师所教的课程（通过班级关联）
     */
    @Select("SELECT DISTINCT c.* FROM course c " +
            "INNER JOIN clazz cl ON c.id = cl.course_id " +
            "WHERE cl.teacher_id = #{teacherId}")
    List<Course> selectByTeacherId(Integer teacherId);

}
