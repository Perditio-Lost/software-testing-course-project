package com.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 监考服务单元测试
 * 测试人脸识别验证和异常行为处理逻辑
 */
@ExtendWith(MockitoExtension.class)
class ProctorServiceTest {

    @Mock
    private TencentCosService tencentCosService;

    @InjectMocks
    private MonitorService monitorService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("人脸验证成功时应返回match=true")
    void testFaceVerifySuccess_ShouldReturnMatchTrue() throws Exception {
        // 通过反射调用私有方法
        Method parseMethod = MonitorService.class.getDeclaredMethod(
                "parseSearchResult", String.class, String.class);
        parseMethod.setAccessible(true);

        String successJson = "{\"error_code\":0,\"result\":{\"user_list\":[{\"user_id\":\"1\",\"score\":95.5}]}}";

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parseMethod.invoke(monitorService, successJson, "1");

        assertTrue((Boolean) result.get("match"), "人脸匹配应返回true");
        assertEquals("人脸验证通过", result.get("message"));
        assertEquals("1", result.get("userId"));
        assertEquals(95.5, result.get("score"));
    }

    @Test
    @DisplayName("人脸验证失败（分数低于阈值）时应返回match=false")
    void testFaceVerifyLowScore_ShouldReturnMatchFalse() throws Exception {
        Method parseMethod = MonitorService.class.getDeclaredMethod(
                "parseSearchResult", String.class, String.class);
        parseMethod.setAccessible(true);

        // 相似度65分，低于70分阈值
        String lowScoreJson = "{\"error_code\":0,\"result\":{\"user_list\":[{\"user_id\":\"1\",\"score\":65.0}]}}";

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parseMethod.invoke(monitorService, lowScoreJson, "1");

        assertFalse((Boolean) result.get("match"), "人脸不匹配应返回false");
        assertEquals("人脸不匹配", result.get("message"));
    }

    @Test
    @DisplayName("人脸验证失败（用户不匹配）时应返回match=false")
    void testFaceVerifyWrongUser_ShouldReturnMatchFalse() throws Exception {
        Method parseMethod = MonitorService.class.getDeclaredMethod(
                "parseSearchResult", String.class, String.class);
        parseMethod.setAccessible(true);

        // 用户ID不匹配
        String wrongUserJson = "{\"error_code\":0,\"result\":{\"user_list\":[{\"user_id\":\"2\",\"score\":90.0}]}}";

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parseMethod.invoke(monitorService, wrongUserJson, "1");

        assertFalse((Boolean) result.get("match"), "用户不匹配应返回false");
        assertEquals("人脸不匹配", result.get("message"));
    }

    @Test
    @DisplayName("未检测到人脸时应返回match=false")
    void testFaceVerifyNoFaceDetected_ShouldReturnMatchFalse() throws Exception {
        Method parseMethod = MonitorService.class.getDeclaredMethod(
                "parseSearchResult", String.class, String.class);
        parseMethod.setAccessible(true);

        // 错误码222202表示未检测到人脸
        String noFaceJson = "{\"error_code\":222202,\"error_msg\":\"pic not has face\"}";

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parseMethod.invoke(monitorService, noFaceJson, "1");

        assertFalse((Boolean) result.get("match"), "未检测到人脸应返回false");
        assertEquals("未检测到人脸", result.get("message"));
    }

    @Test
    @DisplayName("API调用失败时应返回match=false")
    void testFaceVerifyApiError_ShouldReturnMatchFalse() throws Exception {
        Method parseMethod = MonitorService.class.getDeclaredMethod(
                "parseSearchResult", String.class, String.class);
        parseMethod.setAccessible(true);

        String errorJson = "{\"error_code\":10001,\"error_msg\":\"unknown error\"}";

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parseMethod.invoke(monitorService, errorJson, "1");

        assertFalse((Boolean) result.get("match"), "API错误应返回false");
        assertEquals("unknown error", result.get("message"));
    }

    @Test
    @DisplayName("没有匹配用户时应返回match=false")
    void testFaceVerifyNoMatch_ShouldReturnMatchFalse() throws Exception {
        Method parseMethod = MonitorService.class.getDeclaredMethod(
                "parseSearchResult", String.class, String.class);
        parseMethod.setAccessible(true);

        String noMatchJson = "{\"error_code\":0,\"result\":{\"user_list\":[]}}";

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parseMethod.invoke(monitorService, noMatchJson, "1");

        assertFalse((Boolean) result.get("match"), "没有匹配用户应返回false");
        assertEquals("未找到匹配的人脸", result.get("message"));
    }

    @Test
    @DisplayName("异常行为上报应正确记录日志")
    void testReportAbnormal_ShouldLogCorrectly() {
        // 这个测试主要验证方法能正常执行，不抛出异常
        // 由于reportAbnormal方法主要是日志记录，可以通过验证方法不抛异常来测试
        assertDoesNotThrow(() -> {
            monitorService.reportAbnormal("exam1", "student1", 3, "人脸连续3次不匹配");
        });
    }

    @Test
    @DisplayName("reportAbnormal应接受null参数")
    void testReportAbnormalWithNullParams_ShouldNotThrow() {
        assertDoesNotThrow(() -> {
            monitorService.reportAbnormal(null, null, null, null);
        });
    }
}
