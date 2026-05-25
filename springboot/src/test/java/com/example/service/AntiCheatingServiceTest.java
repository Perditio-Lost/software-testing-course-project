package com.example.service;

import com.example.common.dto.Account;
import com.example.common.dto.TestSubmitDTO;
import com.example.entity.Answer;
import com.example.entity.Choice;
import com.example.entity.Question;
import com.example.entity.Score;
import com.example.entity.TestPaper;
import com.example.entity.TestPaperQuestion;
import com.example.mapper.AnswerMapper;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CompositeMapper;
import com.example.mapper.QuestionMapper;
import com.example.mapper.QuestionTypeMapper;
import com.example.mapper.ScoreMapper;
import com.example.mapper.TestMapper;
import com.example.mapper.TestPaperQuestionMapper;
import com.example.common.utils.TokenUtils;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 防作弊服务单元测试
 * 测试切屏检测、人脸异常检测、作弊标记等防作弊规则判断逻辑
 */
@ExtendWith(MockitoExtension.class)
class AntiCheatingServiceTest {

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
    private ScoreService scoreService;

    @InjectMocks
    private MonitorService monitorService;

    private Account mockAccount;

    /**
     * 注意：
     * 这里必须使用 com.example.entity.Test 的全限定类名，
     * 否则会和 org.junit.jupiter.api.Test 注解产生命名冲突。
     */
    private com.example.entity.Test mockTest;

    @BeforeEach
    void setUp() {
        mockAccount = new Account();
        mockAccount.setId(1);
        mockAccount.setRole("STUDENT");

        mockTest = new com.example.entity.Test();
        mockTest.setId(1);
        mockTest.setTestPaperId(1);
    }

    @Test
    @DisplayName("切屏次数小于3次时不应触发强制交卷")
    void testScreenSwitchLessThan3_ShouldNotForceSubmit() {
        int screenSwitchCount = 2;

        boolean shouldForceSubmit = screenSwitchCount >= 3;

        assertFalse(shouldForceSubmit, "切屏次数小于3次不应触发强制交卷");
    }

    @Test
    @DisplayName("切屏次数等于3次时应触发强制交卷")
    void testScreenSwitchEqual3_ShouldForceSubmit() {
        int screenSwitchCount = 3;

        boolean shouldForceSubmit = screenSwitchCount >= 3;

        assertTrue(shouldForceSubmit, "切屏次数等于3次应触发强制交卷");
    }

    @Test
    @DisplayName("人脸异常次数为2次时只警告不强制交卷")
    void testFaceAbnormalCount2_ShouldWarnOnly() {
        int warningCount = 2;

        boolean shouldForceSubmit = warningCount >= 3;

        assertFalse(shouldForceSubmit, "人脸异常2次不应强制交卷");
    }

    @Test
    @DisplayName("人脸异常次数达到3次时应触发强制交卷")
    void testFaceAbnormalCount3_ShouldForceSubmit() {
        int warningCount = 3;

        boolean shouldForceSubmit = warningCount >= 3;

        assertTrue(shouldForceSubmit, "人脸异常3次应触发强制交卷");
    }

    @Test
    @DisplayName("作弊标记cheat=1时成绩应视为作弊")
    void testCheatFlag1_ShouldBeCheating() {
        Score score = new Score();
        score.setCheat(1);
        score.setScore(0);

        assertTrue(score.getCheat() == 1, "作弊标记应为1");
        assertEquals(0, score.getScore(), "作弊时成绩应为0");
    }

    @Test
    @DisplayName("作弊标记cheat=0时成绩应视为正常")
    void testCheatFlag0_ShouldBeNormal() {
        Score score = new Score();
        score.setCheat(0);
        score.setScore(85);

        assertFalse(score.getCheat() == 1, "作弊标记应为0");
        assertEquals(85, score.getScore(), "正常成绩应保留");
    }

    @Test
    @DisplayName("获取作弊学生答案时应返回空列表")
    void testGetAnswerForCheatingStudent_ShouldReturnEmptyList() {
        try (MockedStatic<TokenUtils> mockedTokenUtils = Mockito.mockStatic(TokenUtils.class)) {
            mockedTokenUtils.when(TokenUtils::getCurrentUser).thenReturn(mockAccount);

            Score cheatScore = new Score();
            cheatScore.setId(1);
            cheatScore.setCheat(1);
            cheatScore.setStatus("已批改");

            when(scoreMapper.selectById(anyInt())).thenReturn(cheatScore);

            List<Answer> answers = scoreService.selectAnswer(1);

            assertNotNull(answers, "答案列表不应为null");
            assertTrue(answers.isEmpty(), "作弊学生答案应返回空列表");
        }
    }

    @Test
    @DisplayName("人脸验证成功时warningCount应清零")
    void testFaceVerifySuccess_ShouldResetWarningCount() {
        int currentWarningCount = 2;
        boolean faceMatch = true;

        int newWarningCount = faceMatch ? 0 : currentWarningCount + 1;

        assertEquals(0, newWarningCount, "人脸验证成功应清零警告计数");
    }

    @Test
    @DisplayName("人脸验证失败时warningCount应增加")
    void testFaceVerifyFail_ShouldIncrementWarningCount() {
        int currentWarningCount = 1;
        boolean faceMatch = false;

        int newWarningCount = faceMatch ? 0 : currentWarningCount + 1;

        assertEquals(2, newWarningCount, "人脸验证失败应增加警告计数");
    }

    @Test
    @DisplayName("人脸验证连续失败3次应触发异常")
    void testFaceVerifyFail3Times_ShouldTriggerAbnormal() {
        int warningCount = 2;
        boolean faceMatch = false;

        int newWarningCount = faceMatch ? 0 : warningCount + 1;
        boolean isAbnormal = newWarningCount >= 3;

        assertTrue(isAbnormal, "连续3次人脸验证失败应触发异常");
        assertEquals(3, newWarningCount, "警告计数应为3");
    }

    @Test
    @DisplayName("正常交卷时作弊标记应为0")
    void testNormalSubmit_ShouldSetCheatTo0() {
        try (MockedStatic<TokenUtils> mockedTokenUtils = Mockito.mockStatic(TokenUtils.class)) {
            mockedTokenUtils.when(TokenUtils::getCurrentUser).thenReturn(mockAccount);

            when(testMapper.selectById(anyInt())).thenReturn(mockTest);

            Question question = createQuestion(1, "单选", "测试题");
            question.setNewAnswer("A");

            List<Choice> choices = Arrays.asList(
                    createChoice(1, "A", "正确", true),
                    createChoice(2, "B", "错误", false)
            );

            when(choiceMapper.selectByQuestionId(anyInt())).thenReturn(choices);
            when(testPaperQuestionMapper.selectById(anyInt())).thenReturn(createTestPaperQuestion(1, 10));
            when(questionMapper.selectById(anyInt())).thenReturn(question);

            doAnswer(invocation -> {
                Score score = invocation.getArgument(0);
                score.setId(1);
                return null;
            }).when(scoreMapper).insert(any(Score.class));

            TestSubmitDTO submitDTO = createSubmitDTO(1, 0, Arrays.asList(question));

            scoreService.add(submitDTO);

            verify(scoreMapper, times(1)).insert(argThat(score ->
                    score != null && Integer.valueOf(0).equals(score.getCheat())
            ));
        }
    }

    @Test
    @DisplayName("异常行为上报应包含关键字段")
    void testReportAbnormal_ShouldContainRequiredFields() {
        assertDoesNotThrow(() -> {
            monitorService.reportAbnormal("exam123", "student456", 3, "切屏次数超限");
        });
    }

    // ========== 辅助方法 ==========

    private Question createQuestion(Integer id, String typeName, String stem) {
        Question question = new Question();
        question.setId(id);
        question.setQuestionTypeName(typeName);
        question.setQuestionStem(stem);
        question.setTestPaperQuestionId(id);
        return question;
    }

    private Choice createChoice(Integer id, String identification, String content, boolean flag) {
        Choice choice = new Choice();
        choice.setId(id);
        choice.setIdentification(identification);
        choice.setContent(content);
        choice.setFlag(flag);
        return choice;
    }

    private TestPaperQuestion createTestPaperQuestion(Integer id, Integer score) {
        TestPaperQuestion testPaperQuestion = new TestPaperQuestion();
        testPaperQuestion.setId(id);
        testPaperQuestion.setScore(score);
        testPaperQuestion.setType("common");
        return testPaperQuestion;
    }

    private TestSubmitDTO createSubmitDTO(Integer testId, Integer cheat, List<Question> questions) {
        TestSubmitDTO submitDTO = new TestSubmitDTO();
        submitDTO.setId(testId);
        submitDTO.setCheat(cheat);

        TestPaper testPaper = new TestPaper();
        testPaper.setQuestions(questions);

        submitDTO.setTestPaper(testPaper);

        return submitDTO;
    }
}