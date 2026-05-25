package com.example.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.dto.Account;
import com.example.common.dto.StudentAnalysisDTO;
import com.example.common.enums.RoleEnum;
import com.example.entity.*;
import com.example.exception.CustomException;
import com.example.mapper.*;
import com.example.common.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 试卷信息业务层方法
 */
@Service
public class TestPaperService {

    @Resource
    private TestPaperMapper testPaperMapper;
    @Resource
    private TestPaperQuestionMapper testPaperQuestionMapper;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private TeacherMapper teacherMapper;
    @Resource
    private ScoreMapper scoreMapper;
    @Resource
    private com.example.mapper.AnswerMapper answerMapper;
    @Resource
    private QuestionService questionService;
    @Resource
    private TestPaperTemplateMapper testPaperTemplateMapper;
    @Resource
    private TestMapper testMapper;

    @Transactional
    public void add(TestPaper testPaper) {
        Account currentUser = TokenUtils.getCurrentUser();
        testPaper.setTeacherId(currentUser.getId());

        // 如果使用模板自动组卷，加载模板配置
        TestPaperTemplate template = null;
        if ("自动组卷".equals(testPaper.getType()) && testPaper.getTemplateId() != null) {
            template = testPaperTemplateMapper.selectById(testPaper.getTemplateId());
            if (template != null) {
                testPaper.setChoiceNum(template.getChoiceNum());
                testPaper.setMultiChoiceNum(template.getMultiChoiceNum());
                testPaper.setCheckNum(template.getCheckNum());
                testPaper.setFillInNum(template.getFillInNum());
                testPaper.setTextNum(template.getTextNum());
                testPaper.setCompositeNum(template.getCompositeNum());
            }
        }

        check(testPaper);

        // 插入试卷基本信息
        testPaperMapper.insert(testPaper);

        // 手动选题：从 questionList 获取题目及其分值和顺序
        if ("手动选题".equals(testPaper.getType())) {
            if (CollectionUtil.isEmpty(testPaper.getQuestionList())) {
                throw new CustomException("-1", "手动选题时必须提供题目列表");
            }
            
            for (TestPaper.QuestionItem item : testPaper.getQuestionList()) {
                Question question = questionService.selectById(item.getQuestionId());
                if (question == null)
                    continue;

                TestPaperQuestion tpq = new TestPaperQuestion();
                tpq.setTestPaperId(testPaper.getId());
                tpq.setQuestionId(item.getQuestionId());
                tpq.setSequence(item.getSequence());
                tpq.setScore(item.getScore() != null ? item.getScore() : 10);

                if ("composite".equals(question.getQuestionTypeVariety())) {
                    tpq.setType("father");
                    testPaperQuestionMapper.insert(tpq);

                    if (CollectionUtil.isNotEmpty(question.getQuestionList())) {
                        // 如果前端提供了子题分值，使用前端的分值；否则平均分配
                        if (CollectionUtil.isNotEmpty(item.getSubQuestions())) {
                            // 使用前端提供的子题分值
                            java.util.Map<Integer, Integer> subScoreMap = item.getSubQuestions().stream()
                                .collect(java.util.stream.Collectors.toMap(
                                    TestPaper.SubQuestionScore::getQuestionId,
                                    TestPaper.SubQuestionScore::getScore
                                ));
                            
                            for (Question subQ : question.getQuestionList()) {
                                TestPaperQuestion subTpq = new TestPaperQuestion();
                                subTpq.setTestPaperId(testPaper.getId());
                                subTpq.setQuestionId(subQ.getId());
                                subTpq.setSequence(item.getSequence());
                                subTpq.setScore(subScoreMap.getOrDefault(subQ.getId(), 1));
                                subTpq.setType("son");
                                testPaperQuestionMapper.insert(subTpq);
                            }
                        } else {
                            // 计算子题目分数：总分平均分配，余数加到最后一个子题
                            int totalScore = item.getScore() != null ? item.getScore() : 10;
                            int subQuestionCount = question.getQuestionList().size();
                            int averageScore = totalScore / subQuestionCount;
                            int remainder = totalScore % subQuestionCount;
                            
                            for (int j = 0; j < question.getQuestionList().size(); j++) {
                                Question subQ = question.getQuestionList().get(j);
                                TestPaperQuestion subTpq = new TestPaperQuestion();
                                subTpq.setTestPaperId(testPaper.getId());
                                subTpq.setQuestionId(subQ.getId());
                                subTpq.setSequence(item.getSequence());
                                // 最后一个子题加上余数
                                int subScore = averageScore + (j == subQuestionCount - 1 ? remainder : 0);
                                subTpq.setScore(subScore);
                                subTpq.setType("son");
                                testPaperQuestionMapper.insert(subTpq);
                            }
                        }
                    }
                } else {
                    tpq.setType("common");
                    testPaperQuestionMapper.insert(tpq);
                }
            }
            
            // 计算并保存试卷难度系数
            Double difficulty = calculateDifficulty(testPaper.getId());
            testPaper.setLevel(difficulty);
            testPaperMapper.updateById(testPaper);
            
            return;
        }
        
        // 自动组卷
        if ("自动组卷".equals(testPaper.getType())) {
            List<Integer> questionIds = new ArrayList<>();
            
            // 获取目标难度系数和容差
            Double targetDifficulty = testPaper.getTargetDifficulty();
            Double difficultyTolerance = 0.15; // 默认容差为0.15
            
            // 如果指定了目标难度，使用基于难度的选题方法
            if (targetDifficulty != null && targetDifficulty >= 0) {
                randomQuestionIdsByDifficulty(testPaper, testPaper.getChoiceNum(), questionIds, 1, "单选", 
                                            targetDifficulty, difficultyTolerance);
                randomQuestionIdsByDifficulty(testPaper, testPaper.getMultiChoiceNum(), questionIds, 2, "多选", 
                                            targetDifficulty, difficultyTolerance);
                randomQuestionIdsByDifficulty(testPaper, testPaper.getCheckNum(), questionIds, 3, "判断", 
                                            targetDifficulty, difficultyTolerance);
                randomQuestionIdsByDifficulty(testPaper, testPaper.getFillInNum(), questionIds, 4, "填空", 
                                            targetDifficulty, difficultyTolerance);
                randomQuestionIdsByDifficulty(testPaper, testPaper.getTextNum(), questionIds, 5, "简答", 
                                            targetDifficulty, difficultyTolerance);
                randomQuestionIdsByDifficulty(testPaper, testPaper.getCompositeNum(), questionIds, 6, "复合", 
                                            targetDifficulty, difficultyTolerance);
            } else {
                // 没有指定难度，使用原有的随机选题方法
                randomQuestionIds(testPaper, testPaper.getChoiceNum(), questionIds, 1, "单选");
                randomQuestionIds(testPaper, testPaper.getMultiChoiceNum(), questionIds, 2, "多选");
                randomQuestionIds(testPaper, testPaper.getCheckNum(), questionIds, 3, "判断");
                randomQuestionIds(testPaper, testPaper.getFillInNum(), questionIds, 4, "填空");
                randomQuestionIds(testPaper, testPaper.getTextNum(), questionIds, 5, "简答");
                randomQuestionIds(testPaper, testPaper.getCompositeNum(), questionIds, 6, "复合");
            }

            for (int i = 0; i < questionIds.size(); i++) {
                Integer qId = questionIds.get(i);
                Question question = questionService.selectById(qId);
                if (question == null)
                    continue;

                TestPaperQuestion tpq = new TestPaperQuestion();
                tpq.setTestPaperId(testPaper.getId());
                tpq.setQuestionId(qId);
                tpq.setSequence(i + 1);

                String typeName = question.getQuestionTypeName();
                String variety = question.getQuestionTypeVariety();
                Integer baseScore = testPaper.getDefaultScore();
                if (baseScore == null) {
                    baseScore = 10;
                }
                if ("单选".equals(typeName) && testPaper.getChoiceScore() != null) {
                    baseScore = testPaper.getChoiceScore();
                } else if ("多选".equals(typeName) && testPaper.getMultiChoiceScore() != null) {
                    baseScore = testPaper.getMultiChoiceScore();
                } else if ("判断".equals(typeName) && testPaper.getCheckScore() != null) {
                    baseScore = testPaper.getCheckScore();
                } else if ("填空".equals(typeName) && testPaper.getFillInScore() != null) {
                    baseScore = testPaper.getFillInScore();
                } else if ("text".equals(variety) && !"填空".equals(typeName) && testPaper.getTextScore() != null) {
                    // 简答题（主观题）：名词解析、论述、计算、程序
                    baseScore = testPaper.getTextScore();
                } else if ("composite".equals(variety) && testPaper.getCompositeScore() != null) {
                    // 复合题：资料、完形填空、阅读理解、综合
                    baseScore = testPaper.getCompositeScore();
                }
                tpq.setScore(baseScore);

            if ("composite".equals(question.getQuestionTypeVariety())) {
                tpq.setType("father");
                testPaperQuestionMapper.insert(tpq);

                if (CollectionUtil.isNotEmpty(question.getQuestionList())) {
                    // 计算子题目分数：总分平均分配
                    int subQuestionCount = question.getQuestionList().size();
                    int averageScore = baseScore / subQuestionCount; // 基础分数
                    int remainder = baseScore % subQuestionCount; // 余数
                    
                    for (int j = 0; j < question.getQuestionList().size(); j++) {
                        Question subQ = question.getQuestionList().get(j);
                        TestPaperQuestion subTpq = new TestPaperQuestion();
                        subTpq.setTestPaperId(testPaper.getId());
                        subTpq.setQuestionId(subQ.getId());
                        subTpq.setSequence(i + 1);
                        // 最后一个子题加上余数
                        Integer subScore = averageScore + (j == subQuestionCount - 1 ? remainder : 0);
                        subTpq.setScore(subScore);
                        subTpq.setType("son");
                        testPaperQuestionMapper.insert(subTpq);
                    }
                }
            } else {
                tpq.setType("common");
                testPaperQuestionMapper.insert(tpq);
            }
            }
        }
        
        // 计算并保存试卷难度系数
        Double difficulty = calculateDifficulty(testPaper.getId());
        testPaper.setLevel(difficulty);
        testPaperMapper.updateById(testPaper);
    }

    private void randomQuestionIds(TestPaper testPaper, Integer count, List<Integer> list, Integer typeId,
            String typeName) {
        if (count == null || count == 0) {
            return;
        }
        
        List<Question> questionList;
        
        // 根据题型名称选择不同的查询策略
        if ("单选".equals(typeName) || "多选".equals(typeName) || "判断".equals(typeName) || "填空".equals(typeName)) {
            // 单选、多选、判断、填空：根据题型名称精确查询
            questionList = questionMapper.selectByCourseIdAndTypeName(testPaper.getCourseId(), typeName);
        } else if ("简答".equals(typeName)) {
            // 简答题：从除填空以外的text类题目中随机选择（名词解析、论述、计算、程序）
            questionList = questionMapper.selectByCourseIdTextExceptFillIn(testPaper.getCourseId());
            typeName = "简答（主观）";
        } else if ("复合".equals(typeName)) {
            // 复合题：从composite类题目中随机选择（资料、完形填空、阅读理解、综合）
            questionList = questionMapper.selectByCourseIdAndVariety(testPaper.getCourseId(), "composite");
            typeName = "复合";
        } else {
            // 兼容旧的typeId查询方式
            questionList = questionMapper.selectByCouserIdAndTypeId(testPaper.getCourseId(), typeId);
        }
        
        if (questionList.size() < count) {
            throw new CustomException("-1",
                    "您的题库里该课程的" + typeName + "题数量不足，请减少出题的" + typeName + "题数量或者增加题库里该课程的" + typeName + "题数量");
        }
        Collections.shuffle(questionList); // 打乱
        List<Integer> questions = questionList.subList(0, count).stream().map(Question::getId).toList();
        list.addAll(questions);
    }

    public void check(TestPaper testPaper) {
        if (ObjectUtil.isEmpty(testPaper.getName())
                || ObjectUtil.isEmpty(testPaper.getCourseId())
                || ObjectUtil.isEmpty(testPaper.getType())) {
            throw new CustomException("-1", "请填写完整您要提交的试卷信息");
        }

        // 校验手动选题
        if ("手动选题".equals(testPaper.getType())) {
            // 新版本使用 questionList，旧版本使用 idList
            if (CollectionUtil.isEmpty(testPaper.getQuestionList()) && CollectionUtil.isEmpty(testPaper.getIdList())) {
                throw new CustomException("-1", "手动选题方式，您需要选择具体的题目");
            }
        }
        // 自动组卷校验
        if ("自动组卷".equals(testPaper.getType())) {
            // 为null的数量字段设置默认值0
            if (testPaper.getChoiceNum() == null)
                testPaper.setChoiceNum(0);
            if (testPaper.getMultiChoiceNum() == null)
                testPaper.setMultiChoiceNum(0);
            if (testPaper.getFillInNum() == null)
                testPaper.setFillInNum(0);
            if (testPaper.getCheckNum() == null)
                testPaper.setCheckNum(0);
            if (testPaper.getTextNum() == null)
                testPaper.setTextNum(0);
            if (testPaper.getCompositeNum() == null)
                testPaper.setCompositeNum(0);

            if (testPaper.getChoiceNum() < 0
                    || testPaper.getMultiChoiceNum() < 0
                    || testPaper.getFillInNum() < 0
                    || testPaper.getCheckNum() < 0
                    || testPaper.getTextNum() < 0
                    || testPaper.getCompositeNum() < 0) {
                throw new CustomException("-1", "题型的数量不能小于0");
            }

            // 检查是否至少选择了一种题型
            if (testPaper.getChoiceNum() + testPaper.getMultiChoiceNum()
                    + testPaper.getFillInNum() + testPaper.getCheckNum()
                    + testPaper.getTextNum() + testPaper.getCompositeNum() == 0) {
                throw new CustomException("-1", "至少需要选择一种题型");
            }
        }
    }

    @Transactional
    public void updateById(TestPaper testPaper) {
        // 更新试卷基本信息
        testPaperMapper.updateById(testPaper);
        
        // 如果是手动选题并且提供了题目列表，则更新题目关联
        if ("手动选题".equals(testPaper.getType()) && CollectionUtil.isNotEmpty(testPaper.getQuestionList())) {
            // 先删除原有的题目关联
            testPaperQuestionMapper.deleteByTestPaperId(testPaper.getId());
            
            // 重新添加题目关联
            for (TestPaper.QuestionItem item : testPaper.getQuestionList()) {
                Question question = questionService.selectById(item.getQuestionId());
                if (question == null)
                    continue;

                TestPaperQuestion tpq = new TestPaperQuestion();
                tpq.setTestPaperId(testPaper.getId());
                tpq.setQuestionId(item.getQuestionId());
                tpq.setSequence(item.getSequence());
                tpq.setScore(item.getScore() != null ? item.getScore() : 10);

                if ("composite".equals(question.getQuestionTypeVariety())) {
                    tpq.setType("father");
                    testPaperQuestionMapper.insert(tpq);

                    if (CollectionUtil.isNotEmpty(question.getQuestionList())) {
                        // 如果前端提供了子题分值，使用前端的分值；否则平均分配
                        if (CollectionUtil.isNotEmpty(item.getSubQuestions())) {
                            // 使用前端提供的子题分值
                            java.util.Map<Integer, Integer> subScoreMap = item.getSubQuestions().stream()
                                .collect(java.util.stream.Collectors.toMap(
                                    TestPaper.SubQuestionScore::getQuestionId,
                                    TestPaper.SubQuestionScore::getScore
                                ));
                            
                            for (Question subQ : question.getQuestionList()) {
                                TestPaperQuestion subTpq = new TestPaperQuestion();
                                subTpq.setTestPaperId(testPaper.getId());
                                subTpq.setQuestionId(subQ.getId());
                                subTpq.setSequence(item.getSequence());
                                subTpq.setScore(subScoreMap.getOrDefault(subQ.getId(), 1));
                                subTpq.setType("son");
                                testPaperQuestionMapper.insert(subTpq);
                            }
                        } else {
                            // 计算子题目分数：总分平均分配，余数加到最后一个子题
                            int totalScore = item.getScore() != null ? item.getScore() : 10;
                            int subQuestionCount = question.getQuestionList().size();
                            int averageScore = totalScore / subQuestionCount;
                            int remainder = totalScore % subQuestionCount;
                            
                            for (int j = 0; j < question.getQuestionList().size(); j++) {
                                Question subQ = question.getQuestionList().get(j);
                                TestPaperQuestion subTpq = new TestPaperQuestion();
                                subTpq.setTestPaperId(testPaper.getId());
                                subTpq.setQuestionId(subQ.getId());
                                subTpq.setSequence(item.getSequence());
                                // 最后一个子题加上余数
                                int subScore = averageScore + (j == subQuestionCount - 1 ? remainder : 0);
                                subTpq.setScore(subScore);
                                subTpq.setType("son");
                                testPaperQuestionMapper.insert(subTpq);
                            }
                        }
                    }
                } else {
                    tpq.setType("common");
                    testPaperQuestionMapper.insert(tpq);
                }
            }
            
            // 重新计算并保存试卷难度系数
            Double difficulty = calculateDifficulty(testPaper.getId());
            testPaper.setLevel(difficulty);
            testPaperMapper.updateById(testPaper);
        }
    }

    @Transactional
    public void deleteById(Integer id) {
        // 检查是否有关联的考试
        List<Test> tests = testMapper.selectByTestPaperId(id);
        if (tests != null && !tests.isEmpty()) {
            throw new com.example.exception.CustomException("-1", "该试卷已发放考试，删除失败");
        }
        // 删除试卷题目关联
        testPaperQuestionMapper.deleteByTestPaperId(id);
        // 删除试卷
        testPaperMapper.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Integer> ids) {
        // 先检查所有试卷是否都可以删除
        for (Integer id : ids) {
            List<Test> tests = testMapper.selectByTestPaperId(id);
            if (tests != null && !tests.isEmpty()) {
                throw new com.example.exception.CustomException("-1", "该试卷已发放考试，删除失败");
            }
        }
        // 所有检查通过后再删除
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    public TestPaper selectById(Integer id) {
        TestPaper testPaper = testPaperMapper.selectById(id);
        if (testPaper == null) {
            return null;
        }

        Course course = courseMapper.selectById(testPaper.getCourseId());
        if (ObjectUtil.isNotEmpty(course)) {
            testPaper.setCourseName(course.getName());
        }
        Teacher teacher = teacherMapper.selectById(testPaper.getTeacherId());
        if (ObjectUtil.isNotEmpty(teacher)) {
            testPaper.setTeacherName(teacher.getName());
        }

        // 获取试卷的所有题目
        List<TestPaperQuestion> tpqList = testPaperQuestionMapper.selectByTestPaperId(id);
        java.util.Map<Integer, TestPaperQuestion> qIdToTpq = tpqList.stream()
                .collect(java.util.stream.Collectors.toMap(TestPaperQuestion::getQuestionId, tpq -> tpq, (a, b) -> a));

        List<Question> questions = new ArrayList<>();
        for (TestPaperQuestion tpq : tpqList) {
            if ("son".equals(tpq.getType()))
                continue;
            Question question = questionService.selectById(tpq.getQuestionId());
            if (question != null) {
                question.setTestPaperQuestionId(tpq.getId()); // 设置关联ID
                question.setTypeScore(tpq.getScore()); // 设置题目分值

                if (CollectionUtil.isNotEmpty(question.getQuestionList())) {
                    for (Question subQ : question.getQuestionList()) {
                        TestPaperQuestion subTpq = qIdToTpq.get(subQ.getId());
                        if (subTpq != null) {
                            subQ.setTestPaperQuestionId(subTpq.getId());
                            subQ.setTypeScore(subTpq.getScore());
                        }
                    }
                }
                questions.add(question);
            }
        }
        testPaper.setQuestions(questions);

        // 优化后的统计逻辑：复用 analysis 的思路，支持查看学生详情
        List<Score> scores = scoreMapper.selectByPaperId(id);
        if (CollectionUtil.isEmpty(scores)) {
            return testPaper;
        }

        List<Integer> scoreIds = scores.stream().map(Score::getId).toList();
        List<Answer> allAnswers = answerMapper.selectBatchByScoreIds(scoreIds);

        // 组装数据
        java.util.Map<Integer, List<Answer>> answerMap = allAnswers.stream()
                .collect(java.util.stream.Collectors.groupingBy(Answer::getTestPaperQuestionId));

        java.util.Map<Integer, StudentAnalysisDTO> studentInfoMap = new java.util.HashMap<>();
        for (Score s : scores) {
            StudentAnalysisDTO dto = new StudentAnalysisDTO();
            dto.setStudentId(s.getStudentId());
            dto.setStudentName(s.getStudentName());
            dto.setStudentNumber(s.getStudentNumber());
            studentInfoMap.put(s.getId(), dto);
        }

        for (Question question : questions) {
            Integer tpqId = question.getTestPaperQuestionId();
            List<Answer> questionAnswers = answerMap.get(tpqId);

            if ("复合题".equals(question.getQuestionTypeName())) {
                List<Answer> compositeAnswers = new ArrayList<>();
                if (CollectionUtil.isNotEmpty(question.getQuestionList())) {
                    for (Question subQ : question.getQuestionList()) {
                        Integer subTpqId = subQ.getTestPaperQuestionId();
                        if (subTpqId != null && answerMap.containsKey(subTpqId)) {
                            compositeAnswers.addAll(answerMap.get(subTpqId));
                        }
                    }
                }
                calculateQuestionStats(question, compositeAnswers, studentInfoMap);
            } else {
                calculateQuestionStats(question, questionAnswers, studentInfoMap);
            }
        }

        return testPaper;
    }

    private void calculateQuestionStats(Question question, List<Answer> answers,
            java.util.Map<Integer, StudentAnalysisDTO> studentInfoMap) {
        if (CollectionUtil.isEmpty(answers))
            return;

        // 复合题处理
        if ("复合题".equals(question.getQuestionTypeName()) && CollectionUtil.isNotEmpty(question.getQuestionList())) {
            // 按子题目ID分组
            java.util.Map<Integer, List<Answer>> subAnswerMap = answers.stream()
                    .filter(a -> a.getQuestionId() != null)
                    .collect(java.util.stream.Collectors.groupingBy(Answer::getQuestionId));

            for (Question subQ : question.getQuestionList()) {
                List<Answer> subAnswers = subAnswerMap.get(subQ.getId());
                calculateQuestionStats(subQ, subAnswers, studentInfoMap);
            }
            return;
        }

        question.setTotalCount(answers.size());

        int rightCount = 0;
        int errorCount = 0;
        int partRightCount = 0;
        List<StudentAnalysisDTO> studentList = new ArrayList<>();

        for (Answer ans : answers) {
            StudentAnalysisDTO baseInfo = studentInfoMap.get(ans.getScoreId());
            if (baseInfo != null) {
                StudentAnalysisDTO dto = new StudentAnalysisDTO();
                org.springframework.beans.BeanUtils.copyProperties(baseInfo, dto);
                dto.setScore(ans.getScore());
                dto.setAnswerContent(ans.getAnswer());

                String status = "未知";
                if (ans.getScore() != null) {
                    Integer maxScore = ans.getQuestionScore();
                    if (maxScore == null) {
                        maxScore = question.getTypeScore();
                    }

                    if (maxScore != null) {
                        if (ans.getScore().equals(maxScore)) {
                            status = "正确";
                            rightCount++;
                        } else if (ans.getScore() == 0) {
                            status = "错误";
                            errorCount++;
                        } else {
                            status = "半对";
                            partRightCount++;
                        }
                    }
                }
                dto.setStatus(status);
                studentList.add(dto);
            }
        }

        question.setRightCount(rightCount);
        question.setErrorCount(errorCount);
        question.setPartRightCount(partRightCount);
        question.setStudentList(studentList);
    }

    public List<TestPaper> selectAll(TestPaper testPaper) {
        return testPaperMapper.selectAll(testPaper);
    }

    public PageInfo<TestPaper> selectPage(TestPaper testPaper, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.TEACHER.name().equals(currentUser.getRole())) {
            testPaper.setTeacherId(currentUser.getId());
        }
        PageHelper.startPage(pageNum, pageSize);
        List<TestPaper> list = testPaperMapper.selectAll(testPaper);
        return PageInfo.of(list);
    }

    public void checkTestPaper(Integer testId) {
        Account currentUser = TokenUtils.getCurrentUser();
        Score score = new Score();
        score.setTestId(testId);
        score.setStudentId(currentUser.getId());
        List<Score> scores = scoreMapper.selectAll(score);
        if (CollectionUtil.isNotEmpty(scores)) {
            throw new CustomException("-1", "该门考试您已经提交过试卷了，入口已关闭");
        }
    }

    public List<TestPaper> selectRandom() {
        List<TestPaper> list = testPaperMapper.selectAll(new TestPaper());
        Collections.shuffle(list);
        if (list.size() > 4) {
            return list.subList(0, 4);
        }
        return list;
    }

    public TestPaper analysis(Integer testPaperId, Integer testId) {
        // 1. 获取试卷基本信息和题目
        TestPaper testPaper = selectById(testPaperId);
        if (testPaper == null)
            return null;

        // 清空selectById中可能已经计算的统计信息
        if (CollectionUtil.isNotEmpty(testPaper.getQuestions())) {
            for (Question q : testPaper.getQuestions()) {
                q.setTotalCount(0);
                q.setRightCount(0);
                q.setErrorCount(0);
                q.setPartRightCount(0);
                q.setStudentList(new ArrayList<>());
            }
        } else {
            return testPaper;
        }

        // 2. 查询该次考试的所有成绩记录（包含学生信息）
        Score scoreQuery = new Score();
        scoreQuery.setTestId(testId);
        List<Score> scoreList = scoreMapper.selectAll(scoreQuery);

        if (CollectionUtil.isEmpty(scoreList)) {
            return testPaper;
        }

        // 3. 查询该次考试的所有答题记录
        List<Integer> scoreIds = scoreList.stream().map(Score::getId).toList();
        if (CollectionUtil.isEmpty(scoreIds)) {
            return testPaper;
        }

        List<Answer> allAnswers = answerMapper.selectBatchByScoreIds(scoreIds);

        // 4. 组装数据
        java.util.Map<Integer, List<Answer>> answerMap = allAnswers.stream()
                .collect(java.util.stream.Collectors.groupingBy(Answer::getTestPaperQuestionId));

        java.util.Map<Integer, StudentAnalysisDTO> studentInfoMap = new java.util.HashMap<>();
        for (Score s : scoreList) {
            StudentAnalysisDTO dto = new StudentAnalysisDTO();
            dto.setStudentId(s.getStudentId());
            dto.setStudentName(s.getStudentName());
            dto.setStudentNumber(s.getStudentNumber());
            studentInfoMap.put(s.getId(), dto);
        }

        for (Question question : testPaper.getQuestions()) {
            Integer tpqId = question.getTestPaperQuestionId();
            List<Answer> questionAnswers = answerMap.get(tpqId);

            if ("复合题".equals(question.getQuestionTypeName())) {
                List<Answer> compositeAnswers = new ArrayList<>();
                if (CollectionUtil.isNotEmpty(question.getQuestionList())) {
                    for (Question subQ : question.getQuestionList()) {
                        Integer subTpqId = subQ.getTestPaperQuestionId();
                        if (subTpqId != null && answerMap.containsKey(subTpqId)) {
                            compositeAnswers.addAll(answerMap.get(subTpqId));
                        }
                    }
                }
                calculateQuestionStats(question, compositeAnswers, studentInfoMap);
            } else {
                calculateQuestionStats(question, questionAnswers, studentInfoMap);
            }
        }

        return testPaper;
    }

    /**
     * 计算试卷难度系数
     * 公式：试卷难度系数 = Σ(每道题的难度系数 × 该题分值 / 试卷总分)
     * 
     * @param testPaperId 试卷ID
     * @return 难度系数（0-1之间）
     */
    public Double calculateDifficulty(Integer testPaperId) {
        // 获取试卷所有题目
        List<TestPaperQuestion> testPaperQuestions = testPaperQuestionMapper.selectByTestPaperId(testPaperId);
        if (CollectionUtil.isEmpty(testPaperQuestions)) {
            return 0.0;
        }

        // 计算总分
        int totalScore = testPaperQuestions.stream()
            .filter(tpq -> !"son".equals(tpq.getType())) // 排除复合题的子题
            .mapToInt(TestPaperQuestion::getScore)
            .sum();

        if (totalScore == 0) {
            return 0.0;
        }

        // 计算加权难度系数
        double weightedDifficulty = 0.0;
        for (TestPaperQuestion tpq : testPaperQuestions) {
            // 复合题的子题不参与计算（分值已计入父题）
            if ("son".equals(tpq.getType())) {
                continue;
            }

            Question question = questionMapper.selectById(tpq.getQuestionId());
            if (question != null && question.getLevel() != null) {
                double weight = (double) tpq.getScore() / totalScore;
                weightedDifficulty += question.getLevel() * weight;
            }
        }

        // 保留两位小数
        return Math.round(weightedDifficulty * 100.0) / 100.0;
    }

    /**
     * 根据难度系数范围随机选题
     * 
     * @param testPaper 试卷对象
     * @param count 需要选择的题目数量
     * @param list 结果列表
     * @param typeId 题型ID
     * @param typeName 题型名称
     * @param targetDifficulty 目标难度系数
     * @param difficultyTolerance 难度容差
     */
    private void randomQuestionIdsByDifficulty(TestPaper testPaper, Integer count, List<Integer> list, 
                                               Integer typeId, String typeName, Double targetDifficulty, 
                                               Double difficultyTolerance) {
        if (count == null || count == 0) {
            return;
        }

        List<Question> questionList;
        
        // 根据题型名称选择不同的查询策略
        if ("单选".equals(typeName) || "多选".equals(typeName) || "判断".equals(typeName) || "填空".equals(typeName)) {
            // 单选、多选、判断、填空：根据题型名称精确查询
            questionList = questionMapper.selectByCourseIdAndTypeName(testPaper.getCourseId(), typeName);
        } else if ("简答".equals(typeName)) {
            // 简答题：从除填空以外的text类题目中随机选择（名词解析、论述、计算、程序）
            questionList = questionMapper.selectByCourseIdTextExceptFillIn(testPaper.getCourseId());
            typeName = "简答（主观）";
        } else if ("复合".equals(typeName)) {
            // 复合题：从composite类题目中随机选择（资料、完形填空、阅读理解、综合）
            questionList = questionMapper.selectByCourseIdAndVariety(testPaper.getCourseId(), "composite");
            typeName = "复合";
        } else {
            // 兼容旧的typeId查询方式
            questionList = questionMapper.selectByCouserIdAndTypeId(testPaper.getCourseId(), typeId);
        }
        
        // 如果指定了目标难度系数，筛选难度接近的题目
        if (targetDifficulty != null && targetDifficulty >= 0) {
            final double minDiff1 = Math.max(0.0, targetDifficulty - difficultyTolerance);
            final double maxDiff1 = Math.min(1.0, targetDifficulty + difficultyTolerance);
            
            List<Question> filteredList = questionList.stream()
                .filter(q -> q.getLevel() != null && q.getLevel() >= minDiff1 && q.getLevel() <= maxDiff1)
                .toList();
            
            // 如果筛选后的题目不足，逐步放宽容差
            if (filteredList.size() < count) {
                double newTolerance = difficultyTolerance + 0.1;
                final double minDiff2 = Math.max(0.0, targetDifficulty - newTolerance);
                final double maxDiff2 = Math.min(1.0, targetDifficulty + newTolerance);
                
                filteredList = questionList.stream()
                    .filter(q -> q.getLevel() != null && q.getLevel() >= minDiff2 && q.getLevel() <= maxDiff2)
                    .toList();
                
                // 如果还不够，使用所有题目
                if (filteredList.size() < count) {
                    filteredList = questionList;
                }
            }
            
            questionList = new ArrayList<>(filteredList);
        }
        
        if (questionList.size() < count) {
            throw new CustomException("-1",
                    "您的题库里该课程的" + typeName + "数量不足，请减少出题的" + typeName + "数量或者增加题库里该课程的" + typeName + "数量");
        }
        
        Collections.shuffle(questionList);
        List<Integer> questions = questionList.subList(0, count).stream().map(Question::getId).toList();
        list.addAll(questions);
    }
}
