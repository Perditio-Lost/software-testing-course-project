package com.example.service;

import com.example.common.dto.Account;
import com.example.entity.TestPaperTemplate;
import com.example.common.utils.TokenUtils;
import com.example.mapper.TestPaperTemplateMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestPaperTemplateService {

    @Resource
    private TestPaperTemplateMapper testPaperTemplateMapper;

    public void add(TestPaperTemplate template) {
        Account currentUser = TokenUtils.getCurrentUser();
        template.setTeacherId(currentUser.getId());
        testPaperTemplateMapper.insert(template);
    }

    public void updateById(TestPaperTemplate template) {
        testPaperTemplateMapper.updateById(template);
    }

    public void deleteById(Integer id) {
        testPaperTemplateMapper.deleteById(id);
    }

    public TestPaperTemplate selectById(Integer id) {
        return testPaperTemplateMapper.selectById(id);
    }

    public List<TestPaperTemplate> selectAll(TestPaperTemplate template) {
        Account currentUser = TokenUtils.getCurrentUser();
        // 试卷模板所有教师都能使用，不再限制教师ID
        // template.setTeacherId(currentUser.getId());
        return testPaperTemplateMapper.selectAll(template);
    }

    public PageInfo<TestPaperTemplate> selectPage(TestPaperTemplate template, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        // 试卷模板所有教师都能使用，不再限制教师ID
        // template.setTeacherId(currentUser.getId());
        PageHelper.startPage(pageNum, pageSize);
        List<TestPaperTemplate> list = testPaperTemplateMapper.selectAll(template);
        return PageInfo.of(list);
    }
}

