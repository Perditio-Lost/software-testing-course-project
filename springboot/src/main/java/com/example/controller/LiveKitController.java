package com.example.controller;

import com.example.common.Result;
import com.example.entity.StudentClazz;
import com.example.entity.TestClazz;
import com.example.mapper.StudentClazzMapper;
import com.example.mapper.TestClazzMapper;
import com.example.service.LiveKitService;
import com.example.service.ScoreService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * LiveKit 接口 - 双机位监考
 */
@RestController
@RequestMapping("/livekit")
public class LiveKitController {

    @Resource
    private LiveKitService liveKitService;

    @Resource
    private ScoreService scoreService;

    @Resource
    private TestClazzMapper testClazzMapper;

    @Resource
    private StudentClazzMapper studentClazzMapper;

    /**
     * 获取监控房间信息和二维码
     * 
     * @param examId    考试ID
     * @param studentId 学生ID
     * @return 包含 viewerToken, qrCode, wsUrl, roomName
     */
    @GetMapping("/roomInfo")
    public Result getRoomInfo(@RequestParam String examId, @RequestParam String studentId) {
        try {
            String roomName = liveKitService.generateRoomName(examId, studentId);

            // 生成观看者Token (电脑端，不能发布，只能订阅)
            String viewerToken = liveKitService.generateToken(roomName, "viewer-" + studentId, false);

            // 生成手机端URL和二维码
            String mobileUrl = liveKitService.generateMobileUrl(examId, studentId);
            String qrCode = liveKitService.generateQRCode(mobileUrl);

            // 获取替换为局域网IP的 LiveKit 地址
            String wsUrl = liveKitService.getLivekitHostWithLanIp();

            Map<String, Object> result = new HashMap<>();
            result.put("roomName", roomName);
            result.put("viewerToken", viewerToken);
            result.put("qrCode", qrCode);
            result.put("mobileUrl", mobileUrl);
            result.put("wsUrl", wsUrl);

            return Result.success(result);
        } catch (Exception e) {            return Result.error("获取房间信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取发布者Token (手机端调用)
     */
    @GetMapping("/publisherToken")
    public Result getPublisherToken(@RequestParam String examId, @RequestParam String studentId) {
        try {
            String roomName = liveKitService.generateRoomName(examId, studentId);
            String token = liveKitService.generateToken(roomName, "phone-" + studentId, true);
            String wsUrl = liveKitService.getLivekitHostWithLanIp();

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("wsUrl", wsUrl);
            result.put("roomName", roomName);

            return Result.success(result);
        } catch (Exception e) {            return Result.error("获取Token失败: " + e.getMessage());
        }
    }

    /**
     * 获取电脑摄像头发布者Token
     * 用于电脑端摄像头推流到 LiveKit，供教师端监考
     */
    @GetMapping("/computerToken")
    public Result getComputerToken(@RequestParam String examId, @RequestParam String studentId) {
        try {
            String roomName = liveKitService.generateRoomName(examId, studentId);
            // 使用 computer- 前缀区分电脑端和手机端
            String token = liveKitService.generateToken(roomName, "computer-" + studentId, true);
            String wsUrl = liveKitService.getLivekitHostWithLanIp();

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("wsUrl", wsUrl);
            result.put("roomName", roomName);

            return Result.success(result);
        } catch (Exception e) {            return Result.error("获取电脑端Token失败: " + e.getMessage());
        }
    }

    /**
     * 获取参加考试的学生列表
     * 通过 test_clazz -> student_clazz 关联获取该考试班级的所有学生
     * 
     * @param examId 考试ID (testId)
     * @return 参加考试的学生列表
     */
    @GetMapping("/examStudents")
    public Result getExamStudents(@RequestParam Integer examId) {
        try {
            // 1. 获取该考试关联的所有班级
            List<TestClazz> testClazzList = testClazzMapper.selectByTestId(examId);

            // 使用 Set 去重（一个学生可能在多个班级）
            Set<Integer> studentIdSet = new HashSet<>();
            List<Map<String, Object>> students = new ArrayList<>();

            // 2. 遍历每个班级，获取班级中的学生
            for (TestClazz testClazz : testClazzList) {
                List<StudentClazz> studentClazzList = studentClazzMapper.selectByClazzId(testClazz.getClazzId());

                for (StudentClazz sc : studentClazzList) {
                    // 去重
                    if (!studentIdSet.contains(sc.getStudentId())) {
                        studentIdSet.add(sc.getStudentId());

                        Map<String, Object> student = new HashMap<>();
                        student.put("id", sc.getStudentId());
                        student.put("name", sc.getStudentName());
                        student.put("studentNumber", sc.getStudentNumber());
                        student.put("clazzName", sc.getClazzName());
                        students.add(student);
                    }
                }
            }

            return Result.success(students);
        } catch (Exception e) {            return Result.error("获取学生列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取教师监考Token
     * 为教师生成订阅多个学生房间的viewer tokens
     * 通过 test_clazz -> student_clazz 关联获取该考试班级的所有学生
     * 
     * @param examId 考试ID
     * @return 每个学生房间的viewer token列表
     */
    @GetMapping("/teacherTokens")
    public Result getTeacherTokens(@RequestParam Integer examId) {
        try {
            // 1. 获取该考试关联的所有班级
            List<TestClazz> testClazzList = testClazzMapper.selectByTestId(examId);

            String wsUrl = liveKitService.getLivekitHostWithLanIp();
            Set<Integer> studentIdSet = new HashSet<>();
            List<Map<String, Object>> studentTokens = new ArrayList<>();

            // 2. 遍历每个班级，获取班级中的学生并生成token
            for (TestClazz testClazz : testClazzList) {
                List<StudentClazz> studentClazzList = studentClazzMapper.selectByClazzId(testClazz.getClazzId());

                for (StudentClazz sc : studentClazzList) {
                    // 去重
                    if (!studentIdSet.contains(sc.getStudentId())) {
                        studentIdSet.add(sc.getStudentId());

                        String studentId = String.valueOf(sc.getStudentId());
                        String roomName = liveKitService.generateRoomName(String.valueOf(examId), studentId);
                        // 教师作为观看者，只能订阅，不能发布
                        String viewerToken = liveKitService.generateToken(roomName, "teacher-viewer", false);

                        Map<String, Object> tokenInfo = new HashMap<>();
                        tokenInfo.put("studentId", sc.getStudentId());
                        tokenInfo.put("studentName", sc.getStudentName());
                        tokenInfo.put("studentNumber", sc.getStudentNumber());
                        tokenInfo.put("clazzName", sc.getClazzName());
                        tokenInfo.put("roomName", roomName);
                        tokenInfo.put("token", viewerToken);
                        studentTokens.add(tokenInfo);
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("wsUrl", wsUrl);
            result.put("students", studentTokens);

            return Result.success(result);
        } catch (Exception e) {            return Result.error("获取教师Token失败: " + e.getMessage());
        }
    }
}
