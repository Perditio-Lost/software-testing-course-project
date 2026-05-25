import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/login' },
    {
      path: '/manager',
      component: () => import('@/views/Manager.vue'),
      children: [
        { path: 'home', meta: { name: '系统首页' }, component: () => import('@/views/manager/Home.vue'), },
        { path: 'admin', meta: { name: '管理员信息' }, component: () => import('@/views/manager/Admin.vue'), },
        { path: 'notice', meta: { name: '系统公告' }, component: () => import('@/views/manager/Notice.vue'), },
        { path: 'person', meta: { name: '个人资料' }, component: () => import('@/views/manager/Person.vue'), },
        { path: 'password', meta: { name: '修改密码' }, component: () => import('@/views/manager/Password.vue'), },
        { path: 'teacher', meta: { name: '教师信息' }, component: () => import('@/views/manager/Teacher.vue'), },
        { path: 'student', meta: { name: '学生信息' }, component: () => import('@/views/manager/Student.vue'), },
        { path: 'systemLog', meta: { name: '系统日志' }, component: () => import('@/views/manager/SystemLog.vue'), },
        { path: 'test', meta: { name: '考试安排' }, component: () => import('@/views/manager/ExamPlan.vue'), },
        { path: 'course', meta: { name: '课程信息' }, component: () => import('@/views/manager/Course.vue'), },
        { path: 'clazz', meta: { name: '班级管理' }, component: () => import('@/views/manager/Clazz.vue'), },
        { path: 'question', meta: { name: '题库信息' }, component: () => import('@/views/manager/Question.vue'), },
        { path: 'questionEdit', meta: { name: '出题' }, component: () => import('@/views/manager/QuestionEdit.vue'), },
        { path: 'questionView', meta: { name: '题目详情' }, component: () => import('@/views/manager/QuestionView.vue'), },
        { path: 'share', meta: { name: '交流分享' }, component: () => import('@/views/manager/Share.vue'), },
        { path: 'testPaper', meta: { name: '试卷管理' }, component: () => import('@/views/manager/TestPaper.vue'), },
        { path: 'testPaperEdit', meta: { name: '出卷' }, component: () => import('@/views/manager/TestPaperEdit.vue'), },
        { path: 'testPaperView', meta: { name: '试卷详情' }, component: () => import('@/views/manager/TestPaperView.vue'), },
        { path: 'score', meta: { name: '成绩管理' }, component: () => import('@/views/manager/Score.vue'), },
        { path: 'scoreDetail', meta: { name: '阅卷详情' }, component: () => import('@/views/manager/ScoreDetail.vue'), },
        { path: 'questionGrading', meta: { name: '按题批阅' }, component: () => import('@/views/manager/QuestionGrading.vue'), },
        { path: 'abnormalExam', meta: { name: '异常试卷' }, component: () => import('@/views/manager/AbnormalExam.vue'), },
        { path: 'proctoring', meta: { name: '监考大屏' }, component: () => import('@/views/manager/Proctoring.vue'), },
      ]
    },
    {
      path: '/front',
      component: () => import('@/views/Front.vue'),
      children: [
        { path: 'home', component: () => import('@/views/front/Home.vue'), },
        { path: 'person', component: () => import('@/views/front/Person.vue'), },
        { path: 'myShare', component: () => import('@/views/front/MyShare.vue'), },
        { path: 'forum', component: () => import('@/views/front/Forum.vue'), },
        { path: 'forumDetail', component: () => import('@/views/front/ForumDetail.vue'), },
        { path: 'course', component: () => import('@/views/front/Course.vue'), },
        { path: 'exam', component: () => import('@/views/front/Exam.vue'), },
        { path: 'clazzExam', component: () => import('@/views/front/ClazzExam.vue'), },
        { path: 'examPreparation', component: () => import('@/views/front/ExamPreparation.vue'), },
        { path: 'testPaper', component: () => import('@/views/front/TestPaper.vue'), },
        { path: 'testPaperView', component: () => import('@/views/front/TestPaperView.vue'), },
        { path: 'score', component: () => import('@/views/front/Score.vue'), },
      ]
    },
    { path: '/login', component: () => import('@/views/Login.vue') },
    { path: '/register', component: () => import('@/views/Register.vue') },
    { path: '/forgot-password', component: () => import('@/views/ForgotPassword.vue') },
    { path: '/mobile-camera', component: () => import('@/views/front/MobileCamera.vue') },
    { path: '/404', component: () => import('@/views/404.vue') },
    { path: '/:pathMatch(.*)', redirect: '/404' }
  ]
})

export default router
