package com.example.mapper;

import com.example.entity.Score;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ScoreMapper {

    int insert(Score score);

    void updateById(Score score);

    void deleteById(Integer id);

    Score selectById(Integer id);

    List<Score> selectAll(Score score);

    List<Score> selectByPaperId(Integer paperId);

    /**
     * 根据考试ID查询成绩列表
     */
    List<Score> selectByTestId(@Param("testId") Integer testId);

    /**
     * 根据考试ID和班级ID查询成绩列表
     */
    List<Score> selectByTestIdAndClazz(@Param("testId") Integer testId, @Param("clazzId") Integer clazzId);

    /**
     * 根据考试ID查询需要批阅的题目列表
     */
    List<Map<String, Object>> selectQuestionsByTestId(@Param("testId") Integer testId);

    /**
     * 根据考试ID和班级ID查询需要批阅的题目列表
     */
    List<Map<String, Object>> selectQuestionsByTestIdAndClazz(@Param("testId") Integer testId, @Param("clazzId") Integer clazzId);

    /**
     * 根据题目ID查询所有学生的答案
     */
    Map<String, Object> selectAnswersByQuestion(@Param("testId") Integer testId, @Param("questionId") Integer questionId);

    /**
     * 查询指定题目的所有学生答案列表
     */
    List<Map<String, Object>> selectStudentAnswersByQuestion(@Param("testId") Integer testId, @Param("questionId") Integer questionId);

    /**
     * 更新答案分数
     */
    void updateAnswerScore(@Param("scoreId") Integer scoreId, @Param("questionId") Integer questionId, @Param("score") Double score);

    /**
     * 更新复合题父题目的状态为"已批改"
     */
    void updateCompositeParentStatus(@Param("scoreId") Integer scoreId, @Param("questionId") Integer questionId);

    /**
     * 查询异常试卷（作弊记录）
     */
    List<Score> selectAbnormal(@Param("testName") String testName, 
                               @Param("studentName") String studentName,
                               @Param("testPaperName") String testPaperName, 
                               @Param("teacherId") Integer teacherId);

    /**
     * 查询已批改的成绩列表（用于导出Excel）
     */
    List<Score> selectGradedScores(@Param("clazzId") Integer clazzId, @Param("testId") Integer testId);

}
