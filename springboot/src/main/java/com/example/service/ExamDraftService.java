package com.example.service;

import com.example.common.utils.TokenUtils;
import com.example.common.dto.Account;
import com.example.entity.ExamDraft;
import com.example.mapper.ExamDraftMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 考试答题草稿Service
 */
@Service
public class ExamDraftService {

    @Resource
    private ExamDraftMapper examDraftMapper;

    /**
     * 保存或更新草稿
     */
    public void saveOrUpdate(Integer testId, String draftData) {
        Account currentUser = TokenUtils.getCurrentUser();
        ExamDraft draft = new ExamDraft();
        draft.setStudentId(currentUser.getId());
        draft.setTestId(testId);
        draft.setDraftData(draftData);
        examDraftMapper.saveOrUpdate(draft);
    }

    /**
     * 获取草稿
     */
    public String getDraft(Integer testId) {
        Account currentUser = TokenUtils.getCurrentUser();
        ExamDraft draft = examDraftMapper.selectByStudentAndTest(currentUser.getId(), testId);
        return draft != null ? draft.getDraftData() : null;
    }

    /**
     * 删除草稿
     */
    public void deleteDraft(Integer testId) {
        Account currentUser = TokenUtils.getCurrentUser();
        examDraftMapper.deleteByStudentAndTest(currentUser.getId(), testId);
    }
}
