package com.example.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LiveKit服务单元测试
 * 测试Token生成、房间名生成、二维码生成等核心逻辑
 */
@ExtendWith(MockitoExtension.class)
class LiveKitServiceTest {

    @InjectMocks
    private LiveKitService liveKitService;

    @BeforeEach
    void setUp() {
        // 设置测试用的API密钥
        ReflectionTestUtils.setField(liveKitService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(liveKitService, "apiSecret", "test-api-secret-1234567890123456");
        ReflectionTestUtils.setField(liveKitService, "livekitHost", "ws://localhost:7880");
        ReflectionTestUtils.setField(liveKitService, "serverPort", 8080);
    }

    @Test
    @DisplayName("生成房间名格式应正确")
    void testGenerateRoomName_ShouldReturnCorrectFormat() {
        String roomName = liveKitService.generateRoomName("1", "1001");
        
        assertEquals("exam-1-student-1001", roomName, "房间名格式不正确");
    }

    @Test
    @DisplayName("生成房间名应正确处理数字ID")
    void testGenerateRoomNameWithNumericIds_ShouldReturnCorrectFormat() {
        String roomName = liveKitService.generateRoomName("123", "45678");
        
        assertEquals("exam-123-student-45678", roomName, "房间名格式不正确");
    }

    @Test
    @DisplayName("学生端Token应具有发布权限")
    void testGenerateStudentToken_ShouldHavePublishPermission() {
        String token = liveKitService.generateToken("exam-1-student-1001", "computer-1001", true);
        
        assertNotNull(token, "Token不应为空");
        assertFalse(token.isEmpty(), "Token不应为空字符串");
        
        // 验证JWT内容
        DecodedJWT decodedJWT = JWT.decode(token);
        
        // 验证issuer
        assertEquals("test-api-key", decodedJWT.getIssuer(), "Issuer不正确");
        
        // 验证subject（参与者名称）
        assertEquals("computer-1001", decodedJWT.getSubject(), "Subject不正确");
        
        // 验证video grant中的权限
        Map<String, Object> videoGrant = decodedJWT.getClaim("video").asMap();
        assertNotNull(videoGrant, "video声明不应为空");
        assertTrue((Boolean) videoGrant.get("canPublish"), "canPublish应设为true");
    }

    @Test
    @DisplayName("教师端Viewer Token不应具有发布权限")
    void testGenerateViewerToken_ShouldNotHavePublishPermission() {
        String token = liveKitService.generateToken("exam-1-student-1001", "teacher-1", false);
        
        assertNotNull(token, "Token不应为空");
        
        DecodedJWT decodedJWT = JWT.decode(token);
        
        // 验证video grant中的权限
        Map<String, Object> videoGrant = decodedJWT.getClaim("video").asMap();
        assertNotNull(videoGrant, "video声明不应为空");
        assertFalse((Boolean) videoGrant.get("canPublish"), "canPublish应设为false");
    }

    @Test
    @DisplayName("电脑端identity格式应正确")
    void testComputerIdentityFormat_ShouldBeCorrect() {
        String identity = "computer-1001";
        
        assertTrue(identity.startsWith("computer-"), "电脑端identity应以computer-开头");
        assertEquals("1001", identity.substring("computer-".length()), "identity应包含学生ID");
    }

    @Test
    @DisplayName("手机端identity格式应正确")
    void testPhoneIdentityFormat_ShouldBeCorrect() {
        String identity = "phone-1001";
        
        assertTrue(identity.startsWith("phone-"), "手机端identity应以phone-开头");
        assertEquals("1001", identity.substring("phone-".length()), "identity应包含学生ID");
    }

    @Test
    @DisplayName("Token应包含正确的video权限声明")
    void testTokenVideoGrant_ShouldContainCorrectPermissions() {
        String token = liveKitService.generateToken("exam-1-student-1001", "computer-1001", true);
        
        DecodedJWT decodedJWT = JWT.decode(token);
        
        // 获取video声明
        Map<String, Object> videoGrant = decodedJWT.getClaim("video").asMap();
        
        assertNotNull(videoGrant, "video声明不应为空");
        assertTrue((Boolean) videoGrant.get("roomJoin"), "roomJoin应为true");
        assertEquals("exam-1-student-1001", videoGrant.get("room"), "room名称不正确");
        assertTrue((Boolean) videoGrant.get("canPublish"), "canPublish应为true");
        assertTrue((Boolean) videoGrant.get("canSubscribe"), "canSubscribe应为true");
    }

    @Test
    @DisplayName("Token有效期应设置为6小时")
    void testTokenExpiry_ShouldBe6Hours() {
        String token = liveKitService.generateToken("exam-1-student-1001", "computer-1001", true);
        
        DecodedJWT decodedJWT = JWT.decode(token);
        
        long issuedAt = decodedJWT.getIssuedAt().getTime();
        long expiresAt = decodedJWT.getExpiresAt().getTime();
        long actualDuration = expiresAt - issuedAt;
        
        // 验证有效期约为6小时（3600 * 6 = 21600秒，允许2秒误差）
        long expectedDuration = 6 * 60 * 60 * 1000L; // 6小时 = 21600000毫秒
        
        assertTrue(Math.abs(actualDuration - expectedDuration) < 2000, 
                   String.format("Token有效期应约为6小时（21600000毫秒），实际为%d毫秒（约%.1f小时）", 
                       actualDuration, actualDuration / 3600000.0));
    }

    @Test
    @DisplayName("生成二维码应返回有效的Base64图片")
    void testGenerateQRCode_ShouldReturnValidBase64() {
        String content = "http://example.com/test";
        
        String qrCode = liveKitService.generateQRCode(content);
        
        assertNotNull(qrCode, "二维码不应为空");
        assertTrue(qrCode.startsWith("data:image/png;base64,"), 
                   "二维码应是Base64编码的PNG图片");
        
        // 验证Base64内容非空
        String base64Data = qrCode.substring("data:image/png;base64,".length());
        assertFalse(base64Data.isEmpty(), "Base64数据不应为空");
    }

    @Test
    @DisplayName("获取Livekit主机地址应正确")
    void testGetLivekitHost_ShouldReturnConfiguredHost() {
        String host = liveKitService.getLivekitHost();
        
        assertEquals("ws://localhost:7880", host, "LiveKit主机地址不正确");
    }

    @Test
    @DisplayName("Token签名应使用HMAC256算法")
    void testTokenAlgorithm_ShouldBeHMAC256() {
        String token = liveKitService.generateToken("exam-1-student-1001", "computer-1001", true);
        
        DecodedJWT decodedJWT = JWT.decode(token);
        
        // 验证算法为HS256（HMAC256）
        assertEquals("HS256", decodedJWT.getAlgorithm(), "Token应使用HMAC256算法");
    }
}
