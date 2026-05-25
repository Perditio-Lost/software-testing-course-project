package com.example.mapper;

import com.example.entity.Question;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface QuestionMapper {

    int insert(Question question);

    void updateById(Question question);

    void deleteById(Integer id);

    @Select("select question.*, course.name as courseName, teacher.name as teacherName, " +
            "question_type.name as questionTypeName, question_type.variety as questionTypeVariety " +
            "from `question` " +
            "left join course on question.course_id = course.id " +
            "left join teacher on question.teacher_id = teacher.id " +
            "left join question_type on question.question_type_id = question_type.id " +
            "where question.id = #{id}")
    Question selectById(Integer id);

    @Select("select question.*, question_type.name as questionTypeName, question_type.variety as questionTypeVariety " +
            "from `question` " +
            "left join question_type on question.question_type_id = question_type.id " +
            "where question.id = #{id}")
    Question selectByIdSimple(Integer id);

    List<Question> selectAll(Question question);

    @Select("select * from question where course_id = #{courseId} and question_type_id = #{typeId} and (flag = 1 or flag is null)")
    List<Question> selectByCouserIdAndTypeId(@Param("courseId") Integer courseId, @Param("typeId") Integer typeId);
    
    @Select("select question.*, question_type.name as questionTypeName, question_type.variety as questionTypeVariety " +
            "from question " +
            "left join question_type on question.question_type_id = question_type.id " +
            "where question.course_id = #{courseId} " +
            "and question_type.name = #{typeName} " +
            "and (question.flag = 1 or question.flag is null)")
    List<Question> selectByCourseIdAndTypeName(@Param("courseId") Integer courseId, @Param("typeName") String typeName);
    
    @Select("select question.*, question_type.name as questionTypeName, question_type.variety as questionTypeVariety " +
            "from question " +
            "left join question_type on question.question_type_id = question_type.id " +
            "where question.course_id = #{courseId} " +
            "and question_type.variety = #{variety} " +
            "and (question.flag = 1 or question.flag is null)")
    List<Question> selectByCourseIdAndVariety(@Param("courseId") Integer courseId, @Param("variety") String variety);
    
    @Select("select question.*, question_type.name as questionTypeName, question_type.variety as questionTypeVariety " +
            "from question " +
            "left join question_type on question.question_type_id = question_type.id " +
            "where question.course_id = #{courseId} " +
            "and question_type.variety = 'text' " +
            "and question_type.name != '填空' " +
            "and (question.flag = 1 or question.flag is null)")
    List<Question> selectByCourseIdTextExceptFillIn(@Param("courseId") Integer courseId);
    
    @Select("select q.*, qt.name as questionTypeName, qt.variety as questionTypeVariety " +
            "from composite c " +
            "inner join question q on c.question_son_id = q.id " +
            "left join question_type qt on q.question_type_id = qt.id " +
            "where c.question_father_id = #{parentId} " +
            "order by c.sequence")
    List<Question> selectByParentId(@Param("parentId") Integer parentId);
}
