<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input
          v-model="data.questionStem"
          prefix-icon="Search"
          style="width: 240px; margin-right: 10px"
          placeholder="请输入题目名称查询"
      ></el-input>
      <el-button type="info" plain @click="load">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
      <el-button
          v-if="data.user.role === 'TEACHER'"
          type="primary"
          plain
          style="margin-left: 10px"
          @click="handleAdd"
      >新增</el-button>
      <el-button type="danger" plain style="margin-left: 10px" @click="delBatch">批量删除</el-button>
      <el-upload
          v-if="data.user.role === 'TEACHER'"
          style="display: inline-block; margin-left: 10px; vertical-align: middle"
          :action="''"
          :before-upload="handleExcelUpload"
          :show-file-list="false"
          accept=".xlsx,.xls"
      >
        <el-button type="success" plain>Excel导入题目</el-button>
      </el-upload>
      <el-button
          v-if="data.user.role === 'TEACHER'"
          type="primary"
          plain
          style="margin-left: 10px"
          @click="downloadTemplate"
      >下载导入模板</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table
          stripe
          :data="data.tableData"
          @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="questionStem" label="题目名称">
          <template v-slot="scope">
            <span style="color: #409EFF; cursor: pointer; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" @click="viewQuestion(scope.row.id)">{{ scope.row.questionStem }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="courseName" label="课程名称" />
        <el-table-column prop="teacherName" label="授课教师" width="120" />
        <el-table-column prop="questionTypeName" label="题型" width="100">
          <template v-slot="scope">
            <el-tag :type="getQuestionTypeStyle(scope.row.questionTypeName).type" :effect="getQuestionTypeStyle(scope.row.questionTypeName).effect">{{ scope.row.questionTypeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="难度系数" width="80" />
        <el-table-column label="操作" width="100" fixed="right">
          <template v-slot="scope">
            <el-button
                v-if="data.user.role === 'TEACHER' && scope.row.teacherId === data.user.id"
                type="primary"
                circle
                :icon="Edit"
                @click="handleEdit(scope.row)"
            ></el-button>
            <el-button
                v-if="scope.row.teacherId === data.user.id"
                type="danger"
                circle
                :icon="Delete"
                @click="del(scope.row.id)"
            ></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination
          @current-change="load"
          background
          layout="prev, pager, next"
          :page-size="data.pageSize"
          v-model:current-page="data.pageNum"
          :total="data.total"
      />
    </div>

    <el-dialog
        title="题目信息"
        v-model="data.formVisible"
        width="65%"
        destroy-on-close
        :close-on-click-modal="false"
        top="5vh"
    >
      <el-form
          ref="form"
          :model="data.form"
          label-width="100px"
          style="padding: 10px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item prop="courseId" label="选择课程">
              <el-select v-model="data.form.courseId" placeholder="请选择课程" style="width: 100%" filterable>
                <el-option
                    v-for="item in data.courseData"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                >
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="questionTypeId" label="选择题型">
              <el-select v-model="data.form.questionTypeId" placeholder="请选择题型" @change="handleTypeChange" style="width: 100%">
                <el-option label="单选题" :value="1"></el-option>
                <el-option label="多选题" :value="2"></el-option>
                <el-option label="判断题" :value="3"></el-option>
                <el-option label="填空题" :value="4"></el-option>
                <el-option label="简答题" :value="5"></el-option>
                <el-option label="复合题" :value="6"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item prop="level" label="难度系数">
              <el-input-number v-model="data.form.level" :precision="1" :step="0.1" :min="0.1" :max="1.0" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item prop="questionStem" label="题目名称">
          <el-input
              v-model="data.form.questionStem"
              placeholder="请输入题目名称"
              type="textarea"
              :rows="2"
          ></el-input>
        </el-form-item>

        <!-- 单选题选项（单选正确答案） -->
        <div v-if="data.form.questionTypeId === 1">
          <el-form-item label="题目选项">
            <el-card shadow="never" style="width: 100%; background-color: #f8f9fa;">
              <div style="margin-bottom: 10px; color: #909399; font-size: 13px;">
                <el-icon style="vertical-align: middle; margin-right: 4px"><InfoFilled /></el-icon>
                请勾选正确答案
              </div>
              <div v-for="(choice, idx) in data.form.choiceList" :key="idx" style="display: flex; align-items: center; gap: 10px; margin-bottom: 10px">
                <el-radio v-model="data.singleAnswer" :value="choice.identification" @change="updateChoiceFlag">
                  <span style="font-weight: bold; width: 20px; display: inline-block; text-align: center;">{{ choice.identification }}</span>
                </el-radio>
                <el-input
                    v-model="choice.identification"
                    placeholder="标识"
                    style="width: 70px"
                ></el-input>
                <el-input
                    v-model="choice.content"
                    placeholder="请输入选项内容"
                    style="flex: 1"
                ></el-input>
                <el-button
                    type="danger"
                    circle
                    :icon="Delete"
                    size="small"
                    @click="delChoice(idx)"
                    v-if="data.form.choiceList.length > 1"
                ></el-button>
              </div>
              <div style="margin-top: 10px;">
                <el-button type="primary" plain size="small" :icon="Plus" @click="addChoice">添加选项</el-button>
              </div>
            </el-card>
          </el-form-item>
        </div>
        <!-- 多选题选项（多选正确答案） -->
        <div v-if="data.form.questionTypeId === 2">
          <el-form-item label="题目选项">
            <el-card shadow="never" style="width: 100%; background-color: #f8f9fa;">
              <div style="margin-bottom: 10px; color: #909399; font-size: 13px;">
                <el-icon style="vertical-align: middle; margin-right: 4px"><InfoFilled /></el-icon>
                请勾选正确答案（可多选）
              </div>
              <div v-for="(choice, idx) in data.form.choiceList" :key="idx" style="display: flex; align-items: center; gap: 10px; margin-bottom: 10px">
                <el-checkbox v-model="choice.flag">
                  <span style="font-weight: bold; width: 20px; display: inline-block; text-align: center;">{{ choice.identification }}</span>
                </el-checkbox>
                <el-input
                    v-model="choice.identification"
                    placeholder="标识"
                    style="width: 70px"
                ></el-input>
                <el-input
                    v-model="choice.content"
                    placeholder="请输入选项内容"
                    style="flex: 1"
                ></el-input>
                <el-button
                    type="danger"
                    circle
                    :icon="Delete"
                    size="small"
                    @click="delChoice(idx)"
                    v-if="data.form.choiceList.length > 1"
                ></el-button>
              </div>
              <div style="margin-top: 10px;">
                <el-button type="primary" plain size="small" :icon="Plus" @click="addChoice">添加选项</el-button>
              </div>
            </el-card>
          </el-form-item>
        </div>
        <!-- 复合题：添加子题目 -->
        <div v-if="data.form.questionTypeId === 6">
          <el-form-item label="子题目">
            <div style="width: 100%">
              <div style="margin-bottom: 15px; color: #606266; font-size: 14px; display: flex; align-items: center; justify-content: space-between;">
                <span><el-icon style="vertical-align: middle; margin-right: 4px"><Menu /></el-icon> 子题目列表</span>
                <el-button type="success" plain size="small" :icon="Plus" @click="addSubQuestion">添加子题目</el-button>
              </div>
              
              <div v-if="data.form.questionList.length === 0" style="text-align: center; color: #909399; padding: 20px; background: #f5f7fa; border-radius: 4px;">
                暂无子题目，请点击右上角添加
              </div>

              <div v-for="(subQ, idx) in data.form.questionList" :key="idx" 
                   style="margin-bottom: 20px;">
                <el-card shadow="hover" :body-style="{ padding: '15px' }">
                  <template #header>
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                      <span style="font-weight: bold; color: #409EFF;">子题目 {{ idx + 1 }}</span>
                      <el-button type="danger" link :icon="Delete" @click="delSubQuestion(idx)">删除此题</el-button>
                    </div>
                  </template>
                  
                  <el-row :gutter="15">
                     <el-col :span="12">
                        <el-form-item label="题型" label-width="60px" style="margin-bottom: 15px;">
                          <el-select v-model="subQ.questionTypeId" placeholder="请选择题型" @change="handleSubTypeChange(idx)" style="width: 100%;">
                            <el-option label="单选题" :value="1"></el-option>
                            <el-option label="多选题" :value="2"></el-option>
                            <el-option label="判断题" :value="3"></el-option>
                            <el-option label="填空题" :value="4"></el-option>
                            <el-option label="简答题" :value="5"></el-option>
                          </el-select>
                        </el-form-item>
                     </el-col>
                     <el-col :span="24">
                        <el-form-item label="题目" label-width="60px" style="margin-bottom: 15px;">
                          <el-input v-model="subQ.questionStem" placeholder="请输入子题目内容" type="textarea" :rows="2"></el-input>
                        </el-form-item>
                     </el-col>
                  </el-row>
                  
                  <!-- 子题目选项区域 -->
                  <div style="background-color: #fafafa; padding: 15px; border-radius: 4px;">
                    <!-- 单选题选项 -->
                    <div v-if="subQ.questionTypeId === 1">
                       <div style="margin-bottom: 10px; font-size: 13px; color: #606266;">选项配置 (勾选为正确答案)</div>
                       <div v-for="(choice, cIdx) in subQ.choiceList" :key="cIdx" style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px">
                          <el-radio v-model="subQ.singleAnswer" :value="choice.identification" @change="updateSubChoiceFlag(idx)">
                            <span style="width: 10px;"></span>
                          </el-radio>
                          <el-input v-model="choice.identification" placeholder="标识" style="width: 60px"></el-input>
                          <el-input v-model="choice.content" placeholder="选项内容" style="flex: 1"></el-input>
                          <el-button type="danger" circle :icon="Delete" size="small" @click="delSubChoice(idx, cIdx)" v-if="subQ.choiceList.length > 1"></el-button>
                       </div>
                       <el-button type="primary" link size="small" :icon="Plus" @click="addSubChoice(idx)">添加选项</el-button>
                    </div>
                    
                    <!-- 多选题选项 -->
                    <div v-if="subQ.questionTypeId === 2">
                       <div style="margin-bottom: 10px; font-size: 13px; color: #606266;">选项配置 (勾选为正确答案)</div>
                       <div v-for="(choice, cIdx) in subQ.choiceList" :key="cIdx" style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px">
                          <el-checkbox v-model="choice.flag"><span style="width: 10px;"></span></el-checkbox>
                          <el-input v-model="choice.identification" placeholder="标识" style="width: 60px"></el-input>
                          <el-input v-model="choice.content" placeholder="选项内容" style="flex: 1"></el-input>
                          <el-button type="danger" circle :icon="Delete" size="small" @click="delSubChoice(idx, cIdx)" v-if="subQ.choiceList.length > 1"></el-button>
                       </div>
                       <el-button type="primary" link size="small" :icon="Plus" @click="addSubChoice(idx)">添加选项</el-button>
                    </div>
                    
                    <!-- 判断题选项 -->
                    <div v-if="subQ.questionTypeId === 3">
                        <div style="margin-bottom: 10px; font-size: 13px; color: #606266;">正确答案</div>
                        <div v-for="(choice, cIdx) in subQ.choiceList" :key="cIdx" style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px">
                          <el-radio v-model="subQ.singleAnswer" :value="choice.identification" @change="updateSubChoiceFlag(idx)">
                            <el-tag :type="choice.identification === '√' ? 'success' : 'danger'" effect="dark">{{ choice.identification }} {{ choice.content }}</el-tag>
                          </el-radio>
                        </div>
                    </div>
                    
                    <!-- 填空题/简答题答案 -->
                    <div v-if="subQ.questionTypeId === 4 || subQ.questionTypeId === 5">
                        <div style="margin-bottom: 10px; font-size: 13px; color: #606266;">参考答案</div>
                        <el-input v-if="subQ.questionTypeId === 4" v-model="subQ.answer" placeholder="请输入参考答案"></el-input>
                        <el-input v-if="subQ.questionTypeId === 5" type="textarea" :rows="3" v-model="subQ.answer" placeholder="请输入参考答案"></el-input>
                    </div>
                  </div>

                  <!-- 解析 -->
                  <div style="margin-top: 15px;" v-if="[1,2,3].includes(subQ.questionTypeId)">
                     <el-form-item label="解析" label-width="60px" style="margin-bottom: 0;">
                        <el-input type="textarea" :rows="1" v-model="subQ.answer" placeholder="请输入题目解析（选填）"></el-input>
                     </el-form-item>
                  </div>
                </el-card>
              </div>
              
              <div v-if="data.form.questionList.length > 0" style="margin-top: 15px; text-align: center;">
                 <el-button type="success" plain @click="addSubQuestion" style="width: 100%; border-style: dashed;">+ 继续添加子题目</el-button>
              </div>
            </div>
          </el-form-item>
        </div>

        <!-- 判断题固定选项（单选正确答案） -->
        <div v-if="data.form.questionTypeId === 3">
          <el-form-item label="题目选项">
            <el-card shadow="never" style="width: 100%; background-color: #f8f9fa;">
                <div style="margin-bottom: 10px; color: #909399; font-size: 13px;">请选择正确答案</div>
                <div v-for="(choice, idx) in data.form.choiceList" :key="idx" style="display: flex; align-items: center; gap: 20px; margin-bottom: 5px">
                  <el-radio v-model="data.singleAnswer" :value="choice.identification" @change="updateChoiceFlag">
                    <el-tag :type="choice.identification === '√' ? 'success' : 'danger'" size="large" effect="dark">{{ choice.identification }} {{ choice.content }}</el-tag>
                  </el-radio>
                </div>
            </el-card>
          </el-form-item>
        </div>

        <!-- 单选题、多选题、判断题的题目解析 -->
        <el-form-item
            prop="answer"
            label="题目解析"
            v-if="data.form.questionTypeId === 1 || data.form.questionTypeId === 2 || data.form.questionTypeId === 3"
        >
          <el-input
              type="textarea"
              :rows="3"
              v-model="data.form.answer"
              placeholder="请输入题目解析（选填）"
          ></el-input>
        </el-form-item>

        <!-- 填空题答案 -->
        <el-form-item
            prop="answer"
            label="参考答案"
            v-if="data.form.questionTypeId === 4"
        >
          <el-input
              v-model="data.form.answer"
              placeholder="请输入参考答案"
          ></el-input>
        </el-form-item>

        <!-- 简答题答案 -->
        <el-form-item
            prop="answer"
            label="参考答案"
            v-if="data.form.questionTypeId === 5"
        >
          <el-input
              type="textarea"
              :rows="5"
              v-model="data.form.answer"
              placeholder="请输入参考答案"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button plain @click="data.formVisible = false">取 消</el-button>
          <el-button type="primary" plain @click="save">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox, ElLoading } from "element-plus";
import { Delete, Edit, Plus, InfoFilled, Menu } from "@element-plus/icons-vue";
import { useRouter, useRoute } from "vue-router";
import { getQuestionTypeStyle } from "@/utils/questionTypeStyles";

const router = useRouter();

const data = reactive({
  user: JSON.parse(localStorage.getItem("xm-user") || "{}"),
  formVisible: false,
  form: {
    courseId: "",
    questionStem: "",
    questionTypeId: "",
    level: "",
    answer: "",
    choiceList: [],
    questionList: [] // 复合题的子题目列表
  },
  singleAnswer: "", // 单选题/判断题的正确答案标识
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  questionStem: null,
  ids: [],
  courseData: [],
});

const viewQuestion = (id) => {
  router.push({
    path: '/manager/questionView',
    query: { id: id, fromPage: data.pageNum }
  })
}

const loadCourse = () => {
  // 教师只能选择自己教授的课程
  request
      .get("/course/selectByTeacher", {
        params: {
          teacherId: data.user.id,
        },
      })
      .then((res) => {
        if (res.code === "200") {
          data.courseData = res.data;
        } else {
          ElMessage.error(res.msg);
        }
      });
};

const load = () => {
  request
      .get("/question/selectPage", {
        params: {
          pageNum: data.pageNum,
          pageSize: data.pageSize,
          questionStem: data.questionStem,
        },
      })
      .then((res) => {
        if (res.code === "200") {
          data.tableData = res.data?.list || [];
          data.total = res.data?.total;
        } else {
          ElMessage.error(res.msg || '获取题库数据失败');
        }
      })
      .catch((err) => {
        ElMessage.error('获取题库数据失败');
      });
};

const handleTypeChange = () => {
  data.singleAnswer = ""; // 重置单选答案
  if (data.form.questionTypeId === 1 || data.form.questionTypeId === 2) {
    data.form.choiceList = [
      { identification: "A", content: "", flag: false },
      { identification: "B", content: "", flag: false },
      { identification: "C", content: "", flag: false },
      { identification: "D", content: "", flag: false },
    ];
    data.form.questionList = [];
  } else if (data.form.questionTypeId === 3) {
    // 判断题固定两个选项：√ 和 ×
    data.form.choiceList = [
      { identification: "√", content: "正确", flag: false },
      { identification: "×", content: "错误", flag: false },
    ];
    data.form.questionList = [];
  } else if (data.form.questionTypeId === 6) {
    // 复合题：初始化子题目列表
    data.form.choiceList = [];
    data.form.questionList = [];
  } else {
    data.form.choiceList = [];
    data.form.questionList = [];
  }
};

// 单选题/判断题更新选项的flag
const updateChoiceFlag = () => {
  if (data.form.choiceList) {
    data.form.choiceList.forEach(choice => {
      choice.flag = choice.identification === data.singleAnswer;
    });
  }
};

// 子题目单选题/判断题更新选项的flag
const updateSubChoiceFlag = (subIdx) => {
  const subQ = data.form.questionList[subIdx];
  if (subQ && subQ.choiceList) {
    subQ.choiceList.forEach(choice => {
      choice.flag = choice.identification === subQ.singleAnswer;
    });
  }
};

// 添加子题目
const addSubQuestion = () => {
  data.form.questionList.push({
    questionTypeId: 1, // 默认单选题
    questionStem: "",
    answer: "",
    singleAnswer: "", // 用于单选题/判断题
    choiceList: [
      { identification: "A", content: "", flag: false },
      { identification: "B", content: "", flag: false },
      { identification: "C", content: "", flag: false },
      { identification: "D", content: "", flag: false },
    ]
  });
};

// 删除子题目
const delSubQuestion = (idx) => {
  data.form.questionList.splice(idx, 1);
};

// 子题目题型变化
const handleSubTypeChange = (idx) => {
  const subQ = data.form.questionList[idx];
  subQ.singleAnswer = "";
  if (subQ.questionTypeId === 1 || subQ.questionTypeId === 2) {
    subQ.choiceList = [
      { identification: "A", content: "", flag: false },
      { identification: "B", content: "", flag: false },
      { identification: "C", content: "", flag: false },
      { identification: "D", content: "", flag: false },
    ];
  } else if (subQ.questionTypeId === 3) {
    subQ.choiceList = [
      { identification: "√", content: "正确", flag: false },
      { identification: "×", content: "错误", flag: false },
    ];
  } else {
    subQ.choiceList = [];
  }
};

// 添加子题目选项
const addSubChoice = (subIdx) => {
  const subQ = data.form.questionList[subIdx];
  const lastIdx = subQ.choiceList.length - 1;
  const lastChar = lastIdx >= 0 ? subQ.choiceList[lastIdx].identification : "A";
  const nextChar = String.fromCharCode(lastChar.charCodeAt(0) + 1);
  subQ.choiceList.push({
    identification: nextChar,
    content: "",
    flag: false
  });
};

// 删除子题目选项
const delSubChoice = (subIdx, choiceIdx) => {
  data.form.questionList[subIdx].choiceList.splice(choiceIdx, 1);
};

const addChoice = () => {
  const lastIdx = data.form.choiceList.length - 1;
  const lastChar = lastIdx >= 0 ? data.form.choiceList[lastIdx].identification : "A";
  const nextChar = String.fromCharCode(lastChar.charCodeAt(0) + 1);
  data.form.choiceList.push({
    identification: nextChar,
    content: "",
    flag: false
  });
};

const delChoice = (idx) => {
  data.form.choiceList.splice(idx, 1);
};

const handleAdd = () => {
  // 跳转到新增题目页面
  router.push('/manager/questionEdit')
};

const handleEdit = (row) => {
  // 跳转到编辑题目页面
  router.push({
    path: '/manager/questionEdit',
    query: { id: row.id }
  })
};

const initEditForm = (rowData) => {
  data.form = JSON.parse(JSON.stringify(rowData));
  if (!data.form.choiceList) {
    data.form.choiceList = [];
  }
  if (!data.form.questionList) {
    data.form.questionList = [];
  }
  // 初始化singleAnswer（从flag=true的选项中获取）
  data.singleAnswer = "";
  if (data.form.questionTypeId === 1 || data.form.questionTypeId === 3) {
    const correctChoice = data.form.choiceList.find(c => c.flag === true || c.flag === 1);
    if (correctChoice) {
      data.singleAnswer = correctChoice.identification;
    }
  }
  // 复合题：初始化子题目的singleAnswer
  if (data.form.questionTypeId === 6 && data.form.questionList) {
    data.form.questionList.forEach(subQ => {
      if (!subQ.choiceList) subQ.choiceList = [];
      if (subQ.questionTypeId === 1 || subQ.questionTypeId === 3) {
        const correctChoice = subQ.choiceList.find(c => c.flag === true || c.flag === 1);
        subQ.singleAnswer = correctChoice ? correctChoice.identification : "";
      }
    });
  }
  data.formVisible = true;
};

const add = () => {
  data.form.teacherId = data.user.id;
  request.post("/question/add", data.form).then((res) => {
    if (res.code === "200") {
      ElMessage.success("操作成功");
      data.formVisible = false;
      load();
    } else {
      ElMessage.error(res.msg);
    }
  });
};

const update = () => {
  request.put("/question/update", data.form).then((res) => {
    if (res.code === "200") {
      ElMessage.success("操作成功");
      data.formVisible = false;
      load();
    }
  });
};

const save = () => {
  data.form.id ? update() : add();
};

const del = (id) => {
  ElMessageBox.confirm("删除后数据无法恢复，您确定删除吗？", "删除确认", {
    type: "warning",
  })
      .then((res) => {
        request.delete("/question/delete/" + id).then((res) => {
          if (res.code === "200") {
            ElMessage.success("删除成功");
            load();
          } else {
            ElMessage.error(res.msg);
          }
        });
      })
      .catch((err) => {
        
      });
};

const delBatch = () => {
  if (!data.ids.length) {
    ElMessage.warning("请选择数据");
    return;
  }
  ElMessageBox.confirm("删除后数据无法恢复，您确定删除吗？", "删除确认", {
    type: "warning",
  })
      .then((res) => {
        request
            .delete("/question/delete/batch", { data: data.ids })
            .then((res) => {
              if (res.code === "200") {
                ElMessage.success("操作成功");
                load();
              } else {
                ElMessage.error(res.msg);
              }
            });
      })
      .catch((err) => {
        
      });
};

const handleExcelUpload = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  
  const loading = ElLoading.service({
    lock: true,
    text: '正在导入，请稍候...',
    background: 'rgba(0, 0, 0, 0.7)'
  })
  
  request.post('/question/importQuestions', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }).then(res => {
    loading.close()
    if (res.code === '200') {
      ElMessage.success(res.data || '导入成功')
      load()
    } else {
      ElMessage.error(res.msg)
    }
  }).catch(err => {
    loading.close()
    ElMessage.error('导入失败')
  })
  
  return false // 阻止自动上传
}

const handleSelectionChange = (rows) => {
  data.ids = rows.map((v) => v.id);
};

const reset = () => {
  data.questionStem = null;
  load();
};

const downloadTemplate = () => {
  const link = document.createElement('a')
  link.href = 'https://onlineexamnation-1392474432.cos.ap-guangzhou.myqcloud.com/example-excel/%E6%95%99%E5%B8%88%E5%AF%BC%E5%85%A5%E9%A2%98%E7%9B%AE%E6%A8%A1%E6%9D%BF.xlsx'
  link.download = '教师导入题目模板.xlsx'
  link.target = '_blank'
  link.click()
}

// 从路由恢复页码
const route = useRoute()
if (route.query.page) {
  data.pageNum = parseInt(route.query.page)
}

load();
loadCourse();
</script>

<style scoped>
:global(.el-tag--complex) {
  background-color: #9c27b0;
  color: #fff;
}
:global(.el-tag--complex:hover) {
  background-color: #7b1fa2;
}
</style>