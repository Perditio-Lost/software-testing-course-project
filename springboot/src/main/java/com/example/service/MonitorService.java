package com.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 考试监控服务 - 百度人脸识别
 */
@Service
public class MonitorService {

    private static final Logger log = LoggerFactory.getLogger(MonitorService.class);

    @Resource
    private TencentCosService tencentCosService;

    // 百度AI配置 - 请替换为你的实际值
    @Value("${baidu.ai.apiKey:9tSjheJj5JnaHFwZdeCZ6oBi}")
    private String apiKey;

    @Value("${baidu.ai.secretKey:5JpNSW6Ek6D68l37VsdmONI3VgSSXldz}")
    private String secretKey;

    @Value("${baidu.ai.groupId:123}")
    private String groupId;

    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String SEARCH_URL = "https://aip.baidubce.com/rest/2.0/face/v3/search";
    private static final String ADD_URL = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/user/add";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 缓存 access token
    private String cachedToken = null;
    private long tokenExpireTime = 0;

    /**
     * 人脸验证
     */
    public Map<String, Object> verifyFace(String examId, String studentId, String imageData) throws Exception {
        Map<String, Object> result = new HashMap<>();

        // 1. 获取 Access Token
        String accessToken = getAccessToken();
        if (accessToken == null) {
            result.put("match", false);
            result.put("message", "获取AI凭证失败");
            return result;
        }

        // 3. 调用百度人脸搜索API
        String base64Image = imageData.contains(",") ? imageData.split(",")[1] : imageData;
        String searchResult = searchFaceInGroup(accessToken, base64Image);

        if (searchResult == null) {
            result.put("match", false);
            result.put("message", "人脸搜索请求失败");
            return result;
        }

        // 4. 解析结果
        result = parseSearchResult(searchResult, studentId);
        
        // 5. 每次检测都上传人脸照片到云端（不管检测结果如何）
        try {
            // 将base64图片转换为字节数组
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            
            // 构建文件名：exam_draft/studentId_examId.png
            String fileName = "exam_draft/" + studentId + "_" + examId + ".png";
            
            // 上传到云端（覆盖前一张）
            String fileUrl = tencentCosService.uploadPngImage(imageBytes, fileName);
            
            boolean match = result.get("match") != null && (Boolean) result.get("match");
            log.info("人脸照片上传成功 - 学生ID: {}, 考试ID: {}, 验证结果: {}, URL: {}", 
                    studentId, examId, match ? "成功" : "失败", fileUrl);
            result.put("facePhotoUrl", fileUrl);
        } catch (Exception e) {
            log.error("人脸照片上传失败", e);
            // 上传失败不影响人脸验证结果
        }
        
        return result;
    }

    /**
     * 人脸注册（添加人脸到库）
     */
    public void registerFace(String studentId, String imageData) throws Exception {
        // 1. 获取 Access Token
        String accessToken = getAccessToken();
        if (accessToken == null) {
            throw new Exception("获取AI凭证失败");
        }

        // 2. 准备请求数据
        String base64Image = imageData.contains(",") ? imageData.split(",")[1] : imageData;

        Map<String, Object> body = new HashMap<>();
        body.put("image", base64Image);
        body.put("image_type", "BASE64");
        body.put("group_id", groupId);
        body.put("user_id", studentId);
        body.put("action_type", "REPLACE"); // 如果存在则替换

        String jsonBody = objectMapper.writeValueAsString(body);
        String url = ADD_URL + "?access_token=" + accessToken;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // 3. 发送请求
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode jsonNode = objectMapper.readTree(response.body());

        int errorCode = jsonNode.has("error_code") ? jsonNode.get("error_code").asInt() : -1;
        if (errorCode != 0) {
            String errorMsg = jsonNode.has("error_msg") ? jsonNode.get("error_msg").asText() : "未知错误";
            log.error("人脸注册API错误: {} - {}", errorCode, errorMsg);
            throw new Exception("百度云人脸注册失败: " + errorMsg);
        }

        log.info("学生 {} 人脸注册成功", studentId);
    }

    /**
     * 报告考试异常
     */
    public void reportAbnormal(String examId, String studentId, Integer warningCount, String reason) {
        log.warn("考试异常报告 - 考试ID: {}, 学生ID: {}, 警告次数: {}, 原因: {}",
                examId, studentId, warningCount, reason);

        // 这里可以扩展：保存到数据库、发送通知给监考老师等
        // 例如：abnormalRecordMapper.insert(new AbnormalRecord(examId, studentId,
        // warningCount, reason));
    }

    /**
     * 获取百度 Access Token
     */
    private String getAccessToken() {
        // 检查缓存
        if (cachedToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return cachedToken;
        }

        try {
            String url = TOKEN_URL + "?grant_type=client_credentials&client_id=" + apiKey + "&client_secret="
                    + secretKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode jsonNode = objectMapper.readTree(response.body());

            if (jsonNode.has("access_token")) {
                cachedToken = jsonNode.get("access_token").asText();
                // Token 有效期约30天，这里设置29天
                tokenExpireTime = System.currentTimeMillis() + 29L * 24 * 60 * 60 * 1000;
                return cachedToken;
            } else {
                log.error("获取 access_token 失败: {}", response.body());
                return null;
            }
        } catch (Exception e) {
            log.error("获取 access_token 异常", e);
            return null;
        }
    }

    /**
     * 调用百度人脸搜索API
     */
    private String searchFaceInGroup(String accessToken, String base64Image) {
        try {
            String url = SEARCH_URL + "?access_token=" + accessToken;

            Map<String, Object> body = new HashMap<>();
            body.put("image", base64Image);
            body.put("image_type", "BASE64");
            body.put("group_id_list", groupId);
            body.put("match_threshold", 70); // 匹配阈值

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();

        } catch (Exception e) {
            log.error("人脸搜索请求异常", e);
            return null;
        }
    }

    /**
     * 解析人脸搜索结果
     */
    private Map<String, Object> parseSearchResult(String searchResult, String expectedStudentId) {
        Map<String, Object> result = new HashMap<>();

        try {
            JsonNode jsonNode = objectMapper.readTree(searchResult);
            int errorCode = jsonNode.has("error_code") ? jsonNode.get("error_code").asInt() : -1;

            if (errorCode != 0) {
                String errorMsg = jsonNode.has("error_msg") ? jsonNode.get("error_msg").asText() : "未知错误";
                log.warn("人脸识别API错误: {} - {}", errorCode, errorMsg);

                // 如果是没有检测到人脸，返回不匹配
                if (errorCode == 222202 || errorCode == 222203) {
                    result.put("match", false);
                    result.put("message", "未检测到人脸");
                    return result;
                }

                result.put("match", false);
                result.put("message", errorMsg);
                return result;
            }

            JsonNode resultNode = jsonNode.get("result");
            if (resultNode != null && resultNode.has("user_list")) {
                JsonNode userList = resultNode.get("user_list");
                if (userList.isArray() && userList.size() > 0) {
                    JsonNode firstMatch = userList.get(0);
                    String userId = firstMatch.get("user_id").asText();
                    double score = firstMatch.get("score").asDouble();

                    log.info("人脸匹配结果 - 用户ID: {}, 相似度: {}", userId, score);

                    // 检查是否是期望的学生
                    boolean isMatch = userId.equals(expectedStudentId) && score >= 70;

                    result.put("match", isMatch);
                    result.put("userId", userId);
                    result.put("score", score);
                    result.put("message", isMatch ? "人脸验证通过" : "人脸不匹配");
                    return result;
                }
            }

            result.put("match", false);
            result.put("message", "未找到匹配的人脸");
            return result;

        } catch (Exception e) {
            log.error("解析人脸搜索结果异常", e);
            result.put("match", false);
            result.put("message", "解析结果失败");
            return result;
        }
    }
}
