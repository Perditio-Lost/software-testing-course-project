package com.example.common.dto;

/**
 * 错题信息DTO类
 * 用于AI分析服务中封装错题的关键信息
 */
public class WrongQuestionInfo {
    private Integer questionNumber;   // 试卷中的题号
    private String questionType;      // 题目类型
    private String questionStem;      // 题干内容
    private String studentAnswer;     // 学生答案
    private String correctAnswer;     // 正确答案
    private Integer score;            // 学生得分
    private Integer fullScore;        // 题目满分
    private java.util.List<WrongQuestionInfo> subQuestions; // 子题列表（用于复合题）

    public Integer getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(Integer questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getQuestionStem() {
        return questionStem;
    }

    public void setQuestionStem(String questionStem) {
        this.questionStem = questionStem;
    }

    public String getStudentAnswer() {
        return studentAnswer;
    }

    public void setStudentAnswer(String studentAnswer) {
        this.studentAnswer = studentAnswer;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getFullScore() {
        return fullScore;
    }

    public void setFullScore(Integer fullScore) {
        this.fullScore = fullScore;
    }

    public java.util.List<WrongQuestionInfo> getSubQuestions() {
        return subQuestions;
    }

    public void setSubQuestions(java.util.List<WrongQuestionInfo> subQuestions) {
        this.subQuestions = subQuestions;
    }
}
