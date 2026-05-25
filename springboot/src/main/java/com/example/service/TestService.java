package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.entity.StudentClazz;
import com.example.entity.Test;
import com.example.entity.TestClazz;
import com.example.entity.TestPaper;
import com.example.mapper.StudentClazzMapper;
import com.example.mapper.TestClazzMapper;
import com.example.mapper.TestMapper;
import com.example.mapper.TestPaperMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 考试信息业务层方法
 */
@Service
public class TestService {

    @Resource
    private TestMapper testMapper;
    @Resource
    private TestPaperMapper testPaperMapper;
    @Resource
    private TestPaperService testPaperService;
    @Resource
    private TestClazzMapper testClazzMapper;
    @Resource
    private StudentClazzMapper studentClazzMapper;

    public void add(Test test) {
        if (ObjectUtil.isEmpty(test.getReleaseTime())) {
            test.setReleaseTime(DateUtil.now());
        }
        testMapper.insert(test);
        
        // 保存考试和班级的关联关系
        if (test.getClazzIds() != null && !test.getClazzIds().isEmpty()) {
            for (Integer clazzId : test.getClazzIds()) {
                TestClazz testClazz = new TestClazz();
                testClazz.setTestId(test.getId());
                testClazz.setClazzId(clazzId);
                testClazzMapper.insert(testClazz);
            }
        }
    }

    public void updateById(Test test) {
        testMapper.updateById(test);
        
        // 更新班级关联：先删除旧的关联，再插入新的关联
        if (test.getClazzIds() != null) {
            testClazzMapper.deleteByTestId(test.getId());
            for (Integer clazzId : test.getClazzIds()) {
                TestClazz testClazz = new TestClazz();
                testClazz.setTestId(test.getId());
                testClazz.setClazzId(clazzId);
                testClazzMapper.insert(testClazz);
            }
        }
    }

    public void deleteById(Integer id) {
        testMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            testMapper.deleteById(id);
        }
    }

    public Test selectById(Integer id) {
        Test test = testMapper.selectById(id);
        if (test != null) {
            // 初始化状态
            initStatus(test);
            
            // 加载完整的试卷信息（包括题目）
            if (test.getTestPaperId() != null) {
                TestPaper testPaper = testPaperService.selectById(test.getTestPaperId());
                test.setTestPaper(testPaper);
            }
            
            // 加载关联的班级ID列表
            List<TestClazz> testClazzList = testClazzMapper.selectByTestId(id);
            if (testClazzList != null && !testClazzList.isEmpty()) {
                List<Integer> clazzIds = testClazzList.stream()
                    .map(TestClazz::getClazzId)
                    .collect(Collectors.toList());
                test.setClazzIds(clazzIds);
            }
        }
        return test;
    }

    public List<Test> selectAll(Test test) {
        return testMapper.selectAll(test);
    }

    public PageInfo<Test> selectPage(Test test, Integer pageNum, Integer pageSize) {
        // 教师角色筛选由前端传递testPaperId，或者在查询时过滤
        PageHelper.startPage(pageNum, pageSize);
        List<Test> list = testMapper.selectAll(test);
        
        // 初始化每个考试的状态和班级信息
        for (Test t : list) {
            initStatus(t);
            
            // 加载关联的班级ID列表
            List<TestClazz> testClazzList = testClazzMapper.selectByTestId(t.getId());
            if (testClazzList != null && !testClazzList.isEmpty()) {
                List<Integer> clazzIds = testClazzList.stream()
                    .map(TestClazz::getClazzId)
                    .collect(Collectors.toList());
                t.setClazzIds(clazzIds);
            }
        }
        
        return PageInfo.of(list);
    }

    /**
     * 根据学生ID分页查询该学生班级的考试
     */
    public PageInfo<Test> selectPageByStudent(Integer studentId, String courseName, Integer clazzId, Integer pageNum, Integer pageSize) {
        // 1. 查询学生所在的所有班级
        List<StudentClazz> studentClazzList = studentClazzMapper.selectByStudentId(studentId);
        if (studentClazzList == null || studentClazzList.isEmpty()) {
            return new PageInfo<>(new ArrayList<>());
        }
        
        // 2. 获取班级ID列表
        List<Integer> clazzIds = studentClazzList.stream()
                .map(StudentClazz::getClazzId)
                .collect(Collectors.toList());
        
        // 如果有班级ID筛选条件，只保留指定的班级
        if (clazzId != null) {
            clazzIds = clazzIds.stream()
                    .filter(id -> id.equals(clazzId))
                    .collect(Collectors.toList());
            if (clazzIds.isEmpty()) {
                return new PageInfo<>(new ArrayList<>());
            }
        }
        
        // 3. 查询这些班级关联的所有考试ID
        Set<Integer> testIds = new HashSet<>();
        for (Integer cid : clazzIds) {
            List<TestClazz> testClazzList = testClazzMapper.selectByClazzId(cid);
            for (TestClazz tc : testClazzList) {
                testIds.add(tc.getTestId());
            }
        }
        
        if (testIds.isEmpty()) {
            return new PageInfo<>(new ArrayList<>());
        }
        
        // 4. 查询这些考试的详细信息
        List<Test> allTests = new ArrayList<>();
        for (Integer testId : testIds) {
            Test test = testMapper.selectById(testId);
            if (test != null) {
                // 如果有课程名称筛选条件（不区分大小写）
                if (courseName != null && !courseName.isEmpty()) {
                    if (test.getCourseName() != null && test.getCourseName().toLowerCase().contains(courseName.toLowerCase())) {
                        allTests.add(test);
                    }
                } else {
                    allTests.add(test);
                }
            }
        }
        
        // 5. 初始化考试状态
        for (Test t : allTests) {
            initStatus(t);
        }
        
        // 6. 手动分页
        int total = allTests.size();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        
        List<Test> pageList = new ArrayList<>();
        if (start < total) {
            pageList = allTests.subList(start, end);
        }
        
        PageInfo<Test> pageInfo = new PageInfo<>(pageList);
        pageInfo.setTotal(total);
        pageInfo.setPageNum(pageNum);
        pageInfo.setPageSize(pageSize);
        pageInfo.setPages((total + pageSize - 1) / pageSize);
        
        return pageInfo;
    }

    /**
     * 根据班级ID查询该班级的考试列表
     */
    public List<Test> selectByClazz(Integer clazzId) {
        return selectByClazz(clazzId, false);
    }

    /**
     * 根据班级ID查询该班级的考试列表
     * @param clazzId 班级ID
     * @param onlyFinished 是否只返回已结束的考试
     */
    public List<Test> selectByClazz(Integer clazzId, Boolean onlyFinished) {
        List<TestClazz> testClazzList = testClazzMapper.selectByClazzId(clazzId);
        List<Test> testList = new ArrayList<>();
        
        for (TestClazz tc : testClazzList) {
            Test test = testMapper.selectById(tc.getTestId());
            if (test != null) {
                initStatus(test);
                // 如果只需要已结束的考试，则过滤掉未结束的
                if (onlyFinished != null && onlyFinished && !"已结束".equals(test.getStatus())) {
                    continue;
                }
                testList.add(test);
            }
        }
        
        return testList;
    }

    /**
     * 初始化考试状态
     */
    private void initStatus(Test test) {
        try {
            long now = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            long start = sdf.parse(test.getStart()).getTime();
            long end = sdf.parse(test.getEnd()).getTime();
            
            if (now < start) {
                test.setStatus("未开始");
            } else if (now > end) {
                test.setStatus("已结束");
            } else {
                test.setStatus("进行中");
            }
        } catch (Exception e) {
            test.setStatus("未知");
        }
    }
}
