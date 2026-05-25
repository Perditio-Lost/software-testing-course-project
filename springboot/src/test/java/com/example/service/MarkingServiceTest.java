package com.example.service;

import com.example.common.dto.Account;
import com.example.common.utils.TokenUtils;
import com.example.entity.*;
import com.example.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 主观题阅卷服务单元测试
 * 测试教师人工阅卷逻辑，包括分数保存、边界处理和总分计算
 */
@ExtendWith(MockitoExtension.class)
class MarkingServiceTest {

    @Mock
    private ScoreMapper scoreMapper;

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private AnswerMapper answerMapper;

    @Mock
    private TestPaperQuestionMapper testPaperQuestionMapper;

    @Mock
    private ChoiceMapper choiceMapper;

    @Mock
    private CompositeMapper compositeMapper;

    @Mock
    private TestMapper testMapper;

    @Mock
    private QuestionTypeMapper questionTypeMapper;

    @Mock
    private TencentCosService tencentCosService;

    @InjectMocks
    private ScoreService scoreService; // 使用ScoreService模拟阅卷功能

    private Account mockTeacherAccount;

    @BeforeEach
    void setUp() {
        mockTeacherAccount = new Account();
        mockTeacherAccount.setId(1);
        mockTeacherAccount.setRole("TEACHER");
        mockTeacherAccount.setUsername("testTeacher");
    }

    @Test
    @DisplayName("主观题输入合法分数应正常保存")
    void testSaveValidScore_ShouldSaveSuccessfully() {
        // 创建主观题答案
        Answer answer = createAnswer(1, 1, 1, "学生答案", 0, "待批改");
        
        // 创建Score对象
        Score score = new Score();
        score.setId(1);
        score.setStudentId(1);
        score.setTestId(1);
        score.setScore(0);
        score.setStatus("待批改");
        score.setAnswers(Arrays.asList(answer));

        // 设置mock
        when(testPaperQuestionMapper.selectById(anyInt())).thenReturn(createTestPaperQuestion(1, 10, "common"));

        // 执行测试
        scoreService.updateById(score);

        // 验证答案被更新，状态变为已批改
        verify(answerMapper, times(1)).updateById(argThat(a -> 
            "已批改".equals(a.getStatus())
        ));

        // 验证score被更新，状态变为已批改
        verify(scoreMapper, times(1)).updateById(argThat(s -> 
            "已批改".equals(s.getStatus())
        ));
    }

    @Test
    @DisplayName("计算总分时应排除复合题子题分数")
    void testCalculateTotalScore_ShouldExcludeSubQuestions() {
        // 创建普通题目答案（计入总分）
        Answer commonAnswer = createAnswer(1, 1, 1, "答案1", 10, "已批改");
        
        // 创建复合题子题答案（不计入总分）
        Answer subAnswer = createAnswer(2, 1, 2, "答案2", 5, "已批改");

        // 设置mock - 普通题
        TestPaperQuestion commonTpq = createTestPaperQuestion(1, 10, "common");
        when(testPaperQuestionMapper.selectById(1)).thenReturn(commonTpq);

        // 设置mock - 子题
        TestPaperQuestion subTpq = createTestPaperQuestion(2, 5, "son");
        when(testPaperQuestionMapper.selectById(2)).thenReturn(subTpq);

        // 创建Score对象
        Score score = new Score();
        score.setId(1);
        score.setStudentId(1);
        score.setTestId(1);
        score.setScore(0);
        score.setStatus("待批改");
        score.setAnswers(Arrays.asList(commonAnswer, subAnswer));

        // 执行测试
        scoreService.updateById(score);

        // 验证总分应为10（只计入普通题），不应包含子题的5分
        verify(scoreMapper, times(1)).updateById(argThat(s -> 
            s.getScore().equals(10)
        ));
    }

    @Test
    @DisplayName("总分计算应正确累加所有非子题分数")
    void testCalculateTotalScore_ShouldSumAllNonSubQuestions() {
        Answer answer1 = createAnswer(1, 1, 1, "答案1", 10, "已批改");
        Answer answer2 = createAnswer(2, 1, 2, "答案2", 20, "已批改");
        Answer answer3 = createAnswer(3, 1, 3, "答案3", 15, "已批改");

        when(testPaperQuestionMapper.selectById(1)).thenReturn(createTestPaperQuestion(1, 10, "common"));
        when(testPaperQuestionMapper.selectById(2)).thenReturn(createTestPaperQuestion(2, 20, "common"));
        when(testPaperQuestionMapper.selectById(3)).thenReturn(createTestPaperQuestion(3, 15, "common"));

        Score score = new Score();
        score.setId(1);
        score.setAnswers(Arrays.asList(answer1, answer2, answer3));

        scoreService.updateById(score);

        // 总分应为 10 + 20 + 15 = 45
        verify(scoreMapper, times(1)).updateById(argThat(s -> 
            s.getScore().equals(45) && "已批改".equals(s.getStatus())
        ));
    }

    @Test
    @DisplayName("新增加的答案记录应正确插入")
    void testNewAnswer_ShouldBeInserted() {
        // 创建没有ID的答案（表示新增）
        Answer newAnswer = createAnswer(null, 1, 1, "新答案", 8, "待批改");

        Score score = new Score();
        score.setId(1);
        score.setAnswers(Arrays.asList(newAnswer));

        when(testPaperQuestionMapper.selectById(anyInt())).thenReturn(createTestPaperQuestion(1, 10, "common"));

        scoreService.updateById(score);

        // 验证新增答案被插入
        verify(answerMapper, times(1)).insert(any(Answer.class));
        verify(answerMapper, never()).updateById(any(Answer.class));
    }

    @Test
    @DisplayName("已存在的答案记录应正确更新")
    void testExistingAnswer_ShouldBeUpdated() {
        // 创建有ID的答案（表示已存在）
        Answer existingAnswer = createAnswer(1, 1, 1, "现有答案", 8, "待批改");

        Score score = new Score();
        score.setId(1);
        score.setAnswers(Arrays.asList(existingAnswer));

        when(testPaperQuestionMapper.selectById(anyInt())).thenReturn(createTestPaperQuestion(1, 10, "common"));

        scoreService.updateById(score);

        // 验证现有答案被更新
        verify(answerMapper, times(1)).updateById(any(Answer.class));
        verify(answerMapper, never()).insert(any(Answer.class));
    }

    @Test
    @DisplayName("没有testPaperQuestionId的答案应计入总分")
    void testAnswerWithoutTPQ_ShouldBeIncludedInTotal() {
        // 创建没有testPaperQuestionId的答案
        Answer answer = new Answer();
        answer.setId(1);
        answer.setScoreId(1);
        answer.setTestPaperQuestionId(null); // 没有TPQ ID
        answer.setAnswer("答案");
        answer.setScore(20);
        answer.setStatus("已批改");

        Score score = new Score();
        score.setId(1);
        score.setAnswers(Arrays.asList(answer));

        // 由于没有TPQ ID，直接计入总分
        scoreService.updateById(score);

        verify(scoreMapper, times(1)).updateById(argThat(s -> 
            s.getScore().equals(20)
        ));
    }

    // ========== 辅助方法 ==========

    private Answer createAnswer(Integer id, Integer scoreId, Integer tpqId, String answer, Integer score, String status) {
        Answer ans = new Answer();
        ans.setId(id);
        ans.setScoreId(scoreId);
        ans.setTestPaperQuestionId(tpqId);
        ans.setAnswer(answer);
        ans.setScore(score);
        ans.setStatus(status);
        return ans;
    }

    private TestPaperQuestion createTestPaperQuestion(Integer id, Integer score, String type) {
        TestPaperQuestion tpq = new TestPaperQuestion();
        tpq.setId(id);
        tpq.setScore(score);
        tpq.setType(type);
        return tpq;
    }
}
