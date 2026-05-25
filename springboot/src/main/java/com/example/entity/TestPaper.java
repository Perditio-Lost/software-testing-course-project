package com.example.entity;

import java.util.List;

public class TestPaper {
    // 数据库的基本字段
    private Integer id;
    private String name;
    private Integer courseId;
    private Integer teacherId;
    private String type;
    private Double level; // 试卷难度系数

    // 关联课程信息表和教师信息表查询的额外的字段信息
    private String courseName;
    private String courseImg;
    private String teacherName;
    private String teacherUsername;

    // 用来接收前台那边传过来的业务字段
    private List<Integer> idList; // 接收前台那边手动选题选中的所有的题目的id
    private List<QuestionItem> questionList; // 接收前台手动选题带有分值和顺序的题目列表
    private Integer choiceNum; // 接收前台那边自动组卷填写的单选题的数量
    private Integer multiChoiceNum; // 接收前台那边自动组卷填写的多选题的数量
    private Integer fillInNum; // 接收前台那边自动组卷填写的填空题的数量
    private Integer checkNum; // 接收前台那边自动组卷填写的判断题的数量
    private Integer textNum; // 接收前台那边自动组卷填写的简答题的数量
    private Integer compositeNum; // 接收前台那边自动组卷填写的综合题的数量

    private Integer templateId;
    private Integer defaultScore;
    private Integer choiceScore;
    private Integer multiChoiceScore;
    private Integer fillInScore;
    private Integer checkScore;
    private Integer textScore;
    private Integer compositeScore;
    private List<Integer> scoreList;

    private List<Question> questions;
    
    // 自动组卷目标难度系数（不存储到数据库）
    private Double targetDifficulty;

    // 内部类：用于接收题目信息（包含分值和顺序）
    public static class QuestionItem {
        private Integer questionId;
        private Integer sequence;
        private Integer score;
        private List<SubQuestionScore> subQuestions;  // 复合题的子题分值

        public Integer getQuestionId() {
            return questionId;
        }

        public void setQuestionId(Integer questionId) {
            this.questionId = questionId;
        }

        public Integer getSequence() {
            return sequence;
        }

        public void setSequence(Integer sequence) {
            this.sequence = sequence;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public List<SubQuestionScore> getSubQuestions() {
            return subQuestions;
        }

        public void setSubQuestions(List<SubQuestionScore> subQuestions) {
            this.subQuestions = subQuestions;
        }
    }
    
    // 子题分值信息
    public static class SubQuestionScore {
        private Integer questionId;
        private Integer score;

        public Integer getQuestionId() {
            return questionId;
        }

        public void setQuestionId(Integer questionId) {
            this.questionId = questionId;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getLevel() {
        return level;
    }

    public void setLevel(Double level) {
        this.level = level;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseImg() {
        return courseImg;
    }

    public void setCourseImg(String courseImg) {
        this.courseImg = courseImg;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getTeacherUsername() {
        return teacherUsername;
    }

    public void setTeacherUsername(String teacherUsername) {
        this.teacherUsername = teacherUsername;
    }

    public List<Integer> getIdList() {
        return idList;
    }

    public void setIdList(List<Integer> idList) {
        this.idList = idList;
    }

    public List<QuestionItem> getQuestionList() {
        return questionList;
    }

    public void setQuestionList(List<QuestionItem> questionList) {
        this.questionList = questionList;
    }

    public Integer getChoiceNum() {
        return choiceNum;
    }

    public void setChoiceNum(Integer choiceNum) {
        this.choiceNum = choiceNum;
    }

    public Integer getMultiChoiceNum() {
        return multiChoiceNum;
    }

    public void setMultiChoiceNum(Integer multiChoiceNum) {
        this.multiChoiceNum = multiChoiceNum;
    }

    public Integer getFillInNum() {
        return fillInNum;
    }

    public void setFillInNum(Integer fillInNum) {
        this.fillInNum = fillInNum;
    }

    public Integer getCheckNum() {
        return checkNum;
    }

    public void setCheckNum(Integer checkNum) {
        this.checkNum = checkNum;
    }

    public Integer getTextNum() {
        return textNum;
    }

    public void setTextNum(Integer textNum) {
        this.textNum = textNum;
    }

    public Integer getCompositeNum() {
        return compositeNum;
    }

    public void setCompositeNum(Integer compositeNum) {
        this.compositeNum = compositeNum;
    }

    public Integer getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }

    public Integer getDefaultScore() {
        return defaultScore;
    }

    public void setDefaultScore(Integer defaultScore) {
        this.defaultScore = defaultScore;
    }

    public Integer getChoiceScore() {
        return choiceScore;
    }

    public void setChoiceScore(Integer choiceScore) {
        this.choiceScore = choiceScore;
    }

    public Integer getMultiChoiceScore() {
        return multiChoiceScore;
    }

    public void setMultiChoiceScore(Integer multiChoiceScore) {
        this.multiChoiceScore = multiChoiceScore;
    }

    public Integer getFillInScore() {
        return fillInScore;
    }

    public void setFillInScore(Integer fillInScore) {
        this.fillInScore = fillInScore;
    }

    public Integer getCheckScore() {
        return checkScore;
    }

    public void setCheckScore(Integer checkScore) {
        this.checkScore = checkScore;
    }

    public Integer getTextScore() {
        return textScore;
    }

    public void setTextScore(Integer textScore) {
        this.textScore = textScore;
    }

    public Integer getCompositeScore() {
        return compositeScore;
    }

    public void setCompositeScore(Integer compositeScore) {
        this.compositeScore = compositeScore;
    }

    public List<Integer> getScoreList() {
        return scoreList;
    }

    public void setScoreList(List<Integer> scoreList) {
        this.scoreList = scoreList;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public Double getTargetDifficulty() {
        return targetDifficulty;
    }

    public void setTargetDifficulty(Double targetDifficulty) {
        this.targetDifficulty = targetDifficulty;
    }
}
