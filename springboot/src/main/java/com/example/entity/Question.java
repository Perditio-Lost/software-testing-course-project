package com.example.entity;

import com.example.common.dto.StudentAnalysisDTO;

import java.util.List;

public class Question {
    private Integer id;
    private Integer questionTypeId;
    private Integer courseId;
    private Integer teacherId;
    private Double level;
    private String questionStem;
    private String answer;
    private boolean flag;

    private String courseName;
    private String teacherName;
    private String questionTypeName;
    private String questionTypeVariety;

    private List<Choice> choiceList;
    private List<Question> questionList;

    private String newAnswer;
    private List<String> checkList;
    private Integer typeScore; // 题目在试卷中的分值
    private Integer testPaperQuestionId; // 试卷题目关联ID
    private Integer answerScore; // 学生该题获得的分数
    private String paperQuestionType; // 试卷中题目类型：common/father/son

    private Integer totalCount;
    private Integer rightCount;
    private Integer errorCount;
    private Integer partRightCount;
    private List<StudentAnalysisDTO> studentList;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getQuestionTypeId() {
        return questionTypeId;
    }

    public void setQuestionTypeId(Integer questionTypeId) {
        this.questionTypeId = questionTypeId;
    }

    // questionTypeId getter/setter 保持不变

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public Double getLevel() {
        return level;
    }

    public void setLevel(Double level) {
        this.level = level;
    }

    public String getQuestionStem() {
        return questionStem;
    }

    public void setQuestionStem(String questionStem) {
        this.questionStem = questionStem;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }


    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getQuestionTypeName() {
        return questionTypeName;
    }

    public void setQuestionTypeName(String questionTypeName) {
        this.questionTypeName = questionTypeName;
    }

    public String getQuestionTypeVariety() {
        return questionTypeVariety;
    }

    public void setQuestionTypeVariety(String questionTypeVariety) {
        this.questionTypeVariety = questionTypeVariety;
    }

    public List<Choice> getChoiceList() {
        return choiceList;
    }

    public void setChoiceList(List<Choice> choiceList) {
        this.choiceList = choiceList;
    }

    public List<Question> getQuestionList() {
        return questionList;
    }

    public void setQuestionList(List<Question> questionList) {
        this.questionList = questionList;
    }

    public String getNewAnswer() {
        return newAnswer;
    }

    public void setNewAnswer(String newAnswer) {
        this.newAnswer = newAnswer;
    }

    public List<String> getCheckList() {
        return checkList;
    }

    public void setCheckList(List<String> checkList) {
        this.checkList = checkList;
    }

    public Integer getTypeScore() {
        return typeScore;
    }

    public void setTypeScore(Integer typeScore) {
        this.typeScore = typeScore;
    }

    public Integer getTestPaperQuestionId() {
        return testPaperQuestionId;
    }

    public void setTestPaperQuestionId(Integer testPaperQuestionId) {
        this.testPaperQuestionId = testPaperQuestionId;
    }

    public Integer getAnswerScore() {
        return answerScore;
    }

    public void setAnswerScore(Integer answerScore) {
        this.answerScore = answerScore;
    }

    public String getPaperQuestionType() {
        return paperQuestionType;
    }

    public void setPaperQuestionType(String paperQuestionType) {
        this.paperQuestionType = paperQuestionType;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getRightCount() {
        return rightCount;
    }

    public void setRightCount(Integer rightCount) {
        this.rightCount = rightCount;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public Integer getPartRightCount() {
        return partRightCount;
    }

    public void setPartRightCount(Integer partRightCount) {
        this.partRightCount = partRightCount;
    }

    public List<StudentAnalysisDTO> getStudentList() {
        return studentList;
    }

    public void setStudentList(List<StudentAnalysisDTO> studentList) {
        this.studentList = studentList;
    }
}
