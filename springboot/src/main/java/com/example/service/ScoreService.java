package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.dto.Account;
import com.example.common.enums.RoleEnum;
import com.example.common.dto.TestSubmitDTO;
import com.example.exception.CustomException;
import com.example.entity.*;
import com.example.mapper.QuestionMapper;
import com.example.mapper.ScoreMapper;
import com.example.common.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 成绩信息业务层方法
 */
@Service
public class ScoreService {

    @Resource
    private ScoreMapper scoreMapper;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private com.example.mapper.AnswerMapper answerMapper;
    @Resource
    private com.example.mapper.TestPaperQuestionMapper testPaperQuestionMapper;
    @Resource
    private com.example.mapper.ChoiceMapper choiceMapper;
    @Resource
    private com.example.mapper.CompositeMapper compositeMapper;
    @Resource
    private com.example.mapper.TestMapper testMapper;
    @Resource
    private com.example.mapper.QuestionTypeMapper questionTypeMapper;
    @Resource
    private TencentCosService tencentCosService;
    
    @Value("${tencent.cos.baseUrl}")
    private String cosBaseUrl;

    public void add(TestSubmitDTO submitDTO) {
        // 获取考试ID和作弊标记
        Integer testId = submitDTO.getId();
        Integer cheat = submitDTO.getCheat() != null ? submitDTO.getCheat() : 0;
        
        // 查询考试信息
        Test test = testMapper.selectById(testId);
        if (test == null) {
            throw new CustomException("-1", "考试不存在");
        }

        // 封装一下用户提交的试卷信息
        List<Answer> list = new ArrayList<>();
        for (Question question : submitDTO.getTestPaper().getQuestions()) {
            if ("composite".equals(question.getQuestionTypeVariety())) {
                // 先保存复合题父题目的记录
                Answer parentAnswer = new Answer();
                parentAnswer.setQuestion(question);
                parentAnswer.setQuestionId(question.getId());
                parentAnswer.setTestPaperQuestionId(question.getTestPaperQuestionId());
                parentAnswer.setAnswer(""); // 复合题父题目没有答案
                list.add(parentAnswer);
                
                // 再保存子题目
                if (question.getQuestionList() != null) {
                    for (Question subQ : question.getQuestionList()) {
                        Answer answer = new Answer();
                        answer.setQuestion(subQ);
                        answer.setQuestionId(subQ.getId());
                        answer.setTestPaperQuestionId(subQ.getTestPaperQuestionId());

                        if ("多选".equals(subQ.getQuestionTypeName())) {
                            List<String> checkList = subQ.getCheckList();
                            if (checkList != null && !checkList.isEmpty()) {
                                // 对答案进行排序，保证保存顺序一致
                                List<String> sortedList = new ArrayList<>(checkList);
                                java.util.Collections.sort(sortedList);
                                answer.setAnswer(String.join(",", sortedList));
                            } else {
                                answer.setAnswer("");
                            }
                        } else {
                            answer.setAnswer(subQ.getNewAnswer());
                        }
                        list.add(answer);
                    }
                }
            } else {
                Answer answer = new Answer();
                answer.setQuestion(question);
                answer.setQuestionId(question.getId());
                answer.setTestPaperQuestionId(question.getTestPaperQuestionId()); // 保存关联ID
                if ("多选".equals(question.getQuestionTypeName())) {
                    List<String> checkList = question.getCheckList();
                    if (checkList != null && !checkList.isEmpty()) {
                        // 对答案进行排序，保证保存顺序一致
                        List<String> sortedList = new ArrayList<>(checkList);
                        java.util.Collections.sort(sortedList);
                        answer.setAnswer(String.join(",", sortedList));
                    } else {
                        answer.setAnswer("");
                    }
                } else {
                    answer.setAnswer(question.getNewAnswer());
                }
                list.add(answer);
            }
        }

        Score score = new Score();
        Account currentUser = TokenUtils.getCurrentUser();
        score.setStudentId(currentUser.getId());
        score.setTestId(test.getId());
        
        // 使用从submitData中提取的cheat标记
        score.setCheat(cheat);
        
        // 如果作弊，成绩为0且标记为待批改，不保存答案记录
        if (cheat == 1) {
            score.setScore(0);
            score.setStatus("待批改");
            score.setAnswers(null);  // 作弊不保存答案
            // 插入 score
            scoreMapper.insert(score);
            return;  // 直接返回，不处理答案
        }
        
        // 正常考试流程
        score.setScore(0); // 初始总分为0，等待教师批改后统计
        score.setStatus("待批改");
        score.setAnswers(list);
        // 插入 score 并获取生成的 id
        scoreMapper.insert(score);
        
        // 客观题自动判分（只设置answer的分数，不更新score表总分）
        if (score.getId() != null) {
            for (Answer ans : list) {
                ans.setScoreId(score.getId());
                
                // 获取题目信息
                Question question = questionMapper.selectById(ans.getQuestionId());
                if (question == null) {
                    ans.setStatus("待批改");
                    ans.setScore(0);
                    answerMapper.insert(ans);
                    continue;
                }
                
                // 获取题目分值
                TestPaperQuestion tpq = null;
                if (ans.getTestPaperQuestionId() != null) {
                    tpq = testPaperQuestionMapper.selectById(ans.getTestPaperQuestionId());
                }
                int questionScore = (tpq != null && tpq.getScore() != null) ? tpq.getScore() : 0;
                
                String typeName = question.getQuestionTypeName();
                
                // 客观题自动判分：单选、多选、判断
                if ("单选".equals(typeName) || "多选".equals(typeName) || "判断".equals(typeName)) {
                    // 获取选项列表
                    List<com.example.entity.Choice> choiceList = choiceMapper.selectByQuestionId(question.getId());
                    if (choiceList != null && !choiceList.isEmpty()) {
                        // 构建正确答案（所有flag=true的选项标识）
                        StringBuilder correctAnswer = new StringBuilder();
                        for (com.example.entity.Choice choice : choiceList) {
                            if (choice.isFlag()) {
                                correctAnswer.append(choice.getIdentification());
                            }
                        }
                        
                        // 获取学生答案并标准化
                        String studentAnswer = (ans.getAnswer() != null) ? ans.getAnswer().replace(",", "").replace(" ", "").trim() : "";
                        String correct = correctAnswer.toString();
                        
                        // 多选题判分逻辑
                        if ("多选".equals(typeName)) {
                            if (studentAnswer.isEmpty()) {
                                // 未作答得0分
                                ans.setScore(0);
                                ans.setStatus("已批改");
                            } else {
                                // 将学生答案和正确答案转换为字符集合
                                java.util.Set<Character> studentSet = new java.util.HashSet<>();
                                java.util.Set<Character> correctSet = new java.util.HashSet<>();
                                
                                for (char c : studentAnswer.toCharArray()) {
                                    studentSet.add(c);
                                }
                                for (char c : correct.toCharArray()) {
                                    correctSet.add(c);
                                }
                                
                                // 检查是否有错误选项
                                boolean hasWrongChoice = false;
                                for (char c : studentSet) {
                                    if (!correctSet.contains(c)) {
                                        hasWrongChoice = true;
                                        break;
                                    }
                                }
                                
                                if (hasWrongChoice) {
                                    // 有错误选项，得0分
                                    ans.setScore(0);
                                    ans.setStatus("已批改");
                                } else if (studentSet.size() == correctSet.size()) {
                                    // 完全正确，满分
                                    ans.setScore(questionScore);
                                    ans.setStatus("已批改");
                                } else {
                                    // 漏选，一半分数向上取整
                                    ans.setScore((int) Math.ceil(questionScore / 2.0));
                                    ans.setStatus("已批改");
                                }
                            }
                        } else {
                            // 单选题和判断题：完全相同才得分
                            if (studentAnswer.equals(correct)) {
                                ans.setScore(questionScore);
                                ans.setStatus("已批改");
                            } else {
                                ans.setScore(0);
                                ans.setStatus("已批改");
                            }
                        }
                    } else {
                        // 没有选项，待批改
                        ans.setStatus("待批改");
                        ans.setScore(0);
                    }
                } else {
                    // 主观题待批改
                    ans.setStatus("待批改");
                    ans.setScore(0);
                }
                
                answerMapper.insert(ans);
            }
            
            // 注意：不在这里更新score表的总分，保持为0，等待教师批改后再统计
        }
    }

    public void updateById(Score score) {
        List<Answer> answerData = score.getAnswers();
        int total = 0;
        // 计算总分时，排除复合题的子题（type='son'）
        for (Answer answer : answerData) {
            if (ObjectUtil.isNotEmpty(answer.getScore())) {
                // 获取题目类型，如果是子题则不计入总分
                if (answer.getTestPaperQuestionId() != null) {
                    TestPaperQuestion tpq = testPaperQuestionMapper.selectById(answer.getTestPaperQuestionId());
                    // 只有不是son类型的题目才计入总分
                    if (tpq != null && !"son".equals(tpq.getType())) {
                        total += answer.getScore();
                    }
                } else {
                    // 如果没有testPaperQuestionId，也计入总分
                    total += answer.getScore();
                }
            }
        }
        score.setScore(total);
        score.setAnswers(answerData);
        score.setStatus("已批改");
        // 更新总分
        scoreMapper.updateById(score);
        // 更新每个 answer 条目
        if (answerData != null) {
            for (Answer ans : answerData) {
                ans.setStatus("已批改"); // 设置答案状态为已批改
                if (ans.getId() != null) {
                    answerMapper.updateById(ans);
                } else {
                    // 新增的answer记录（可能是复合题的子题）
                    ans.setScoreId(score.getId());
                    answerMapper.insert(ans);
                }
            }
        }
    }

    public void deleteById(Integer id) {
        scoreMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            scoreMapper.deleteById(id);
        }
    }

    public Score selectById(Integer id) {
        Score score = scoreMapper.selectById(id);
        // 学生只能查看已批改的成绩
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.STUDENT.name().equals(currentUser.getRole())) {
            if (!"已批改".equals(score.getStatus())) {
                throw new CustomException("-1", "试卷尚未批改完成，暂时无法查看");
            }
        }
        List<Answer> list = score.getAnswers();
        if (list != null && !list.isEmpty()) {
            List<Question> questions = new ArrayList<>();
            for (Answer ans : list) {
                Question question = questionMapper.selectById(ans.getQuestionId());
                if (ObjectUtil.isNotEmpty(question)) {
                    // 为单选题、多选题、判断题加载选项列表
                    String questionType = question.getQuestionTypeName();
                    if ("单选".equals(questionType) || "多选".equals(questionType) || "判断".equals(questionType)) {
                        List<Choice> choiceList = choiceMapper.selectByQuestionId(question.getId());
                        question.setChoiceList(choiceList);
                    }

                    if ("多选".equals(questionType)) {
                        String newAnswer = ans.getAnswer(); // A,B,C
                        if (newAnswer != null && !newAnswer.isEmpty()) {
                            List<String> checkList = Arrays.asList(newAnswer.split(","));
                            question.setCheckList(checkList);
                        }
                    } else {
                        question.setNewAnswer(ans.getAnswer());
                    }
                    
                    // 复合题：加载子题目列表（判断questionTypeVariety或questionTypeName）
                    if ("composite".equals(question.getQuestionTypeVariety()) || "复合".equals(questionType)) {
                        List<Composite> composites = compositeMapper.selectByFatherId(question.getId());
                        if (composites != null && !composites.isEmpty()) {
                            List<Question> subQuestions = new ArrayList<>();
                            for (Composite composite : composites) {
                                Question subQ = questionMapper.selectById(composite.getQuestionSonId());
                                if (subQ != null) {
                                    // 加载子题目选项
                                    String subQType = subQ.getQuestionTypeName();
                                    if ("单选".equals(subQType) || "多选".equals(subQType) || "判断".equals(subQType)) {
                                        List<Choice> subChoiceList = choiceMapper.selectByQuestionId(subQ.getId());
                                        subQ.setChoiceList(subChoiceList);
                                    }
                                    
                                    // 查找子题目的答题记录
                                    Answer subAnswer = list.stream()
                                        .filter(a -> a.getQuestionId().equals(subQ.getId()))
                                        .findFirst()
                                        .orElse(null);
                                    
                                    if (subAnswer != null) {
                                        if ("多选".equals(subQType)) {
                                            String subNewAnswer = subAnswer.getAnswer();
                                            if (subNewAnswer != null && !subNewAnswer.isEmpty()) {
                                                List<String> subCheckList = Arrays.asList(subNewAnswer.split(","));
                                                subQ.setCheckList(subCheckList);
                                            }
                                        } else {
                                            subQ.setNewAnswer(subAnswer.getAnswer());
                                        }
                                        
                                        // 设置子题目分值和得分
                                        if (subAnswer.getTestPaperQuestionId() != null) {
                                            TestPaperQuestion subTpq = testPaperQuestionMapper.selectById(subAnswer.getTestPaperQuestionId());
                                            if (subTpq != null) {
                                                subQ.setTypeScore(subTpq.getScore());
                                                subQ.setPaperQuestionType(subTpq.getType());
                                            }
                                        }
                                        subQ.setAnswerScore(subAnswer.getScore());
                                    }
                                    
                                    subQuestions.add(subQ);
                                }
                            }
                            question.setQuestionList(subQuestions);
                        }
                    }
                    
                    // 设置题目分值（从testPaperQuestion获取）和学生得分（从answer获取）
                    if (ans.getTestPaperQuestionId() != null) {
                        TestPaperQuestion tpq = testPaperQuestionMapper.selectById(ans.getTestPaperQuestionId());
                        if (tpq != null) {
                            question.setTypeScore(tpq.getScore());
                            question.setPaperQuestionType(tpq.getType()); // 设置题目类型：common/father/son
                        }
                    }
                    // 设置学生该题获得的分数
                    question.setAnswerScore(ans.getScore());
                    questions.add(question);
                }
            }
            score.setQuestions(questions);
        }
        return score;
    }

    public List<Score> selectAll(Score score) {
        return scoreMapper.selectAll(score);
    }

    public PageInfo<Score> selectPage(Score score, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.STUDENT.name().equals(currentUser.getRole())) {
            score.setStudentId(currentUser.getId());
        }
        if (RoleEnum.TEACHER.name().equals(currentUser.getRole())) {
            score.setTeacherId(currentUser.getId());
        }
        PageHelper.startPage(pageNum, pageSize);
        List<Score> list = scoreMapper.selectAll(score);
        return PageInfo.of(list);
    }

    public List<Answer> selectAnswer(Integer id) {
        Score score = scoreMapper.selectById(id);
        
        // 检查是否作弊 - 如果作弊返回空列表
        if (score.getCheat() != null && score.getCheat() == 1) {
            return new ArrayList<>();
        }
        
        // 学生只能查看已批改的答案
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.STUDENT.name().equals(currentUser.getRole())) {
            if (!"已批改".equals(score.getStatus())) {
                throw new CustomException("-1", "试卷尚未批改完成，暂时无法查看");
            }
        }
        List<Answer> list = score.getAnswers();

        // 用于存储已处理的题目ID，避免重复处理复合题的子题
        List<Integer> processedQuestionIds = new ArrayList<>();
        List<Answer> filteredList = new ArrayList<>();

        // 客观题自动阅卷功能
        for (Answer answer : list) {
            Question question = questionMapper.selectById(answer.getQuestionId());

            // 如果题目不存在，跳过
            if (question == null) {
                continue;
            }

            // 加载题目的选项列表
            question.setChoiceList(choiceMapper.selectByQuestionId(question.getId()));

            // 对于选择题和判断题，将answer字段从选项内容转换为选项标识（A、B、C等）
            if (("单选".equals(question.getQuestionTypeName()) ||
                    "多选".equals(question.getQuestionTypeName()) ||
                    "判断".equals(question.getQuestionTypeName())) &&
                    question.getChoiceList() != null && !question.getChoiceList().isEmpty()) {

                // 找出所有正确答案的选项标识
                StringBuilder correctIdentifications = new StringBuilder();
                for (com.example.entity.Choice choice : question.getChoiceList()) {
                    if (choice.isFlag()) {
                        correctIdentifications.append(choice.getIdentification());
                    }
                }

                // 如果找到了正确答案标识，更新question的answer字段
                if (correctIdentifications.length() > 0) {
                    question.setAnswer(correctIdentifications.toString());
                }
            }

            // 如果是复合题的子题（flag=0），跳过，因为会在父题中处理
            if (!question.isFlag()) {
                continue;
            }

            // 如果是复合题父题，加载所有子题及其答案
            if ("composite".equals(question.getQuestionTypeVariety())) {
                List<com.example.entity.Composite> composites = compositeMapper.selectByFatherId(question.getId());
                if (composites != null && !composites.isEmpty()) {
                    List<Question> subQuestions = new ArrayList<>();
                    List<String> subAnswers = new ArrayList<>();

                    for (com.example.entity.Composite composite : composites) {
                        Question subQ = questionMapper.selectById(composite.getQuestionSonId());
                        if (subQ != null) {
                            subQ.setChoiceList(choiceMapper.selectByQuestionId(subQ.getId()));

                            // 对于选择题和判断题，将answer字段从选项内容转换为选项标识
                            if (("单选".equals(subQ.getQuestionTypeName()) ||
                                    "多选".equals(subQ.getQuestionTypeName()) ||
                                    "判断".equals(subQ.getQuestionTypeName())) &&
                                    subQ.getChoiceList() != null && !subQ.getChoiceList().isEmpty()) {

                                StringBuilder correctIdentifications = new StringBuilder();
                                for (com.example.entity.Choice choice : subQ.getChoiceList()) {
                                    if (choice.isFlag()) {
                                        correctIdentifications.append(choice.getIdentification());
                                    }
                                }

                                if (correctIdentifications.length() > 0) {
                                    subQ.setAnswer(correctIdentifications.toString());
                                }
                            }

                            subQuestions.add(subQ);

                            // 查找该子题的学生答案
                            String subAnswer = "";
                            for (Answer ans : list) {
                                if (ans.getQuestionId().equals(subQ.getId())) {
                                    subAnswer = ans.getAnswer() != null ? ans.getAnswer() : "";
                                    break;
                                }
                            }
                            subAnswers.add(subAnswer);

                            // 标记子题已处理
                            processedQuestionIds.add(subQ.getId());
                        }
                    }
                    // 为子题设置分数（从test_paper_question表获取）
                    for (Question subQ : subQuestions) {
                        for (Answer ans : list) {
                            if (ans.getQuestionId().equals(subQ.getId()) && ans.getTestPaperQuestionId() != null) {
                                TestPaperQuestion tpq = testPaperQuestionMapper.selectById(ans.getTestPaperQuestionId());
                                if (tpq != null) {
                                    subQ.setTypeScore(tpq.getScore());
                                }
                                break;
                            }
                        }
                    }
                    
                    question.setQuestionList(subQuestions);

                    // 将子题答案数组存储到answer对象的一个扩展字段中
                    if (!subAnswers.isEmpty()) {
                        // 将子答案用逗号分隔存储
                        answer.setAnswer(String.join(",", subAnswers));
                    }
                    
                    // 获取子题的answer记录ID、分数和最大分数，供前端编辑使用
                    List<Integer> subAnswerIds = new ArrayList<>();
                    List<Integer> subScores = new ArrayList<>();
                    List<Integer> subTPQIds = new ArrayList<>();
                    List<Integer> subMaxScores = new ArrayList<>();
                    for (com.example.entity.Composite composite : composites) {
                        for (Answer ans : list) {
                            if (ans.getQuestionId().equals(composite.getQuestionSonId())) {
                                subAnswerIds.add(ans.getId());
                                subScores.add(ans.getScore() != null ? ans.getScore() : 0);
                                subTPQIds.add(ans.getTestPaperQuestionId());
                                
                                // 获取子题的最大分数
                                if (ans.getTestPaperQuestionId() != null) {
                                    TestPaperQuestion tpq = testPaperQuestionMapper.selectById(ans.getTestPaperQuestionId());
                                    subMaxScores.add(tpq != null ? tpq.getScore() : 0);
                                } else {
                                    subMaxScores.add(0);
                                }
                                break;
                            }
                        }
                    }
                    // 将子题信息存储到answer的扩展字段
                    answer.setSubQuestionAnswerIds(subAnswerIds);
                    answer.setSubScoreData(subScores);
                    answer.setSubQuestionTPQIds(subTPQIds);
                    answer.setSubMaxScores(subMaxScores);
                }
            }

            answer.setQuestion(question);

            // 获取题目分值，如果为null则默认为0
            Integer questionScore = answer.getQuestionScore();
            if (questionScore == null) {
                questionScore = 0;
            }

            // 客观题自动打分
            String questionType = answer.getQuestion().getQuestionTypeName();
            if (!"简答题".equals(questionType) && !"填空题".equals(questionType) && !"复合题".equals(questionType)) {
                String studentAnswer = answer.getAnswer();

                // 从choice表获取正确答案（flag=true的选项）
                List<com.example.entity.Choice> choices = choiceMapper.selectByQuestionId(answer.getQuestionId());
                List<String> correctAnswers = new ArrayList<>();
                for (com.example.entity.Choice choice : choices) {
                    if (choice.isFlag()) {
                        correctAnswers.add(choice.getIdentification());
                    }
                }

                if ("单选题".equals(questionType) || "判断题".equals(questionType)) {
                    // 单选题、判断题：只有一个正确答案
                    String correctAnswer = correctAnswers.isEmpty() ? "" : correctAnswers.get(0);
                    if (studentAnswer != null && studentAnswer.equals(correctAnswer)) {
                        answer.setScore(questionScore);
                    } else {
                        answer.setScore(0);
                    }
                } else if ("多选题".equals(questionType)) {
                    // 多选题：根据新逻辑判分
                    if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
                        // 未作答得0分
                        answer.setScore(0);
                    } else {
                        // 多选题答案格式为 "A,B,C"，需要用逗号分隔，并去除空格
                        String[] studentArray = studentAnswer.split(",");
                        List<String> studentList = new ArrayList<>();
                        for (String s : studentArray) {
                            studentList.add(s.trim()); // 去除每个选项的空格
                        }
                        
                        boolean hasWrongChoice = false;

                        // 检查是否选择了错误选项
                        for (String s : studentList) {
                            if (!correctAnswers.contains(s)) {
                                hasWrongChoice = true;
                                break;
                            }
                        }

                        if (hasWrongChoice) {
                            // 有错误选项，得0分
                            answer.setScore(0);
                        } else if (studentList.size() == correctAnswers.size()) {
                            // 完全正确，满分
                            answer.setScore(questionScore);
                        } else {
                            // 部分正确，一半分数向上取整
                            answer.setScore((int) Math.ceil(questionScore / 2.0));
                        }
                    }
                }
            }

            filteredList.add(answer);
        }

        return filteredList;
    }

    /**
     * 根据考试ID查询学生成绩列表（用于按学生批阅）
     */
    public PageInfo<Score> selectByTest(Integer testId, Integer clazzId, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Score> list = scoreMapper.selectByTestIdAndClazz(testId, clazzId);
        return PageInfo.of(list);
    }

    /**
     * 根据考试ID查询需要批阅的题目列表（用于按题目批阅）
     * 返回所有主观题（不包括客观题）
     */
    public List<java.util.Map<String, Object>> selectQuestionsByTest(Integer testId, Integer clazzId) {
        return scoreMapper.selectQuestionsByTestIdAndClazz(testId, clazzId);
    }

    /**
     * 根据题目ID查询所有学生的答案（用于按题目批阅）
     */
    public java.util.Map<String, Object> selectAnswersByQuestion(Integer testId, Integer questionId) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        
        // 1. 获取题目信息
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            return result;
        }
        
        // 2. 获取题目类型信息
        QuestionType questionType = questionTypeMapper.selectById(question.getQuestionTypeId());
        
        // 3. 获取试卷中该题的分值
        Test test = testMapper.selectById(testId);
        Integer testPaperId = test.getTestPaperId();
        List<TestPaperQuestion> tpqList = testPaperQuestionMapper.selectByTestPaperId(testPaperId);
        TestPaperQuestion currentTpq = null;
        for (TestPaperQuestion tpq : tpqList) {
            if (tpq.getQuestionId().equals(questionId)) {
                currentTpq = tpq;
                break;
            }
        }
        
        // 4. 构建题目信息
        java.util.Map<String, Object> questionInfo = new java.util.HashMap<>();
        questionInfo.put("id", question.getId());
        questionInfo.put("questionStem", question.getQuestionStem());
        questionInfo.put("questionTypeName", questionType.getName());
        questionInfo.put("questionTypeVariety", questionType.getVariety());
        questionInfo.put("answer", question.getAnswer());
        questionInfo.put("typeScore", currentTpq != null ? currentTpq.getScore() : question.getTypeScore());
        
        // 4. 如果是选择题，获取选项
        if ("choice".equals(questionType.getVariety())) {
            List<Choice> choiceList = choiceMapper.selectByQuestionId(questionId);
            questionInfo.put("choiceList", choiceList);
        }
        
        // 5. 如果是复合题，获取子题列表
        if ("composite".equals(questionType.getVariety())) {
            List<Question> subQuestions = questionMapper.selectByParentId(questionId);
            List<java.util.Map<String, Object>> subQuestionList = new ArrayList<>();
            
            for (Question subQ : subQuestions) {
                QuestionType subType = questionTypeMapper.selectById(subQ.getQuestionTypeId());
                
                // 获取子题在试卷中的分值
                TestPaperQuestion subTpq = null;
                for (TestPaperQuestion tpq : tpqList) {
                    if (tpq.getQuestionId().equals(subQ.getId())) {
                        subTpq = tpq;
                        break;
                    }
                }
                
                java.util.Map<String, Object> subQMap = new java.util.HashMap<>();
                subQMap.put("id", subQ.getId());
                subQMap.put("questionStem", subQ.getQuestionStem());
                subQMap.put("questionTypeName", subType.getName());
                subQMap.put("questionTypeVariety", subType.getVariety());
                subQMap.put("answer", subQ.getAnswer());
                subQMap.put("typeScore", subTpq != null ? subTpq.getScore() : subQ.getTypeScore());
                
                if ("choice".equals(subType.getVariety())) {
                    List<Choice> subChoices = choiceMapper.selectByQuestionId(subQ.getId());
                    subQMap.put("choiceList", subChoices);
                }
                
                subQuestionList.add(subQMap);
            }
            questionInfo.put("questionList", subQuestionList);
        }
        
        // 6. 获取所有学生的答案
        List<java.util.Map<String, Object>> studentList = scoreMapper.selectStudentAnswersByQuestion(testId, questionId);
        
        // 7. 解析学生答案数据（处理复合题的多个子答案）
        for (java.util.Map<String, Object> student : studentList) {
            String answers = (String) student.get("answers");
            String scores = (String) student.get("scores");
            String statuses = (String) student.get("statuses");
            String questionIds = (String) student.get("questionIds");
            
            if ("composite".equals(questionType.getVariety())) {
                // 复合题：解析子题答案
                List<java.util.Map<String, Object>> subAnswers = new ArrayList<>();
                if (answers != null && !answers.isEmpty()) {
                    String[] answerArray = answers.split("\\|\\|\\|");
                    String[] scoreArray = scores != null ? scores.split(",") : new String[0];
                    String[] statusArray = statuses != null ? statuses.split("\\|\\|\\|") : new String[0];
                    
                    // 第一个是父题目的answer/score/status
                    student.put("status", statusArray.length > 0 ? statusArray[0] : "未批改");
                    
                    // 从索引1开始才是子题的
                    for (int i = 1; i < answerArray.length; i++) {
                        java.util.Map<String, Object> subAnswer = new java.util.HashMap<>();
                        subAnswer.put("answer", answerArray[i]);
                        subAnswer.put("score", i < scoreArray.length ? Double.parseDouble(scoreArray[i]) : 0.0);
                        subAnswer.put("status", i < statusArray.length ? statusArray[i] : "未批改");
                        subAnswers.add(subAnswer);
                    }
                }
                student.put("subAnswers", subAnswers);
            } else {
                // 普通题：直接使用answer、score和status
                student.put("answer", answers);
                student.put("score", scores != null && !scores.isEmpty() ? Double.parseDouble(scores) : 0.0);
                student.put("status", statuses != null && !statuses.isEmpty() ? statuses : "未批改");
            }
            
            // 移除临时字段
            student.remove("answers");
            student.remove("scores");
            student.remove("statuses");
            student.remove("questionIds");
            student.remove("answerIds");
        }
        
        result.put("questionInfo", questionInfo);
        result.put("studentList", studentList);
        
        return result;
    }

    /**
     * 保存单个题目的评分（用于按题目批阅）
     */
    public void saveQuestionScore(java.util.Map<String, Object> params) {
        // 类型安全转换
        Integer questionId = params.get("questionId") instanceof String 
            ? Integer.parseInt((String) params.get("questionId")) 
            : (Integer) params.get("questionId");
        Integer scoreId = params.get("scoreId") instanceof String 
            ? Integer.parseInt((String) params.get("scoreId")) 
            : (Integer) params.get("scoreId");
        String questionTypeVariety = (String) params.get("questionTypeVariety");

        if ("composite".equals(questionTypeVariety)) {
            // 复合题：保存子题分数
            @SuppressWarnings("unchecked")
            List<Object> subScoreObjs = (List<Object>) params.get("subScores");
            @SuppressWarnings("unchecked")
            List<Object> subQuestionIdObjs = (List<Object>) params.get("subQuestionIds");

            if (subScoreObjs != null && subQuestionIdObjs != null) {
                for (int i = 0; i < subQuestionIdObjs.size(); i++) {
                    // 转换子题ID
                    Object idObj = subQuestionIdObjs.get(i);
                    Integer subQuestionId = idObj instanceof String 
                        ? Integer.parseInt((String) idObj) 
                        : (Integer) idObj;
                    
                    // 转换分数
                    Object scoreObj = i < subScoreObjs.size() ? subScoreObjs.get(i) : 0.0;
                    Double score = scoreObj instanceof String 
                        ? Double.parseDouble((String) scoreObj)
                        : (scoreObj instanceof Integer 
                            ? ((Integer) scoreObj).doubleValue() 
                            : (Double) scoreObj);
                    
                    scoreMapper.updateAnswerScore(scoreId, subQuestionId, score);
                }
                
                // 更新父题目的状态为"已批改"（父题目没有分数，只需要更新状态）
                scoreMapper.updateCompositeParentStatus(scoreId, questionId);
            }
        } else {
            // 普通题：保存分数
            Object scoreObj = params.get("score");
            Double score = scoreObj instanceof String 
                ? Double.parseDouble((String) scoreObj)
                : (scoreObj instanceof Integer 
                    ? ((Integer) scoreObj).doubleValue() 
                    : (Double) scoreObj);
            scoreMapper.updateAnswerScore(scoreId, questionId, score);
        }

        // 检查该学生的所有答案是否都已批阅，如果是则更新Score表
        checkAndUpdateScoreStatus(scoreId);
    }

    /**
     * 检查并更新成绩状态
     * 如果所有主观题都已批改，则更新Score表的status为"已批改"，并计算总分
     */
    private void checkAndUpdateScoreStatus(Integer scoreId) {
        // 查询该成绩记录的所有答案
        List<Answer> answers = answerMapper.selectByScoreId(scoreId);
        
        boolean allGraded = true;
        double totalScore = 0.0;

        for (Answer answer : answers) {
            Question question = questionMapper.selectById(answer.getQuestionId());
            
            // 跳过复合题的父题目（它们没有分数）
            if ("composite".equals(question.getQuestionTypeVariety()) && answer.getAnswer().isEmpty()) {
                continue;
            }

            // 跳过客观题（它们已经自动判分）
            if (Arrays.asList("单选", "多选", "判断").contains(question.getQuestionTypeName())) {
                totalScore += (answer.getScore() != null ? answer.getScore() : 0);
                continue;
            }

            // 检查主观题是否已批改（使用status字段）
            if (!"已批改".equals(answer.getStatus())) {
                allGraded = false;
                break;
            }
            
            totalScore += (answer.getScore() != null ? answer.getScore() : 0);
        }

        if (allGraded) {
            // 所有题目都已批改，更新Score表
            Score score = new Score();
            score.setId(scoreId);
            score.setScore((int) Math.round(totalScore));
            score.setStatus("已批改");
            scoreMapper.updateById(score);
        }
    }

    /**
     * 查询异常试卷（作弊记录）
     */
    public PageInfo<Score> selectAbnormal(String testName, String studentName, String testPaperName, 
                                          Integer teacherId, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Score> list = scoreMapper.selectAbnormal(testName, studentName, testPaperName, teacherId);
        
        // 为每条记录生成人脸照片URL（如果文件存在）
        for (Score score : list) {
            String fileName = "exam_draft/" + score.getStudentId() + "_" + score.getTestId() + ".png";
            // 检查文件是否存在
            if (tencentCosService.doesObjectExist(fileName)) {
                String facePhotoUrl = cosBaseUrl + "/" + fileName;
                score.setFacePhotoUrl(facePhotoUrl);
            }
        }
        
        return PageInfo.of(list);
    }

    /**
     * 导出已批改的成绩Excel
     */
    public void exportGradedScores(Integer clazzId, Integer testId, HttpServletResponse response) throws IOException {
        // 查询已批改的成绩列表
        List<Score> scoreList = scoreMapper.selectGradedScores(clazzId, testId);
        
        if (scoreList == null || scoreList.isEmpty()) {
            // 设置响应为JSON格式返回错误信息
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"500\",\"msg\":\"不存在已批改的学生\"}");
            return;
        }

        // 获取班级名称和考试名称用于文件名
        String clazzName = scoreList.get(0).getClazzName();
        String testName = scoreList.get(0).getTest().getName();

        // 创建工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("成绩单");

        // 创建表头样式
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // 创建数据样式
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setAlignment(HorizontalAlignment.CENTER);

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"课程名称", "试卷名称", "考试名称", "班级名称", "授课教师", "学生姓名", "学生用户名", "成绩"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000); // 设置列宽
        }

        // 填充数据
        int rowNum = 1;
        for (Score score : scoreList) {
            Row row = sheet.createRow(rowNum++);
            
            // 课程名称
            Cell cell0 = row.createCell(0);
            cell0.setCellValue(score.getTest() != null && score.getTest().getTestPaper() != null 
                ? score.getTest().getTestPaper().getCourseName() : "");
            cell0.setCellStyle(dataStyle);
            
            // 试卷名称
            Cell cell1 = row.createCell(1);
            cell1.setCellValue(score.getTest() != null && score.getTest().getTestPaper() != null 
                ? score.getTest().getTestPaper().getName() : "");
            cell1.setCellStyle(dataStyle);
            
            // 考试名称
            Cell cell2 = row.createCell(2);
            cell2.setCellValue(score.getTest() != null ? score.getTest().getName() : "");
            cell2.setCellStyle(dataStyle);
            
            // 班级名称
            Cell cell3 = row.createCell(3);
            cell3.setCellValue(score.getClazzName() != null ? score.getClazzName() : "");
            cell3.setCellStyle(dataStyle);
            
            // 授课教师
            Cell cell4 = row.createCell(4);
            cell4.setCellValue(score.getTest() != null && score.getTest().getTestPaper() != null 
                ? score.getTest().getTestPaper().getTeacherName() : "");
            cell4.setCellStyle(dataStyle);
            
            // 学生姓名
            Cell cell5 = row.createCell(5);
            cell5.setCellValue(score.getStudentName() != null ? score.getStudentName() : "");
            cell5.setCellStyle(dataStyle);
            
            // 学生用户名
            Cell cell6 = row.createCell(6);
            cell6.setCellValue(score.getStudentNumber() != null ? score.getStudentNumber() : "");
            cell6.setCellStyle(dataStyle);
            
            // 成绩
            Cell cell7 = row.createCell(7);
            cell7.setCellValue(score.getScore() != null ? score.getScore() : 0);
            cell7.setCellStyle(dataStyle);
        }

        // 设置响应头 - 文件名格式：班级名-考试名-成绩统计
        String filename = clazzName + "-" + testName + "-成绩统计.xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);

        // 写入响应流
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}
