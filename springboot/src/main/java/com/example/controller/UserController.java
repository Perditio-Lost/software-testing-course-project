package com.example.controller;

import com.example.common.Result;
import com.example.common.enums.RoleEnum;
import com.example.entity.Student;
import com.example.service.StudentService;
import com.example.common.utils.SendMailUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 用户注册控制器
 */
@CrossOrigin
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private StudentService studentService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SendMailUtil sendMailUtil;

    /**
     * 发送邮箱验证码
     * 
     * @param email 邮箱地址
     * @return 操作结果
     */
    @PostMapping("/sendCode")
    public Result getCode(@RequestParam String email) {
        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱地址不能为空");
        }

        // 首先判断邮箱是否已经注册
        Student existingStudent = studentService.selectByEmail(email);
        if (existingStudent != null) {
            return Result.error("此邮箱已经注册过了");
        }

        // 定义返回消息
        String message;
        try {
            // 判断邮箱格式是否正确
            if (sendMailUtil.isEmail(email)) {
                ValueOperations<String, Object> operations = redisTemplate.opsForValue();
                // 如果redis的验证码还存在则提示
                if (operations.get(email) != null) {
                    message = "验证码已发送,请60秒后重试";
                } else {
                    // 生成6位随机码
                    String code = sendMailUtil.createCode();
                    operations.set(Objects.requireNonNull(email, "email cannot be null"),
                            Objects.requireNonNull(code, "code cannot be null"));
                    // 设置验证码过期时间为60秒
                    redisTemplate.expire(Objects.requireNonNull(email, "email cannot be null"), 60, TimeUnit.SECONDS);

                    // 发送验证码邮件
                    message = sendMailUtil.sendEmail(Objects.requireNonNull(email, "email cannot be null"), code);
                }
            } else {
                message = "邮箱格式不正确";
            }
        } catch (Exception e) {            message = "出现未知错误";
        }

        return Result.success(message);
    }

    /**
     * 邮箱注册
     * 
     * @param student          学生信息
     * @param verificationCode 验证码
     * @return 注册结果
     */
    @PostMapping("/emailRegister")
    @Transactional(rollbackFor = Exception.class)
    public Result emailRegister(@RequestBody Student student,
            @RequestParam String verificationCode) {

        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        // 获取redis中的验证码
        String studentEmail = student.getEmail();
        Object codeObj = studentEmail != null ? operations.get(Objects.requireNonNull(studentEmail)) : null;
        String code = codeObj != null ? (String) codeObj : null;

        // 再次检查邮箱是否已经注册
        Student existingStudent = studentService.selectByEmail(student.getEmail());
        if (existingStudent != null) {
            return Result.error("邮箱已经注册");
        }

        // 检查用户名是否已存在
        Student existingUsername = studentService.selectByUsername(student.getUsername());
        if (existingUsername != null) {
            return Result.error("用户名已存在");
        }

        // 如果验证码正确
        if (code != null && code.equals(verificationCode)) {
            // 设置默认值
            student.setRole(RoleEnum.STUDENT.name());
            student.setStatus("待审核");

            try {
                studentService.add(student);
                // 删除验证码，保证验证码只能使用一次
                String emailToDelete = student.getEmail();
                if (emailToDelete != null) {
                    redisTemplate.delete(Objects.requireNonNull(emailToDelete));
                }
                return Result.success("注册成功");
            } catch (Exception e) {                return Result.error("注册失败，请重试");
            }
        } else {
            return Result.error("验证码无效，请重新获取");
        }
    }

    /**
     * 发送重置密码验证码
     * 
     * @param email 邮箱地址
     * @return 操作结果
     */
    @PostMapping("/sendResetCode")
    public Result sendResetCode(@RequestParam String email) {
        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱地址不能为空");
        }

        // 检查邮箱是否已注册
        Student existingStudent = studentService.selectByEmail(email);
        if (existingStudent == null) {
            return Result.error("该邮箱未注册");
        }

        // 定义返回消息
        String message;
        try {
            // 判断邮箱格式是否正确
            if (sendMailUtil.isEmail(email)) {
                ValueOperations<String, Object> operations = redisTemplate.opsForValue();
                String redisKey = "reset_" + email;
                // 如果redis的验证码还存在则提示
                if (operations.get(redisKey) != null) {
                    message = "验证码已发送,请60秒后重试";
                    return Result.error(message);
                } else {
                    // 生成6位随机码
                    String code = sendMailUtil.createCode();
                    operations.set(redisKey, code);
                    // 设置验证码过期时间为60秒
                    redisTemplate.expire(redisKey, 60, TimeUnit.SECONDS);

                    // 发送验证码邮件
                    message = sendMailUtil.sendEmail(email, code);
                    return Result.success(message);
                }
            } else {
                message = "邮箱格式不正确";
                return Result.error(message);
            }
        } catch (Exception e) {            return Result.error("出现未知错误");
        }
    }

    /**
     * 重置密码
     * 
     * @param email            邮箱地址
     * @param verificationCode 验证码
     * @param newPassword      新密码
     * @return 操作结果
     */
    @PostMapping("/resetPassword")
    @Transactional(rollbackFor = Exception.class)
    public Result resetPassword(@RequestParam String email,
            @RequestParam String verificationCode,
            @RequestParam String newPassword) {
        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱地址不能为空");
        }
        if (verificationCode == null || verificationCode.trim().isEmpty()) {
            return Result.error("验证码不能为空");
        }
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error("密码长度不能少于6位");
        }

        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        String redisKey = "reset_" + email;
        Object codeObj = operations.get(redisKey);
        String code = codeObj != null ? (String) codeObj : null;

        // 验证验证码
        if (code == null || !code.equals(verificationCode)) {
            return Result.error("验证码无效或已过期");
        }

        // 查找用户并更新密码
        Student student = studentService.selectByEmail(email);
        if (student == null) {
            return Result.error("该邮箱未注册");
        }

        try {
            // 更新密码
            studentService.resetPasswordByEmail(email, newPassword);
            // 删除验证码
            redisTemplate.delete(redisKey);
            return Result.success("密码重置成功");
        } catch (Exception e) {            return Result.error("密码重置失败，请重试");
        }
    }
}