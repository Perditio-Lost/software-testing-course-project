package com.example.entity;

import java.util.List;

public class Test {
    private Integer id;
    private String name;
    private String content;
    private Integer testPaperId;
    private String start;
    private String end;
    private Integer duration;
    private String releaseTime;
    private Boolean weatherCheck;
    private Boolean weatherQuestionUnordered;
    private Boolean weatherChoiceUnordered;
    private Boolean weatherCopy;
    private Boolean weatherFace;
    private Boolean weatherDualCamera;
    private Boolean weatherScreenSwitch;

    private TestPaper testPaper;
    private String status;

    private String courseName;
    private String teacherName;
    
    // 用于接收前端传递的班级ID列表
    private List<Integer> clazzIds;

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

    public Integer getTestPaperId() {
        return testPaperId;
    }

    public void setTestPaperId(Integer testPaperId) {
        this.testPaperId = testPaperId;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(String releaseTime) {
        this.releaseTime = releaseTime;
    }

    public TestPaper getTestPaper() {
        return testPaper;
    }

    public void setTestPaper(TestPaper testPaper) {
        this.testPaper = testPaper;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public List<Integer> getClazzIds() {
        return clazzIds;
    }

    public void setClazzIds(List<Integer> clazzIds) {
        this.clazzIds = clazzIds;
    }

    public Boolean getWeatherCheck() {
        return weatherCheck;
    }

    public void setWeatherCheck(Boolean weatherCheck) {
        this.weatherCheck = weatherCheck;
    }

    public Boolean getWeatherQuestionUnordered() {
        return weatherQuestionUnordered;
    }

    public void setWeatherQuestionUnordered(Boolean weatherQuestionUnordered) {
        this.weatherQuestionUnordered = weatherQuestionUnordered;
    }

    public Boolean getWeatherChoiceUnordered() {
        return weatherChoiceUnordered;
    }

    public void setWeatherChoiceUnordered(Boolean weatherChoiceUnordered) {
        this.weatherChoiceUnordered = weatherChoiceUnordered;
    }

    public Boolean getWeatherCopy() {
        return weatherCopy;
    }

    public void setWeatherCopy(Boolean weatherCopy) {
        this.weatherCopy = weatherCopy;
    }

    public Boolean getWeatherFace() {
        return weatherFace;
    }

    public void setWeatherFace(Boolean weatherFace) {
        this.weatherFace = weatherFace;
    }

    public Boolean getWeatherDualCamera() {
        return weatherDualCamera;
    }

    public void setWeatherDualCamera(Boolean weatherDualCamera) {
        this.weatherDualCamera = weatherDualCamera;
    }

    public Boolean getWeatherScreenSwitch() {
        return weatherScreenSwitch;
    }

    public void setWeatherScreenSwitch(Boolean weatherScreenSwitch) {
        this.weatherScreenSwitch = weatherScreenSwitch;
    }
}
