package com.example.entity;

public class Answer {

    private Integer id;
    private Integer scoreId;
    private Integer testPaperQuestionId;
    private String answer;
    private Integer score;
    private String status;

    private Integer questionId;
    private Integer questionScore;
    private Question question;
    private String testPaperQuestionType; // 题目类型：common/father/son
    
    // 用于前端编辑复合题的扩展字段
    private java.util.List<Integer> subQuestionAnswerIds; // 子题的answer记录ID
    private java.util.List<Integer> subScoreData; // 子题的分数
    private java.util.List<Integer> subQuestionTPQIds; // 子题的testPaperQuestionId
    private java.util.List<Integer> subMaxScores; // 子题的最大分数

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getScoreId() {
        return scoreId;
    }

    public void setScoreId(Integer scoreId) {
        this.scoreId = scoreId;
    }

    public Integer getTestPaperQuestionId() {
        return testPaperQuestionId;
    }

    public void setTestPaperQuestionId(Integer testPaperQuestionId) {
        this.testPaperQuestionId = testPaperQuestionId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public Integer getQuestionScore() {
        return questionScore;
    }

    public void setQuestionScore(Integer questionScore) {
        this.questionScore = questionScore;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public String getTestPaperQuestionType() {
        return testPaperQuestionType;
    }

    public void setTestPaperQuestionType(String testPaperQuestionType) {
        this.testPaperQuestionType = testPaperQuestionType;
    }

    public java.util.List<Integer> getSubQuestionAnswerIds() {
        return subQuestionAnswerIds;
    }

    public void setSubQuestionAnswerIds(java.util.List<Integer> subQuestionAnswerIds) {
        this.subQuestionAnswerIds = subQuestionAnswerIds;
    }

    public java.util.List<Integer> getSubScoreData() {
        return subScoreData;
    }

    public void setSubScoreData(java.util.List<Integer> subScoreData) {
        this.subScoreData = subScoreData;
    }

    public java.util.List<Integer> getSubQuestionTPQIds() {
        return subQuestionTPQIds;
    }

    public void setSubQuestionTPQIds(java.util.List<Integer> subQuestionTPQIds) {
        this.subQuestionTPQIds = subQuestionTPQIds;
    }

    public java.util.List<Integer> getSubMaxScores() {
        return subMaxScores;
    }

    public void setSubMaxScores(java.util.List<Integer> subMaxScores) {
        this.subMaxScores = subMaxScores;
    }
}
