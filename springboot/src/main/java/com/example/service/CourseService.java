package com.example.service;

import com.example.common.enums.RoleEnum;
import com.example.common.dto.Account;
import com.example.entity.Clazz;
import com.example.entity.Course;
import com.example.mapper.CourseMapper;
import com.example.common.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课程信息业务层方法
 */
@Service
public class CourseService {

    @Resource
    private CourseMapper courseMapper;
    
    @Resource
    private ClazzService clazzService;

    public void add(Course course) {
        Account currentUser = TokenUtils.getCurrentUser();
        course.setAdminId(currentUser.getId());
        courseMapper.insert(course);
        
        // 自动创建默认班级
        Clazz defaultClazz = new Clazz();
        defaultClazz.setName(course.getName() + "默认班级");
        defaultClazz.setCourseId(course.getId());
        defaultClazz.setTeacherId(null); // 授课教师为空
        clazzService.add(defaultClazz);
    }

    public void updateById(Course course) {
        courseMapper.updateById(course);
    }

    public void deleteById(Integer id) {
        // 检查是否有关联的班级
        Clazz clazz = new Clazz();
        clazz.setCourseId(id);
        List<Clazz> clazzList = clazzService.selectAll(clazz);
        if (clazzList != null && !clazzList.isEmpty()) {
            throw new com.example.exception.CustomException("-1", "该课程存在课程班，删除失败");
        }
        courseMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        // 先检查所有课程是否都可以删除
        for (Integer id : ids) {
            Clazz clazz = new Clazz();
            clazz.setCourseId(id);
            List<Clazz> clazzList = clazzService.selectAll(clazz);
            if (clazzList != null && !clazzList.isEmpty()) {
                throw new com.example.exception.CustomException("-1", "该课程存在课程班，删除失败");
            }
        }
        // 所有检查通过后再删除
        for (Integer id : ids) {
            courseMapper.deleteById(id);
        }
    }

    public Course selectById(Integer id) {
        return courseMapper.selectById(id);
    }

    public List<Course> selectAll(Course course) {
        return courseMapper.selectAll(course);
    }

    /**
     * 查询教师所教的课程（通过班级关联）
     */
    public List<Course> selectByTeacherId(Integer teacherId) {
        return courseMapper.selectByTeacherId(teacherId);
    }

    public PageInfo<Course> selectPage(Course course, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.ADMIN.name().equals(currentUser.getRole())) {
            course.setAdminId(currentUser.getId());
        }
        PageHelper.startPage(pageNum, pageSize);
        List<Course> list = courseMapper.selectAll(course);
        return PageInfo.of(list);
    }

}
