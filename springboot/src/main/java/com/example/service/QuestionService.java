package com.example.service;

import com.example.common.enums.RoleEnum;
import com.example.common.dto.Account;
import com.example.entity.Choice;
import com.example.entity.Composite;
import com.example.entity.Question;
import com.example.entity.TestPaperQuestion;
import com.example.mapper.CourseMapper;
import com.example.mapper.QuestionMapper;
import com.example.common.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 题目信息业务层方法
 */
@Service
public class QuestionService {

    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private com.example.mapper.ChoiceMapper choiceMapper;
    @Resource
    private com.example.mapper.CompositeMapper compositeMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private com.example.mapper.TestPaperQuestionMapper testPaperQuestionMapper;

    public void add(Question question) {
        Account currentUser = TokenUtils.getCurrentUser();
        question.setTeacherId(currentUser.getId());

        // 所有题目默认 flag=1（可作为独立题目）
        // 只有复合题的子题目才会在后面设置为 flag=0
        question.setFlag(true);

        // 插入题目
        questionMapper.insert(question);

        // 插入选项（如果有）
        if (question.getChoiceList() != null && !question.getChoiceList().isEmpty()) {
            int seq = 1;
            for (com.example.entity.Choice c : question.getChoiceList()) {
                c.setQuestionId(question.getId());
                c.setSequence(seq++);
                choiceMapper.insert(c);
            }
        }

        // 复合题：处理子题目
        if (question.getQuestionTypeId() != null && question.getQuestionTypeId() == 6
                && question.getQuestionList() != null && !question.getQuestionList().isEmpty()) {
            int subSeq = 1;
            for (Question subQuestion : question.getQuestionList()) {
                // 子题目设置：flag=0，继承父题目的courseId和teacherId
                subQuestion.setFlag(false);
                subQuestion.setCourseId(question.getCourseId());
                subQuestion.setTeacherId(question.getTeacherId());
                subQuestion.setLevel(question.getLevel());

                // 插入子题目
                questionMapper.insert(subQuestion);

                // 插入子题目的选项
                if (subQuestion.getChoiceList() != null && !subQuestion.getChoiceList().isEmpty()) {
                    int choiceSeq = 1;
                    for (com.example.entity.Choice c : subQuestion.getChoiceList()) {
                        c.setQuestionId(subQuestion.getId());
                        c.setSequence(choiceSeq++);
                        choiceMapper.insert(c);
                    }
                }

                // 插入父子关系
                com.example.entity.Composite composite = new com.example.entity.Composite();
                composite.setQuestionFatherId(question.getId());
                composite.setQuestionSonId(subQuestion.getId());
                composite.setSequence(subSeq++);
                compositeMapper.insert(composite);
            }
        }
    }

    public void updateById(Question question) {
        // 更新题目基本信息
        questionMapper.updateById(question);

        // 先删除旧选项，再插入新选项
        choiceMapper.deleteByQuestionId(question.getId());
        if (question.getChoiceList() != null && !question.getChoiceList().isEmpty()) {
            int seq = 1;
            for (com.example.entity.Choice c : question.getChoiceList()) {
                c.setQuestionId(question.getId());
                c.setSequence(seq++);
                choiceMapper.insert(c);
            }
        }

        // 复合题：处理子题目更新
        if (question.getQuestionTypeId() != null && question.getQuestionTypeId() == 6) {
            // 先获取旧的子题目关系
            List<com.example.entity.Composite> oldComposites = compositeMapper.selectByFatherId(question.getId());

            // 删除旧的子题目及其选项
            if (oldComposites != null) {
                for (com.example.entity.Composite comp : oldComposites) {
                    choiceMapper.deleteByQuestionId(comp.getQuestionSonId());
                    questionMapper.deleteById(comp.getQuestionSonId());
                }
            }
            // 删除旧的父子关系
            compositeMapper.deleteByFatherId(question.getId());

            // 插入新的子题目
            if (question.getQuestionList() != null && !question.getQuestionList().isEmpty()) {
                int subSeq = 1;
                for (Question subQuestion : question.getQuestionList()) {
                    // 子题目设置：flag=0
                    subQuestion.setFlag(false);
                    subQuestion.setCourseId(question.getCourseId());
                    subQuestion.setTeacherId(question.getTeacherId());
                    subQuestion.setLevel(question.getLevel());
                    subQuestion.setId(null); // 确保是新插入

                    // 插入子题目
                    questionMapper.insert(subQuestion);

                    // 插入子题目的选项
                    if (subQuestion.getChoiceList() != null && !subQuestion.getChoiceList().isEmpty()) {
                        int choiceSeq = 1;
                        for (com.example.entity.Choice c : subQuestion.getChoiceList()) {
                            c.setQuestionId(subQuestion.getId());
                            c.setSequence(choiceSeq++);
                            choiceMapper.insert(c);
                        }
                    }

                    // 插入父子关系
                    com.example.entity.Composite composite = new com.example.entity.Composite();
                    composite.setQuestionFatherId(question.getId());
                    composite.setQuestionSonId(subQuestion.getId());
                    composite.setSequence(subSeq++);
                    compositeMapper.insert(composite);
                }
            }
        }
    }

    public void deleteById(Integer id) {
        // 检查题目是否被试卷使用
        List<TestPaperQuestion> testPaperQuestions = testPaperQuestionMapper.selectByQuestionId(id);
        if (testPaperQuestions != null && !testPaperQuestions.isEmpty()) {
            throw new com.example.exception.CustomException("-1", "该题目已被使用于试卷中，删除失败");
        }
        
        // 获取题目信息判断是否为复合题
        Question question = questionMapper.selectById(id);

        // 如果是复合题，先检查子题目是否被使用
        if (question != null && question.getQuestionTypeId() != null && question.getQuestionTypeId() == 6) {
            List<com.example.entity.Composite> composites = compositeMapper.selectByFatherId(id);
            if (composites != null) {
                for (com.example.entity.Composite comp : composites) {
                    List<TestPaperQuestion> subTestPaperQuestions = testPaperQuestionMapper.selectByQuestionId(comp.getQuestionSonId());
                    if (subTestPaperQuestions != null && !subTestPaperQuestions.isEmpty()) {
                        throw new com.example.exception.CustomException("-1", "该题目已被使用于试卷中，删除失败");
                    }
                    choiceMapper.deleteByQuestionId(comp.getQuestionSonId());
                    questionMapper.deleteById(comp.getQuestionSonId());
                }
            }
            compositeMapper.deleteByFatherId(id);
        }

        // 删除选项
        choiceMapper.deleteByQuestionId(id);
        questionMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        // 先检查所有题目是否都可以删除
        for (Integer id : ids) {
            List<TestPaperQuestion> testPaperQuestions = testPaperQuestionMapper.selectByQuestionId(id);
            if (testPaperQuestions != null && !testPaperQuestions.isEmpty()) {
                throw new com.example.exception.CustomException("-1", "该题目已被使用于试卷中，删除失败");
            }
            // 检查复合题的子题目
            Question question = questionMapper.selectById(id);
            if (question != null && question.getQuestionTypeId() != null && question.getQuestionTypeId() == 6) {
                List<com.example.entity.Composite> composites = compositeMapper.selectByFatherId(id);
                if (composites != null) {
                    for (com.example.entity.Composite comp : composites) {
                        List<TestPaperQuestion> subTestPaperQuestions = testPaperQuestionMapper.selectByQuestionId(comp.getQuestionSonId());
                        if (subTestPaperQuestions != null && !subTestPaperQuestions.isEmpty()) {
                            throw new com.example.exception.CustomException("-1", "该题目已被使用于试卷中，删除失败");
                        }
                    }
                }
            }
        }
        // 所有检查通过后再删除
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    public Question selectById(Integer id) {
        Question q = questionMapper.selectById(id);
        if (q != null) {
            q.setChoiceList(choiceMapper.selectByQuestionId(q.getId()));
            // 如果是复合题（variety为composite），加载子题目
            if ("composite".equals(q.getQuestionTypeVariety())) {
                List<com.example.entity.Composite> composites = compositeMapper.selectByFatherId(q.getId());
                if (composites != null && !composites.isEmpty()) {
                    List<Question> subQuestions = new java.util.ArrayList<>();
                    for (com.example.entity.Composite composite : composites) {
                        Question subQ = questionMapper.selectByIdSimple(composite.getQuestionSonId());
                        if (subQ != null) {
                            subQ.setChoiceList(choiceMapper.selectByQuestionId(subQ.getId()));
                            subQuestions.add(subQ);
                        }
                    }
                    q.setQuestionList(subQuestions);
                }
            }
        }
        return q;
    }

    public List<Question> selectAll(Question question) {
        List<Question> list = questionMapper.selectAll(question);
        if (list != null && !list.isEmpty()) {
            for (Question q : list) {
                // 加载选择题的选项
                q.setChoiceList(choiceMapper.selectByQuestionId(q.getId()));

                // 加载复合题的子题（variety为composite）
                if ("composite".equals(q.getQuestionTypeVariety())) {
                    List<Composite> composites = compositeMapper.selectByFatherId(q.getId());
                    if (composites != null && !composites.isEmpty()) {
                        List<Question> subQuestions = new ArrayList<>();
                        for (Composite composite : composites) {
                            Question subQ = questionMapper.selectById(composite.getQuestionSonId());
                            if (subQ != null) {
                                subQ.setChoiceList(choiceMapper.selectByQuestionId(subQ.getId()));
                                subQuestions.add(subQ);
                            }
                        }
                        q.setQuestionList(subQuestions);
                    }
                }
            }
        }
        return list;
    }

    public PageInfo<Question> selectPage(Question question, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        // 题库所有教师都能查看，不再限制教师ID
        // if (RoleEnum.TEACHER.name().equals(currentUser.getRole())) {
        //     question.setTeacherId(currentUser.getId());
        // }
        PageHelper.startPage(pageNum, pageSize);
        List<Question> list = questionMapper.selectAll(question);
        if (list != null && !list.isEmpty()) {
            for (Question q : list) {
                q.setChoiceList(choiceMapper.selectByQuestionId(q.getId()));
            }
        }
        return PageInfo.of(list);
    }

    /**
     * 从Excel导入题目
     */
    public String importQuestionsFromExcel(MultipartFile file) throws Exception {
        Account currentUser = TokenUtils.getCurrentUser();
        Integer teacherId = currentUser.getId();

        InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);

        // 存储待导入的题目数据
        List<QuestionImportData> importDataList = new ArrayList<>();
        StringBuilder errorMsg = new StringBuilder();

        // 第一阶段：验证所有题目格式
        int i = 12;  // 从第13行开始
        while (i <= sheet.getLastRowNum()) {
            Row row = sheet.getRow(i);
            if (row == null) {
                i++;
                continue;
            }

            // 检查是否为空行（课程名称为空则认为是空行）
            String courseName = getCellValue(row.getCell(0));
            if (courseName == null || courseName.trim().isEmpty()) {
                i++;
                continue;
            }

            try {
                QuestionImportData data = new QuestionImportData();
                data.rowNumber = i + 1;

                // 1. 验证课程名称
                courseName = courseName.trim();
                Integer courseId = courseMapper.selectIdByName(courseName);
                if (courseId == null) {
                    errorMsg.append("第").append(i + 1).append("行：课程'").append(courseName).append("'不存在\n");
                    continue;
                }
                data.courseId = courseId;

                // 2. 验证题型
                String questionTypeName = getCellValue(row.getCell(1));
                if (questionTypeName == null || questionTypeName.trim().isEmpty()) {
                    errorMsg.append("第").append(i + 1).append("行：题目类型不能为空\n");
                    continue;
                }
                Integer questionTypeId = getQuestionTypeId(questionTypeName.trim());
                if (questionTypeId == null) {
                    errorMsg.append("第").append(i + 1).append("行：题型'").append(questionTypeName).append("'无效\n");
                    continue;
                }
                data.questionTypeId = questionTypeId;

                // 3. 验证难度系数
                String levelStr = getCellValue(row.getCell(2));
                if (levelStr == null || levelStr.trim().isEmpty()) {
                    errorMsg.append("第").append(i + 1).append("行：难度系数不能为空\n");
                    continue;
                }
                Double level;
                try {
                    level = Double.parseDouble(levelStr.trim());
                    if (level < 0 || level > 1) {
                        errorMsg.append("第").append(i + 1).append("行：难度系数必须在0-1之间\n");
                        continue;
                    }
                } catch (NumberFormatException e) {
                    errorMsg.append("第").append(i + 1).append("行：难度系数格式错误\n");
                    continue;
                }
                data.level = level;

                // 4. 验证题干
                String questionStem = getCellValue(row.getCell(3));
                if (questionStem == null || questionStem.trim().isEmpty()) {
                    errorMsg.append("第").append(i + 1).append("行：题干不能为空\n");
                    continue;
                }
                data.questionStem = questionStem.trim();

                // 5. 读取答案（E列，索引4）- 单选、多选、判断题专用
                String answerStr = getCellValue(row.getCell(4));
                
                // 6. 读取题目解析（F列，索引5）
                String analysis = getCellValue(row.getCell(5));
                data.analysis = analysis != null ? analysis.trim() : "";

                // 7. 读取子题目个数（G列，索引6）
                String subQuestionCountStr = getCellValue(row.getCell(6));
                int subQuestionCount = 0;
                if (subQuestionCountStr != null && !subQuestionCountStr.trim().isEmpty()) {
                    try {
                        subQuestionCount = Integer.parseInt(subQuestionCountStr.trim());
                    } catch (NumberFormatException e) {
                        errorMsg.append("第").append(i + 1).append("行：子题目个数格式错误\n");
                        i++;
                        continue;
                    }
                }

                // 8. 根据题型验证答案、选项和子题目
                if (questionTypeId == 1 || questionTypeId == 2) {
                    // 单选题、多选题：子题目个数必须为0或空
                    if (subQuestionCount > 0) {
                        errorMsg.append("第").append(i + 1).append("行：单选题和多选题不能有子题目\n");
                        i++;
                        continue;
                    }
                    
                    // 答案不能为空
                    if (answerStr == null || answerStr.trim().isEmpty()) {
                        errorMsg.append("第").append(i + 1).append("行：").append(questionTypeName).append("的答案不能为空\n");
                        i++;
                        continue;
                    }
                    data.correctAnswer = answerStr.trim().toUpperCase();

                    // 读取选项（从H列开始，索引7及之后）
                    List<ChoiceData> choices = new ArrayList<>();
                    for (int j = 7; j < 100; j++) {
                        String optionContent = getCellValue(row.getCell(j));
                        if (optionContent == null || optionContent.trim().isEmpty()) {
                            if (j > 7 && getCellValue(row.getCell(j + 1)) == null) {
                                break;
                            }
                            continue;
                        }

                        String content = optionContent.trim();
                        if (!content.matches("^[A-Z]\\..*")) {
                            errorMsg.append("第").append(i + 1).append("行，第").append(j + 1).append("列：选项格式错误，必须为'A.内容'格式\n");
                            choices.clear();
                            break;
                        }

                        String identification = content.substring(0, 1);
                        String choiceContent = content.substring(2).trim();
                        if (choiceContent.isEmpty()) {
                            errorMsg.append("第").append(i + 1).append("行，第").append(j + 1).append("列：选项内容不能为空\n");
                            choices.clear();
                            break;
                        }

                        ChoiceData choice = new ChoiceData();
                        choice.identification = identification;
                        choice.content = choiceContent;
                        choices.add(choice);
                    }

                    if (choices.isEmpty()) {
                        errorMsg.append("第").append(i + 1).append("行：").append(questionTypeName).append("的选项个数不能为0\n");
                        i++;
                        continue;
                    }
                    data.choices = choices;

                } else if (questionTypeId == 3) {
                    // 判断题：子题目个数必须为0或空
                    if (subQuestionCount > 0) {
                        errorMsg.append("第").append(i + 1).append("行：判断题不能有子题目\n");
                        i++;
                        continue;
                    }
                    
                    // 答案必须是"正确"或"错误"
                    if (answerStr == null || answerStr.trim().isEmpty()) {
                        errorMsg.append("第").append(i + 1).append("行：判断题的答案不能为空\n");
                        i++;
                        continue;
                    }
                    answerStr = answerStr.trim();
                    if (!answerStr.equals("正确") && !answerStr.equals("错误")) {
                        errorMsg.append("第").append(i + 1).append("行：判断题答案必须是'正确'或'错误'\n");
                        i++;
                        continue;
                    }
                    data.correctAnswer = answerStr;

                } else if (questionTypeId == 6) {
                    // 复合题：子题目个数必须大于0
                    if (subQuestionCount <= 0) {
                        errorMsg.append("第").append(i + 1).append("行：复合题的子题目个数必须大于0\n");
                        i++;
                        continue;
                    }
                    
                    // 验证后面有足够的子题目
                    if (i + subQuestionCount > sheet.getLastRowNum()) {
                        errorMsg.append("第").append(i + 1).append("行：复合题的子题目数量不足（需要").append(subQuestionCount).append("个）\n");
                        i++;
                        continue;
                    }
                    
                    // 读取并验证子题目
                    List<QuestionImportData> subQuestions = new ArrayList<>();
                    boolean subQuestionsValid = true;
                    
                    for (int subIdx = 1; subIdx <= subQuestionCount; subIdx++) {
                        int subRowIdx = i + subIdx;
                        Row subRow = sheet.getRow(subRowIdx);
                        
                        if (subRow == null) {
                            errorMsg.append("第").append(i + 1).append("行：第").append(subIdx).append("个子题目为空\n");
                            subQuestionsValid = false;
                            break;
                        }
                        
                        // 验证子题目的课程名称
                        String subCourseName = getCellValue(subRow.getCell(0));
                        if (subCourseName == null || !subCourseName.trim().equals(courseName)) {
                            errorMsg.append("第").append(subRowIdx + 1).append("行：子题目的课程名称必须与父题目一致\n");
                            subQuestionsValid = false;
                            break;
                        }
                        
                        // 验证子题目类型（不能是复合题）
                        String subQuestionTypeName = getCellValue(subRow.getCell(1));
                        if (subQuestionTypeName == null || subQuestionTypeName.trim().isEmpty()) {
                            errorMsg.append("第").append(subRowIdx + 1).append("行：子题目类型不能为空\n");
                            subQuestionsValid = false;
                            break;
                        }
                        Integer subQuestionTypeId = getQuestionTypeId(subQuestionTypeName.trim());
                        if (subQuestionTypeId == null) {
                            errorMsg.append("第").append(subRowIdx + 1).append("行：子题目类型'").append(subQuestionTypeName).append("'无效\n");
                            subQuestionsValid = false;
                            break;
                        }
                        if (subQuestionTypeId == 6) {
                            errorMsg.append("第").append(subRowIdx + 1).append("行：子题目不能是复合题\n");
                            subQuestionsValid = false;
                            break;
                        }
                        
                        // 验证子题目的子题目个数必须为0或空
                        String subSubCountStr = getCellValue(subRow.getCell(6));
                        if (subSubCountStr != null && !subSubCountStr.trim().isEmpty()) {
                            try {
                                int subSubCount = Integer.parseInt(subSubCountStr.trim());
                                if (subSubCount > 0) {
                                    errorMsg.append("第").append(subRowIdx + 1).append("行：子题目不能有子题目\n");
                                    subQuestionsValid = false;
                                    break;
                                }
                            } catch (NumberFormatException e) {
                                // 忽略格式错误，后续会处理
                            }
                        }
                        
                        // 创建子题目数据（简化验证，完整验证在递归处理时进行）
                        QuestionImportData subData = new QuestionImportData();
                        subData.rowNumber = subRowIdx + 1;
                        subData.courseId = courseId;
                        subData.questionTypeId = subQuestionTypeId;
                        subData.isSubQuestion = true;
                        
                        // 验证难度系数
                        String subLevelStr = getCellValue(subRow.getCell(2));
                        if (subLevelStr == null || subLevelStr.trim().isEmpty()) {
                            errorMsg.append("第").append(subRowIdx + 1).append("行：难度系数不能为空\n");
                            subQuestionsValid = false;
                            break;
                        }
                        try {
                            Double subLevel = Double.parseDouble(subLevelStr.trim());
                            if (subLevel < 0 || subLevel > 1) {
                                errorMsg.append("第").append(subRowIdx + 1).append("行：难度系数必须在0-1之间\n");
                                subQuestionsValid = false;
                                break;
                            }
                            subData.level = subLevel;
                        } catch (NumberFormatException e) {
                            errorMsg.append("第").append(subRowIdx + 1).append("行：难度系数格式错误\n");
                            subQuestionsValid = false;
                            break;
                        }
                        
                        // 验证题干
                        String subStem = getCellValue(subRow.getCell(3));
                        if (subStem == null || subStem.trim().isEmpty()) {
                            errorMsg.append("第").append(subRowIdx + 1).append("行：题干不能为空\n");
                            subQuestionsValid = false;
                            break;
                        }
                        subData.questionStem = subStem.trim();
                        
                        // 读取答案和解析
                        String subAnswerStr = getCellValue(subRow.getCell(4));
                        String subAnalysis = getCellValue(subRow.getCell(5));
                        subData.analysis = subAnalysis != null ? subAnalysis.trim() : "";
                        
                        // 根据子题目类型验证
                        if (subQuestionTypeId == 1 || subQuestionTypeId == 2) {
                            // 单选题、多选题
                            if (subAnswerStr == null || subAnswerStr.trim().isEmpty()) {
                                errorMsg.append("第").append(subRowIdx + 1).append("行：答案不能为空\n");
                                subQuestionsValid = false;
                                break;
                            }
                            subData.correctAnswer = subAnswerStr.trim().toUpperCase();
                            
                            // 读取选项
                            List<ChoiceData> subChoices = new ArrayList<>();
                            for (int j = 7; j < 100; j++) {
                                String subOptionContent = getCellValue(subRow.getCell(j));
                                if (subOptionContent == null || subOptionContent.trim().isEmpty()) {
                                    if (j > 7 && getCellValue(subRow.getCell(j + 1)) == null) {
                                        break;
                                    }
                                    continue;
                                }
                                
                                String content = subOptionContent.trim();
                                if (!content.matches("^[A-Z]\\..*")) {
                                    errorMsg.append("第").append(subRowIdx + 1).append("行，第").append(j + 1).append("列：选项格式错误\n");
                                    subChoices.clear();
                                    subQuestionsValid = false;
                                    break;
                                }
                                
                                String identification = content.substring(0, 1);
                                String choiceContent = content.substring(2).trim();
                                if (choiceContent.isEmpty()) {
                                    errorMsg.append("第").append(subRowIdx + 1).append("行，第").append(j + 1).append("列：选项内容不能为空\n");
                                    subChoices.clear();
                                    subQuestionsValid = false;
                                    break;
                                }
                                
                                ChoiceData choice = new ChoiceData();
                                choice.identification = identification;
                                choice.content = choiceContent;
                                subChoices.add(choice);
                            }
                            
                            if (!subQuestionsValid) break;
                            
                            if (subChoices.isEmpty()) {
                                errorMsg.append("第").append(subRowIdx + 1).append("行：选项个数不能为0\n");
                                subQuestionsValid = false;
                                break;
                            }
                            subData.choices = subChoices;
                            
                        } else if (subQuestionTypeId == 3) {
                            // 判断题
                            if (subAnswerStr == null || subAnswerStr.trim().isEmpty()) {
                                errorMsg.append("第").append(subRowIdx + 1).append("行：判断题的答案不能为空\n");
                                subQuestionsValid = false;
                                break;
                            }
                            subAnswerStr = subAnswerStr.trim();
                            if (!subAnswerStr.equals("正确") && !subAnswerStr.equals("错误")) {
                                errorMsg.append("第").append(subRowIdx + 1).append("行：判断题答案必须是'正确'或'错误'\n");
                                subQuestionsValid = false;
                                break;
                            }
                            subData.correctAnswer = subAnswerStr;
                        }
                        
                        subQuestions.add(subData);
                    }
                    
                    if (!subQuestionsValid) {
                        i++;
                        continue;
                    }
                    
                    data.subQuestions = subQuestions;
                    data.subQuestionCount = subQuestionCount;
                    
                    // 跳过子题目行
                    i += subQuestionCount;
                    
                } else {
                    // 简答题、填空题等
                    if (subQuestionCount > 0) {
                        errorMsg.append("第").append(i + 1).append("行：该题型不支持子题目\n");
                        i++;
                        continue;
                    }
                }

                importDataList.add(data);

            } catch (Exception e) {
                errorMsg.append("第").append(i + 1).append("行：处理异常 - ").append(e.getMessage()).append("\n");
            }
            
            i++;
        }

        workbook.close();

        // 如果有任何错误，返回失败
        if (errorMsg.length() > 0) {
            return "导入失败！\n错误详情：\n" + errorMsg.toString();
        }

        // 如果没有待导入数据
        if (importDataList.isEmpty()) {
            return "导入失败：没有找到有效的题目数据";
        }

        // 第二阶段：批量插入所有题目
        int importedCount = 0;
        try {
            for (QuestionImportData data : importDataList) {
                // 创建题目对象
                Question question = new Question();
                question.setCourseId(data.courseId);
                question.setQuestionTypeId(data.questionTypeId);
                question.setLevel(data.level);
                question.setQuestionStem(data.questionStem);
                question.setAnswer(data.analysis); // 题目解析存入answer字段
                question.setTeacherId(teacherId);
                
                // 如果是复合题或子题目，设置flag=0，否则设置flag=1
                if (data.isSubQuestion) {
                    question.setFlag(false);  // 子题目flag=0
                } else {
                    question.setFlag(true);   // 主题目flag=1
                }

                // 插入题目
                questionMapper.insert(question);

                // 插入选项
                if (data.questionTypeId == 1 || data.questionTypeId == 2) {
                    // 单选题、多选题
                    int seq = 1;
                    for (ChoiceData choiceData : data.choices) {
                        Choice choice = new Choice();
                        choice.setQuestionId(question.getId());
                        choice.setIdentification(choiceData.identification);
                        choice.setContent(choiceData.content);
                        choice.setSequence(seq++);
                        // flag字段：true为正确答案，false为错误答案
                        boolean isCorrect = data.correctAnswer.contains(choiceData.identification);
                        choice.setFlag(isCorrect);
                        choiceMapper.insert(choice);
                    }
                } else if (data.questionTypeId == 3) {
                    // 判断题固定选项
                    Choice choice1 = new Choice();
                    choice1.setQuestionId(question.getId());
                    choice1.setIdentification("√");
                    choice1.setContent("正确");
                    choice1.setSequence(1);
                    choice1.setFlag(data.correctAnswer.equals("正确"));
                    choiceMapper.insert(choice1);

                    Choice choice2 = new Choice();
                    choice2.setQuestionId(question.getId());
                    choice2.setIdentification("×");
                    choice2.setContent("错误");
                    choice2.setSequence(2);
                    choice2.setFlag(data.correctAnswer.equals("错误"));
                    choiceMapper.insert(choice2);
                } else if (data.questionTypeId == 6) {
                    // 复合题：插入子题目
                    int sequence = 1;
                    for (QuestionImportData subData : data.subQuestions) {
                        Question subQuestion = new Question();
                        subQuestion.setCourseId(subData.courseId);
                        subQuestion.setQuestionTypeId(subData.questionTypeId);
                        subQuestion.setLevel(subData.level);
                        subQuestion.setQuestionStem(subData.questionStem);
                        subQuestion.setAnswer(subData.analysis);
                        subQuestion.setTeacherId(teacherId);
                        subQuestion.setFlag(false);  // 子题目flag=0
                        
                        questionMapper.insert(subQuestion);
                        
                        // 在composite表中建立父子关系
                        Composite composite = new Composite();
                        composite.setQuestionFatherId(question.getId());
                        composite.setQuestionSonId(subQuestion.getId());
                        composite.setSequence(sequence++);
                        compositeMapper.insert(composite);
                        
                        // 插入子题目选项
                        if (subData.questionTypeId == 1 || subData.questionTypeId == 2) {
                            int seq = 1;
                            for (ChoiceData choiceData : subData.choices) {
                                Choice choice = new Choice();
                                choice.setQuestionId(subQuestion.getId());
                                choice.setIdentification(choiceData.identification);
                                choice.setContent(choiceData.content);
                                choice.setSequence(seq++);
                                boolean isCorrect = subData.correctAnswer.contains(choiceData.identification);
                                choice.setFlag(isCorrect);
                                choiceMapper.insert(choice);
                            }
                        } else if (subData.questionTypeId == 3) {
                            Choice choice1 = new Choice();
                            choice1.setQuestionId(subQuestion.getId());
                            choice1.setIdentification("√");
                            choice1.setContent("正确");
                            choice1.setSequence(1);
                            choice1.setFlag(subData.correctAnswer.equals("正确"));
                            choiceMapper.insert(choice1);

                            Choice choice2 = new Choice();
                            choice2.setQuestionId(subQuestion.getId());
                            choice2.setIdentification("×");
                            choice2.setContent("错误");
                            choice2.setSequence(2);
                            choice2.setFlag(subData.correctAnswer.equals("错误"));
                            choiceMapper.insert(choice2);
                        }
                    }
                }

                importedCount++;
            }

            return "导入成功！共导入 " + importedCount + " 道题目";

        } catch (Exception e) {            throw new Exception("批量插入失败：" + e.getMessage());
        }
    }

    // 内部类：存储待导入的题目数据
    private static class QuestionImportData {
        int rowNumber;
        Integer courseId;
        Integer questionTypeId;
        Double level;
        String questionStem;
        String analysis;
        String correctAnswer;
        List<ChoiceData> choices;
        boolean isSubQuestion = false;  // 是否为子题目
        int subQuestionCount = 0;  // 子题目个数
        List<QuestionImportData> subQuestions;  // 子题目列表
    }

    // 内部类：存储选项数据
    private static class ChoiceData {
        String identification;
        String content;
    }

    /**
     * 获取单元格值
     */
    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                // 直接返回数字值的字符串形式，保留小数
                double numValue = cell.getNumericCellValue();
                // 如果是整数，不显示小数点
                if (numValue == (long) numValue) {
                    return String.valueOf((long) numValue);
                } else {
                    return String.valueOf(numValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    /**
     * 根据题型名称获取题型ID
     */
    private Integer getQuestionTypeId(String typeName) {
        if (typeName == null) return null;
        
        switch (typeName.trim()) {
            case "单选题":
                return 1;
            case "多选题":
                return 2;
            case "判断题":
                return 3;
            case "填空题":
                return 4;
            case "简答题":
                return 5;
            case "复合题":
                return 6;
            default:
                return null;
        }
    }

}
