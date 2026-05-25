package com.example.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.entity.Answer;
import com.example.entity.Question;
import com.example.entity.Score;
import com.example.common.dto.WrongQuestionInfo;
import com.example.mapper.AnswerMapper;
import com.example.mapper.QuestionMapper;
import jakarta.annotation.Resource;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI分析服务 - 使用通义千问qwen3-max模型
 */
@Service
public class AIAnalysisService {

    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String API_KEY = "sk-2e71df9569cf48778a5e0aaaabc991ff";
    private static final String MODEL = "qwen-max";

    @Resource
    private AnswerMapper answerMapper;
    
    @Resource
    private QuestionMapper questionMapper;
    
    @Resource
    private com.example.mapper.TestPaperQuestionMapper testPaperQuestionMapper;
    
    @Resource
    private com.example.mapper.CompositeMapper compositeMapper;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * 分析学生的错题并提供学习建议
     * @param scoreId 成绩ID
     * @return AI分析结果
     */
    public String analyzeWrongQuestions(Integer scoreId, Score scoreData) {
        try {
            // 获取学生的所有答题记录
            List<Answer> answers = answerMapper.selectByScoreId(scoreId);
            if (answers == null || answers.isEmpty()) {
                return "暂无答题数据";
            }

            // 收集错题信息
            List<WrongQuestionInfo> wrongQuestions = new ArrayList<>();
            int totalQuestions = 0;
            int wrongCount = 0;
            int totalScore = 0; // 试卷总分
            int questionNumber = 0; // 题号计数器

            for (Answer answer : answers) {
                Question question = questionMapper.selectById(answer.getQuestionId());
                if (question == null) {
                    continue;
                }

                // 跳过复合题的子题统计（避免重复）
                if ("son".equals(answer.getTestPaperQuestionType())) {
                    continue;
                }

                totalQuestions++;
                questionNumber++; // 累加题号

                // 判断是否为错题（得分为0或未得满分）
                Integer answerScore = answer.getScore() != null ? answer.getScore() : 0;
                
                // 获取题目满分
                Integer fullScore = getQuestionFullScore(answer);
                totalScore += fullScore; // 累加总分
                
                if (answerScore < fullScore) {
                    wrongCount++;
                    WrongQuestionInfo wrongInfo = new WrongQuestionInfo();
                    wrongInfo.setQuestionNumber(questionNumber); // 设置试卷题号
                    wrongInfo.setQuestionType(question.getQuestionTypeName());
                    wrongInfo.setQuestionStem(question.getQuestionStem());
                    wrongInfo.setStudentAnswer(answer.getAnswer());
                    wrongInfo.setCorrectAnswer(question.getAnswer());
                    wrongInfo.setScore(answerScore);
                    wrongInfo.setFullScore(fullScore);
                    
                    // 如果是复合题，收集子题信息（只收集错误的子题）
                    if ("composite".equals(question.getQuestionTypeVariety())) {
                        List<WrongQuestionInfo> subQuestionInfos = new ArrayList<>();
                        List<com.example.entity.Composite> composites = compositeMapper.selectByFatherId(question.getId());
                        if (composites != null && !composites.isEmpty()) {
                            int subQuestionIndex = 1; // 子题编号
                            for (com.example.entity.Composite composite : composites) {
                                Question subQ = questionMapper.selectById(composite.getQuestionSonId());
                                if (subQ != null) {
                                    // 查找子题的答案记录
                                    Answer subAnswer = answers.stream()
                                        .filter(a -> a.getQuestionId().equals(subQ.getId()))
                                        .findFirst()
                                        .orElse(null);
                                    
                                    if (subAnswer != null) {
                                        Integer subScore = subAnswer.getScore() != null ? subAnswer.getScore() : 0;
                                        Integer subFullScore = getQuestionFullScore(subAnswer);
                                        
                                        // 只收集错误的子题（得分不满分）
                                        if (subScore < subFullScore) {
                                            WrongQuestionInfo subInfo = new WrongQuestionInfo();
                                            subInfo.setQuestionNumber(subQuestionIndex); // 设置子题编号
                                            subInfo.setQuestionType(subQ.getQuestionTypeName());
                                            subInfo.setQuestionStem(subQ.getQuestionStem());
                                            subInfo.setStudentAnswer(subAnswer.getAnswer());
                                            subInfo.setCorrectAnswer(subQ.getAnswer());
                                            subInfo.setScore(subScore);
                                            subInfo.setFullScore(subFullScore);
                                            subQuestionInfos.add(subInfo);
                                        }
                                    }
                                    subQuestionIndex++;
                                }
                            }
                        }
                        // 只有当有错误子题时才设置子题列表
                        if (!subQuestionInfos.isEmpty()) {
                            wrongInfo.setSubQuestions(subQuestionInfos);
                        }
                    }
                    
                    wrongQuestions.add(wrongInfo);
                }
            }

            // 如果没有错题
            if (wrongQuestions.isEmpty()) {
                return "恭喜！本次考试全部正确，继续保持！";
            }

            // 构建提示词
            String prompt = buildPrompt(wrongQuestions, totalQuestions, wrongCount, 
                                       scoreData.getScore(), totalScore);

            // 调用AI接口
            return callQwenAPI(prompt);

        } catch (Exception e) {            return "AI分析服务暂时不可用，请稍后再试。错误信息：" + e.getMessage();
        }
    }

    /**
     * 获取题目的满分分数
     */
    private Integer getQuestionFullScore(Answer answer) {
        // 从answer中获取关联的试卷题目信息
        if (answer.getTestPaperQuestionId() != null) {
            com.example.entity.TestPaperQuestion tpq = 
                testPaperQuestionMapper.selectById(answer.getTestPaperQuestionId());
            if (tpq != null && tpq.getScore() != null) {
                return tpq.getScore();
            }
        }
        return 10; // 默认满分10分
    }

    /**
     * 构建AI提示词
     */
    private String buildPrompt(List<WrongQuestionInfo> wrongQuestions, int totalQuestions, 
                               int wrongCount, Integer studentScore, Integer totalScore) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位经验丰富的教育专家和学习顾问。请根据以下学生的考试错题情况，严格按照指定格式进行分析。\n\n");
        
        prompt.append("【考试数据】\n");
        prompt.append("- 总题数：").append(totalQuestions).append("题\n");
        prompt.append("- 错题数：").append(wrongCount).append("题\n");
        prompt.append("- 正确率：").append(String.format("%.1f", (totalQuestions - wrongCount) * 100.0 / totalQuestions)).append("%\n");
        prompt.append("- 学生得分：").append(studentScore).append("分\n");
        prompt.append("- 试卷满分：").append(totalScore).append("分\n\n");

        prompt.append("【错题详细信息】\n");
        for (WrongQuestionInfo wrong : wrongQuestions) {
            prompt.append("第").append(wrong.getQuestionNumber()).append("题：\n");
            prompt.append("  题型：").append(wrong.getQuestionType()).append("\n");
            prompt.append("  题目：").append(wrong.getQuestionStem()).append("\n");
            
            // 如果是复合题，添加子题信息
            if (wrong.getSubQuestions() != null && !wrong.getSubQuestions().isEmpty()) {
                prompt.append("  【这是复合题，包含以下错误的子题】\n");
                for (WrongQuestionInfo subQ : wrong.getSubQuestions()) {
                    prompt.append("    (").append(subQ.getQuestionNumber()).append(") ");
                    prompt.append("题型：").append(subQ.getQuestionType()).append("\n");
                    prompt.append("      题目：").append(subQ.getQuestionStem()).append("\n");
                    prompt.append("      学生答案：").append(subQ.getStudentAnswer() != null && !subQ.getStudentAnswer().isEmpty() 
                                                            ? subQ.getStudentAnswer() : "未作答").append("\n");
                    prompt.append("      正确答案：").append(subQ.getCorrectAnswer()).append("\n");
                    prompt.append("      得分：").append(subQ.getScore()).append("/").append(subQ.getFullScore()).append("分\n");
                }
            } else {
                // 普通题目
                prompt.append("  学生答案：").append(wrong.getStudentAnswer() != null && !wrong.getStudentAnswer().isEmpty() 
                                                        ? wrong.getStudentAnswer() : "未作答").append("\n");
                prompt.append("  正确答案：").append(wrong.getCorrectAnswer()).append("\n");
            }
            prompt.append("  总得分：").append(wrong.getScore()).append("/").append(wrong.getFullScore()).append("分\n\n");
        }

        prompt.append("【输出要求 - 必须严格遵守】\n");
        prompt.append("请完全按照以下三部分结构输出分析报告，不得遗漏任何部分：\n\n");
        
        prompt.append("## 一、试卷得分情况分析\n");
        prompt.append("分析内容包括：\n");
        prompt.append("- 总体得分评价（优秀/良好/中等/较差）\n");
        prompt.append("- 薄弱题型识别\n");
        prompt.append("- 整体掌握程度评估\n\n");
        
        prompt.append("## 二、每道错题单独的分析解答\n");
        prompt.append("对每道错题进行详细分析，使用试卷中的实际题号，格式如下：\n");
        prompt.append("### 第X题（题型）- 请使用上面【错题详细信息】中标注的实际题号\n");
        prompt.append("- **正确解析**：详细讲解正确答案的原理和思路\n");
        prompt.append("- **知识点**：指出涉及的核心知识点\n");
        prompt.append("【复合题特别说明】：如果是复合题，请对每个错误的子题(1)(2)(3)...分别进行详细分析\n\n");
        
        prompt.append("## 三、未来的学习建议\n");
        prompt.append("提供3-5条具体可行的学习改进建议，包括：\n");
        prompt.append("- 需要重点复习的知识点\n");
        prompt.append("- 学习方法建议\n");
        prompt.append("- 练习方向建议\n");
        prompt.append("- 考试注意事项\n\n");
        
        prompt.append("【格式要求】\n");
        prompt.append("- 使用markdown格式\n");
        prompt.append("- 大标题用 ## ，小标题用 ###\n");
        prompt.append("- 列表项紧密排列，不要空行\n");
        prompt.append("- 章节之间用一个空行分隔\n");
        prompt.append("- 内容简洁准确，避免冗余\n");
        prompt.append("- 必须包含上述三个部分，缺一不可");

        return prompt.toString();
    }

    /**
     * 调用通义千问API
     */
    private String callQwenAPI(String prompt) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL);
        
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);

        RequestBody body = RequestBody.create(
                requestBody.toJSONString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = JSON.parseObject(responseBody);
            
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject messageObj = choice.getJSONObject("message");
                String content = messageObj.getString("content");
                
                // 彻底删除所有空行：将所有连续的换行符（包括2个）替换为单个换行符
                content = content.replaceAll("\\n+", "\n");
                // 删除行首行尾的空白
                content = content.trim();
                
                return content;
            }
            
            return "AI分析返回数据格式异常";
        }
    }

    /**
     * AI识别题目照片，提取题目信息
     * @param imageBase64 图片的base64编码
     * @return 识别结果JSON
     */
    public JSONObject recognizeQuestion(String imageBase64) {
        try {
            // 构建识别题目的提示词
            String prompt = buildRecognizePrompt();

            // 调用AI接口（带图片）
            String aiResponse = callQwenAPIWithImage(prompt, imageBase64);

            // 解析AI返回的JSON
            JSONObject result = new JSONObject();
            try {
                // 提取JSON内容（AI可能会在前后加说明文字）
                String jsonStr = extractJSON(aiResponse);
                result = JSON.parseObject(jsonStr);
            } catch (Exception e) {
                // 解析失败，返回原始文本
                result.put("error", "解析失败");
                result.put("rawText", aiResponse);
            }

            return result;
        } catch (Exception e) {            JSONObject errorResult = new JSONObject();
            errorResult.put("error", "AI识别服务暂时不可用：" + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 构建识别题目的提示词
     */
    private String buildRecognizePrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的题目识别专家。请仔细分析图片中的题目内容，识别题型、题干和选项（如果有）。\n\n");
        
        prompt.append("【识别要求】\n");
        prompt.append("1. 准确识别题型（必须从以下题型中选择）：\n");
        prompt.append("   - 单选题 (ID: 1, variety: choice)\n");
        prompt.append("   - 多选题 (ID: 2, variety: choice)\n");
        prompt.append("   - 判断题 (ID: 3, variety: choice)\n");
        prompt.append("   - 填空题 (ID: 4, variety: text)\n");
        prompt.append("   - 名词解析 (ID: 5, variety: text)\n");
        prompt.append("   - 论述题 (ID: 6, variety: text)\n");
        prompt.append("   - 计算题 (ID: 7, variety: text)\n");
        prompt.append("   - 程序题 (ID: 8, variety: text)\n");
        prompt.append("   - 资料题 (ID: 9, variety: composite)\n");
        prompt.append("   - 完形填空 (ID: 10, variety: composite)\n");
        prompt.append("   - 阅读理解 (ID: 11, variety: composite)\n");
        prompt.append("   - 综合题 (ID: 12, variety: composite)\n");
        prompt.append("2. 完整提取题干内容，保持原文格式\n");
        prompt.append("3. 如果是选择题(单选/多选/判断)，提取所有选项及其标识（A、B、C、D等）\n");
        prompt.append("4. 如果能识别正确答案，也请标注出来\n");
        prompt.append("5. 如果是复合题(资料/完形填空/阅读理解/综合)，识别所有子题目\n\n");
        
        prompt.append("【输出格式 - 严格JSON】\n");
        prompt.append("请以纯JSON格式输出，不要添加任何其他文字说明：\n");
        prompt.append("{\n");
        prompt.append("  \"questionTypeId\": 5,  // 题型ID (1-12)\n");
        prompt.append("  \"questionStem\": \"题干内容\",\n");
        prompt.append("  \"choiceList\": [  // 仅选择题(1,2,3)需要\n");
        prompt.append("    {\"identification\": \"A\", \"content\": \"选项内容\", \"flag\": false},\n");
        prompt.append("    {\"identification\": \"B\", \"content\": \"选项内容\", \"flag\": true}\n");
        prompt.append("  ],\n");
        prompt.append("  \"answer\": \"参考答案或解析\",  // 填空题、名词解析、论述题等文本题型使用\n");
        prompt.append("  \"questionList\": [  // 仅复合题(9,10,11,12)需要\n");
        prompt.append("    {\n");
        prompt.append("      \"questionTypeId\": 1,  // 子题的题型ID\n");
        prompt.append("      \"questionStem\": \"子题干\",\n");
        prompt.append("      \"choiceList\": [...],  // 如果子题是选择题\n");
        prompt.append("      \"answer\": \"答案\"  // 如果子题是文本题\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        
        prompt.append("注意：\n");
        prompt.append("- 只输出JSON，不要有任何其他说明文字\n");
        prompt.append("- choiceList中，flag=true表示正确答案\n");
        prompt.append("- 判断题的选项固定为 {identification: \"√\", content: \"正确\"} 和 {identification: \"×\", content: \"错误\"}\n");
        prompt.append("- questionTypeId必须是1-12之间的数字\n");
        prompt.append("- 名词解析题通常要求解释某个概念或术语\n");
        prompt.append("- 论述题通常要求详细阐述观点或分析问题\n");
        prompt.append("- 计算题包含数学计算或推导过程\n");
        prompt.append("- 程序题要求编写或分析代码\n");
        prompt.append("- 资料题、完形填空、阅读理解、综合题会包含一段材料和多个子题\n");
        
        return prompt.toString();
    }

    /**
     * 调用通义千问API（支持图片）
     */
    private String callQwenAPIWithImage(String prompt, String imageBase64) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "qwen-vl-max");  // 使用视觉模型
        
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        
        // 构建包含图片的content
        JSONArray content = new JSONArray();
        
        // 添加文本消息
        JSONObject textContent = new JSONObject();
        textContent.put("type", "text");
        textContent.put("text", prompt);
        content.add(textContent);
        
        // 添加图片
        JSONObject imageContent = new JSONObject();
        imageContent.put("type", "image_url");
        JSONObject imageUrl = new JSONObject();
        imageUrl.put("url", "data:image/jpeg;base64," + imageBase64);
        imageContent.put("image_url", imageUrl);
        content.add(imageContent);
        
        message.put("content", content);
        messages.add(message);
        
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.3);  // 降低温度以提高准确性
        requestBody.put("max_tokens", 3000);

        RequestBody body = RequestBody.create(
                requestBody.toJSONString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = JSON.parseObject(responseBody);
            
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject messageObj = choice.getJSONObject("message");
                return messageObj.getString("content");
            }
            
            throw new IOException("AI返回数据格式异常");
        }
    }

    /**
     * 从AI响应中提取JSON内容
     */
    private String extractJSON(String text) {
        // 尝试找到JSON代码块
        if (text.contains("```json")) {
            int start = text.indexOf("```json") + 7;
            int end = text.indexOf("```", start);
            if (end > start) {
                return text.substring(start, end).trim();
            }
        }
        
        // 尝试找到第一个 { 和最后一个 }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1).trim();
        }
        
        return text.trim();
    }

}
