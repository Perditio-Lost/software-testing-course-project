package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.common.dto.Account;
import com.example.entity.Student;
import com.example.exception.CustomException;
import com.example.mapper.StudentMapper;
import com.example.common.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 学生信息业务层方法
 */
@Service
public class StudentService {

    @Resource
    private StudentMapper studentMapper;

    public void add(Student student) {
        Student dbStudent = studentMapper.selectByUsername(student.getUsername());
        if (ObjectUtil.isNotNull(dbStudent)) {
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if (ObjectUtil.isEmpty(student.getPassword())) {
            student.setPassword(Constants.USER_DEFAULT_PASSWORD);
        }
        if (ObjectUtil.isEmpty(student.getName())) {
            student.setName(student.getUsername());
        }
        student.setRole(RoleEnum.STUDENT.name());
        student.setStatus("待审核");
        studentMapper.insert(student);
    }

    public void updateById(Student student) {
        studentMapper.updateById(student);
    }

    public void deleteById(Integer id) {
        studentMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            studentMapper.deleteById(id);
        }
    }

    public Student selectById(Integer id) {
        return studentMapper.selectById(id);
    }

    public Student selectByUsername(String username) {
        return studentMapper.selectByUsername(username);
    }

    public Student selectByEmail(String email) {
        return studentMapper.selectByEmail(email);
    }

    public List<Student> selectAll(Student student) {
        return studentMapper.selectAll(student);
    }

    public List<Student> selectApproved(Student student) {
        return studentMapper.selectApproved(student);
    }

    public PageInfo<Student> selectPage(Student student, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Student> list = studentMapper.selectAll(student);
        return PageInfo.of(list);
    }

    /**
     * 登录
     */
    public Student login(Account account) {
        Student dbStudent = studentMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbStudent)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!dbStudent.getPassword().equals(account.getPassword())) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        if (!"审核通过".equals(dbStudent.getStatus())) {
            throw new CustomException("-1", "您的注册信息尚未审核通过，暂不允许登录，请耐心等待管理员审核");
        }
        // 生成token
        String token = TokenUtils.createToken(dbStudent.getId() + "-" + dbStudent.getRole(), dbStudent.getPassword());
        dbStudent.setToken(token);
        return dbStudent;
    }

    /**
     * 修改密码
     */
    public void updatePassword(Account account) {
        Student dbStudent = studentMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbStudent)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!account.getPassword().equals(dbStudent.getPassword())) {
            throw new CustomException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }
        dbStudent.setPassword(account.getNewPassword());
        studentMapper.updateById(dbStudent);
    }

    public void register(Account account) {
        Student student = new Student();
        if (account != null) {
            BeanUtils.copyProperties(account, student);
        }
        add(student);
    }

    /**
     * 通过邮箱重置密码
     * 
     * @param email       邮箱地址
     * @param newPassword 新密码
     */
    public void resetPasswordByEmail(String email, String newPassword) {
        Student dbStudent = studentMapper.selectByEmail(email);
        if (ObjectUtil.isNull(dbStudent)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        dbStudent.setPassword(newPassword);
        studentMapper.updateById(dbStudent);
    }
}
