package com.example.common.dto;

import com.example.entity.TestPaper;

/**
 * 试卷提交数据传输对象
 */
public class TestSubmitDTO {
    private Integer id;  // 考试ID
    private TestPaper testPaper;  // 试卷及答案
    private Integer cheat;  // 作弊标记 0-未作弊 1-作弊

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public TestPaper getTestPaper() {
        return testPaper;
    }

    public void setTestPaper(TestPaper testPaper) {
        this.testPaper = testPaper;
    }

    public Integer getCheat() {
        return cheat;
    }

    public void setCheat(Integer cheat) {
        this.cheat = cheat;
    }
}
