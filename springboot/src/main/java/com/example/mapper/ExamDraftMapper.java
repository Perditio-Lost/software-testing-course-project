package com.example.mapper;

import com.example.entity.ExamDraft;
import org.apache.ibatis.annotations.*;

/**
 * 考试答题草稿Mapper
 */
@Mapper
public interface ExamDraftMapper {

    @Select("SELECT * FROM exam_draft WHERE student_id = #{studentId} AND test_id = #{testId}")
    ExamDraft selectByStudentAndTest(@Param("studentId") Integer studentId, @Param("testId") Integer testId);

    @Insert("INSERT INTO exam_draft(student_id, test_id, draft_data) " +
            "VALUES(#{studentId}, #{testId}, #{draftData}) " +
            "ON DUPLICATE KEY UPDATE draft_data = #{draftData}, update_time = CURRENT_TIMESTAMP")
    int saveOrUpdate(ExamDraft examDraft);

    @Delete("DELETE FROM exam_draft WHERE student_id = #{studentId} AND test_id = #{testId}")
    int deleteByStudentAndTest(@Param("studentId") Integer studentId, @Param("testId") Integer testId);
}
