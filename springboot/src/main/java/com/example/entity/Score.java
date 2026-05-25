package com.example.entity;

import java.util.List;

public class Score {
    private Integer id;
    private Integer studentId;
    private Integer testId;
    private Integer score;
    private String status;
    private Integer cheat;  // 是否作弊，0-未作弊，1-作弊
    private String facePhotoUrl;  // 人脸识别照片URL

    private List<Answer> answers;
    private String studentName;
    private String studentNumber;
    private Test test;
    private String teacherName;  // 教师姓名
    private String clazzName;    // 班级名称

    private Integer teacherId;
    private List<Question> questions; // 试卷题目列表
    private Boolean weatherCheck; // 学生是否可以查看试卷

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Integer getTestId() {
        return testId;
    }

    public void setTestId(Integer testId) {
        this.testId = testId;
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

    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public Boolean getWeatherCheck() {
        return weatherCheck;
    }

    public void setWeatherCheck(Boolean weatherCheck) {
        this.weatherCheck = weatherCheck;
    }

    public Integer getCheat() {
        return cheat;
    }

    public void setCheat(Integer cheat) {
        this.cheat = cheat;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getClazzName() {
        return clazzName;
    }

    public void setClazzName(String clazzName) {
        this.clazzName = clazzName;
    }

    public String getFacePhotoUrl() {
        return facePhotoUrl;
    }

    public void setFacePhotoUrl(String facePhotoUrl) {
        this.facePhotoUrl = facePhotoUrl;
    }
}
