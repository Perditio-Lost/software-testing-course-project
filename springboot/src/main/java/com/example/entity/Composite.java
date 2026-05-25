package com.example.entity;

public class Composite {
    private Integer id;
    private Integer questionFatherId;
    private Integer questionSonId;
    private Integer sequence;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getQuestionFatherId() {
        return questionFatherId;
    }

    public void setQuestionFatherId(Integer questionFatherId) {
        this.questionFatherId = questionFatherId;
    }

    public Integer getQuestionSonId() {
        return questionSonId;
    }

    public void setQuestionSonId(Integer questionSonId) {
        this.questionSonId = questionSonId;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }
}
