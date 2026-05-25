package com.example.controller;

import com.example.common.Result;
import com.example.entity.Clazz;
import com.example.entity.Student;
import com.example.entity.StudentClazz;
import com.example.mapper.StudentClazzMapper;
import com.example.service.ClazzService;
import com.example.service.StudentService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 班级信息前端请求接口
 */
@RestController
@RequestMapping("/clazz")
public class ClazzController {

    @Resource
    private ClazzService clazzService;

    @Resource
    private StudentService studentService;

    @Resource
    private StudentClazzMapper studentClazzMapper;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Clazz clazz) {
        clazzService.add(clazz);
        return Result.success(clazz);
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result update(@RequestBody Clazz clazz) {
        clazzService.updateById(clazz);
        return Result.success();
    }

    /**
     * 单个删除
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        clazzService.deleteById(id);
        return Result.success();
    }

    /**
     * 单个查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Clazz clazz = clazzService.selectById(id);
        return Result.success(clazz);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Clazz clazz) {
        List<Clazz> list = clazzService.selectAll(clazz);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Clazz clazz,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Clazz> pageInfo = clazzService.selectPage(clazz, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 查询指定教师的班级（分页）
     */
    @GetMapping("/selectPageByTeacher")
    public Result selectPageByTeacher(@RequestParam Integer teacherId,
                                     @RequestParam(required = false) String name,
                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        Clazz clazz = new Clazz();
        clazz.setTeacherId(teacherId);
        clazz.setName(name);
        PageInfo<Clazz> pageInfo = clazzService.selectPage(clazz, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 根据课程ID和教师ID查询班级（用于发起考试时选择班级）
     */
    @GetMapping("/selectByCourseAndTeacher")
    public Result selectByCourseAndTeacher(@RequestParam Integer courseId,
                                          @RequestParam(required = false) Integer teacherId) {
        Clazz clazz = new Clazz();
        clazz.setCourseId(courseId);
        if (teacherId != null) {
            clazz.setTeacherId(teacherId);
        }
        List<Clazz> list = clazzService.selectAll(clazz);
        return Result.success(list);
    }

    /**
     * 根据学生ID查询其所在的班级列表（用于学生端展示）
     */
    @GetMapping("/selectByStudent/{studentId}")
    public Result selectByStudent(@PathVariable Integer studentId) {
        List<StudentClazz> studentClazzList = studentClazzMapper.selectByStudentId(studentId);
        List<Clazz> clazzList = new ArrayList<>();
        for (StudentClazz sc : studentClazzList) {
            Clazz clazz = clazzService.selectById(sc.getClazzId());
            if (clazz != null) {
                clazzList.add(clazz);
            }
        }
        return Result.success(clazzList);
    }

    /**
     * 获取班级的学生名单
     */
    @GetMapping("/getStudents/{clazzId}")
    public Result getStudents(@PathVariable Integer clazzId) {
        List<StudentClazz> studentClazzList = studentClazzMapper.selectByClazzId(clazzId);
        List<Student> students = new ArrayList<>();
        for (StudentClazz sc : studentClazzList) {
            Student student = studentService.selectById(sc.getStudentId());
            if (student != null) {
                students.add(student);
            }
        }
        return Result.success(students);
    }

    /**
     * 添加学生到班级
     */
    @PostMapping("/addStudent")
    public Result addStudent(@RequestBody StudentClazz studentClazz) {
        // 验证学生是否存在且已审核通过
        Student student = studentService.selectById(studentClazz.getStudentId());
        if (student == null) {
            return Result.error("学生不存在");
        }
        if (!"审核通过".equals(student.getStatus())) {
            return Result.error("该学生尚未审核通过，无法添加到班级");
        }
        studentClazzMapper.insert(studentClazz);
        return Result.success();
    }

    /**
     * 从班级移除学生
     */
    @DeleteMapping("/removeStudent/{clazzId}/{studentId}")
    public Result removeStudent(@PathVariable Integer clazzId, @PathVariable Integer studentId) {
        studentClazzMapper.deleteByClazzIdAndStudentId(clazzId, studentId);
        return Result.success();
    }

    /**
     * 批量删除班级
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        clazzService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * Excel批量导入学生到班级
     */
    @PostMapping("/importStudents")
    public Result importStudents(@RequestParam("file") MultipartFile file,
                                  @RequestParam("clazzId") Integer clazzId) {
        try {
            // 读取Excel文件
            InputStream inputStream = file.getInputStream();
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            // 获取已在班级中的学生ID
            List<StudentClazz> existingStudents = studentClazzMapper.selectByClazzId(clazzId);
            Set<Integer> existingStudentIds = new HashSet<>();
            for (StudentClazz sc : existingStudents) {
                existingStudentIds.add(sc.getStudentId());
            }

            // 第一阶段：验证所有学生ID
            List<Integer> studentIds = new ArrayList<>();
            StringBuilder errorMsg = new StringBuilder();

            // 从第3行开始读取B列（索引1）的学生ID
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell cell = row.getCell(1); // B列（索引1）
                if (cell == null) continue;

                Integer studentId = null;
                try {
                    // 尝试读取数字
                    if (cell.getCellType() == CellType.NUMERIC) {
                        studentId = (int) cell.getNumericCellValue();
                    } else if (cell.getCellType() == CellType.STRING) {
                        String value = cell.getStringCellValue().trim();
                        if (value.isEmpty()) {
                            continue; // 跳过空行
                        }
                        studentId = Integer.parseInt(value);
                    } else if (cell.getCellType() == CellType.BLANK) {
                        continue; // 跳过空白单元格
                    } else {
                        errorMsg.append("第").append(i + 1).append("行：不是有效的学生ID\n");
                        continue;
                    }
                } catch (NumberFormatException e) {
                    errorMsg.append("第").append(i + 1).append("行：格式错误\n");
                    continue;
                }

                if (studentId == null) continue;

                // 检查学生是否存在
                Student student = studentService.selectById(studentId);
                if (student == null) {
                    errorMsg.append("第").append(i + 1).append("行：学生ID ").append(studentId).append(" 不存在\n");
                    continue;
                }

                // 检查学生是否已审核通过
                if (!"审核通过".equals(student.getStatus())) {
                    errorMsg.append("第").append(i + 1).append("行：学生ID ").append(studentId).append(" 尚未审核通过\n");
                    continue;
                }

                // 检查是否已在班级中（跳过）
                if (existingStudentIds.contains(studentId)) {
                    continue;
                }

                // 检查是否重复添加（跳过）
                if (studentIds.contains(studentId)) {
                    continue;
                }

                studentIds.add(studentId);
            }

            workbook.close();

            // 如果有任何错误，返回失败
            if (errorMsg.length() > 0) {
                return Result.error("导入失败！\n错误详情：\n" + errorMsg.toString());
            }

            // 如果没有待导入学生
            if (studentIds.isEmpty()) {
                return Result.error("导入失败：没有找到有效的学生数据");
            }

            // 第二阶段：批量插入学生
            int importedCount = 0;
            for (Integer studentId : studentIds) {
                StudentClazz studentClazz = new StudentClazz();
                studentClazz.setClazzId(clazzId);
                studentClazz.setStudentId(studentId);
                studentClazzMapper.insert(studentClazz);
                importedCount++;
            }

            String message = "导入成功！共导入 " + importedCount + " 名学生";
            return Result.success(message);

        } catch (Exception e) {            return Result.error("导入失败：" + e.getMessage());
        }
    }
}
