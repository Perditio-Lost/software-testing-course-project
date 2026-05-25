package com.example.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.time.Instant;
import java.util.*;

/**
 * LiveKit 服务 - 生成房间Token和二维码
 */
@Service
public class LiveKitService {

    private static final Logger log = LoggerFactory.getLogger(LiveKitService.class);

    @Value("${livekit.host:ws://localhost:7880}")
    private String livekitHost;

    @Value("${livekit.apiKey:your_api_key}")
    private String apiKey;

    @Value("${livekit.apiSecret:your_api_secret}")
    private String apiSecret;

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * 生成 LiveKit 房间 Token
     *
     * @param roomName        房间名称
     * @param participantName 参与者名称
     * @param canPublish      是否可以发布视频流
     * @return JWT Token
     */
    public String generateToken(String roomName, String participantName, boolean canPublish) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(apiSecret);

            // 构建 video grant
            Map<String, Object> videoGrant = new HashMap<>();
            videoGrant.put("roomJoin", true);
            videoGrant.put("room", roomName);
            videoGrant.put("canPublish", canPublish);
            videoGrant.put("canSubscribe", true);

            Date now = new Date();
            Date expiry = Date.from(Instant.now().plusSeconds(3600 * 6)); // 6小时有效期

            String token = JWT.create()
                    .withIssuer(apiKey)
                    .withSubject(participantName)
                    .withIssuedAt(now)
                    .withExpiresAt(expiry)
                    .withNotBefore(now)
                    .withClaim("video", videoGrant)
                    .sign(algorithm);

            log.info("Generated LiveKit token for room: {}, participant: {}, canPublish: {}",
                    roomName, participantName, canPublish);
            return token;
        } catch (Exception e) {
            log.error("Failed to generate LiveKit token", e);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    /**
     * 为考试生成监控房间名
     */
    public String generateRoomName(String examId, String studentId) {
        return "exam-" + examId + "-student-" + studentId;
    }

    /**
     * 生成手机端监控页面的URL
     */
    public String generateMobileUrl(String examId, String studentId) {
        try {
            // 获取本机局域网IP
            String localIp = getLocalIpAddress();
            String roomName = generateRoomName(examId, studentId);
            String publisherToken = generateToken(roomName, "phone-" + studentId, true);

            // 构建手机端页面URL
            // 使用HTTP协议和ws://
            // 需要在浏览器配置允许不安全源访问摄像头
            return String.format("http://%s:%d/mobile-camera?token=%s&wsUrl=ws://%s:7880&room=%s",
                    localIp, 3000, publisherToken, localIp, roomName);
        } catch (Exception e) {
            log.error("Failed to generate mobile URL", e);
            throw new RuntimeException("URL generation failed", e);
        }
    }

    /**
     * 生成二维码图片 (Base64编码)
     */
    public String generateQRCode(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 200, 200, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            log.error("Failed to generate QR code", e);
            throw new RuntimeException("QR code generation failed", e);
        }
    }

    /**
     * 获取本机局域网IP地址
     * 优先返回192.168.x.x，排除Docker虚拟网络(172.17.x.x)
     */
    private String getLocalIpAddress() {
        try {
            String fallbackIp = null;
            Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                String ifaceName = iface.getDisplayName().toLowerCase();

                // 跳过回环、未启用、虚拟网卡（Docker、VMware、VirtualBox等）
                if (iface.isLoopback() || !iface.isUp())
                    continue;
                if (ifaceName.contains("docker") || ifaceName.contains("veth") ||
                        ifaceName.contains("vmware") || ifaceName.contains("virtualbox") ||
                        ifaceName.contains("hyper-v"))
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        String ip = addr.getHostAddress();

                        // 最优先: 192.168.x.x (家用/企业局域网)
                        if (ip.startsWith("192.168.")) {
                            log.info("Selected LAN IP: {} from interface: {}", ip, iface.getDisplayName());
                            return ip;
                        }

                        // 次优先: 10.x.x.x (企业网络)
                        if (ip.startsWith("10.") && fallbackIp == null) {
                            fallbackIp = ip;
                        }

                        // 排除 172.17.x.x (Docker默认网段) 和 172.18-31 (Docker自定义网段)
                        // 但保留 172.16.x.x 等可能的真实局域网
                        if (ip.startsWith("172.") && !ip.startsWith("172.17.") &&
                                !ip.startsWith("172.18.") && !ip.startsWith("172.19.") &&
                                fallbackIp == null) {
                            fallbackIp = ip;
                        }
                    }
                }
            }

            if (fallbackIp != null) {
                log.info("Using fallback LAN IP: {}", fallbackIp);
                return fallbackIp;
            }

            log.warn("No suitable LAN IP found, using localhost");
            return "localhost";
        } catch (Exception e) {
            log.warn("Failed to get local IP, using localhost", e);
            return "localhost";
        }
    }

    public String getLivekitHost() {
        return livekitHost;
    }

    /**
     * 获取替换为局域网IP的 LiveKit 地址
     */
    public String getLivekitHostWithLanIp() {
        String localIp = getLocalIpAddress();
        return livekitHost.replace("localhost", localIp);
    }
}
