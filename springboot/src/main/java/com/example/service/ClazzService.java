package com.example.service;

import com.example.entity.Clazz;
import com.example.entity.StudentClazz;
import com.example.mapper.ClazzMapper;
import com.example.mapper.StudentClazzMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClazzService {

    @Resource
    private ClazzMapper clazzMapper;

    @Resource
    private StudentClazzMapper studentClazzMapper;

    public void add(Clazz clazz) {
        clazzMapper.insert(clazz);
    }

    public void updateById(Clazz clazz) {
        clazzMapper.updateById(clazz);
    }

    @Transactional
    public void deleteById(Integer id) {
        // 先删除班级学生关联
        studentClazzMapper.deleteByClazzId(id);
        // 再删除班级
        clazzMapper.deleteById(id);
    }

    public Clazz selectById(Integer id) {
        return clazzMapper.selectById(id);
    }

    public List<Clazz> selectAll(Clazz clazz) {
        return clazzMapper.selectAll(clazz);
    }

    public PageInfo<Clazz> selectPage(Clazz clazz, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Clazz> list = clazzMapper.selectAll(clazz);
        return PageInfo.of(list);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    public List<Clazz> selectAllClazz() {
        return clazzMapper.selectAllClazz();
    }
}
