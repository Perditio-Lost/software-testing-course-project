-- OnlineExaminationSystem 数据库完整初始化脚本（含真实场景模拟数据）
-- 生成日期：2026-05-09
-- 数据库：xm_examination；字符集：utf8mb4

CREATE DATABASE IF NOT EXISTS xm_examination DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xm_examination;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 管理员表
DROP TABLE IF EXISTS admin;
CREATE TABLE admin(
  id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(255) COMMENT '账号',
  password VARCHAR(255) COMMENT '密码',
  name VARCHAR(255) COMMENT '姓名',
  role VARCHAR(255) COMMENT '角色',
  phone VARCHAR(255) COMMENT '电话',
  email VARCHAR(255) COMMENT '邮箱'
) COMMENT '管理员表';

-- 学生信息表
DROP TABLE IF EXISTS student;
CREATE TABLE student(
  id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(255) COMMENT '账号',
  password VARCHAR(255) COMMENT '密码',
  name VARCHAR(255) COMMENT '姓名',
  role VARCHAR(255) COMMENT '角色',
  phone VARCHAR(255) COMMENT '电话',
  email VARCHAR(255) COMMENT '邮箱',
  status VARCHAR(255) COMMENT '状态'
) COMMENT '学生信息表';

-- 教师信息表
DROP TABLE IF EXISTS teacher;
CREATE TABLE teacher(
  id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(255) COMMENT '账号',
  password VARCHAR(255) COMMENT '密码',
  name VARCHAR(255) COMMENT '姓名',
  role VARCHAR(255) COMMENT '角色',
  phone VARCHAR(255) COMMENT '电话',
  email VARCHAR(255) COMMENT '邮箱'
) COMMENT '教师信息表';

-- 帖子信息表
DROP TABLE IF EXISTS article;
CREATE TABLE article(
  id INT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) COMMENT '帖子标题',
  content LONGTEXT COMMENT '帖子内容',
  time DATETIME COMMENT '发布时间',
  student_id INT COMMENT '发布的学生ID',
  FOREIGN KEY (student_id) REFERENCES student(id)
) COMMENT '帖子信息表';

-- 课程信息表
DROP TABLE IF EXISTS course;
CREATE TABLE course(
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) COMMENT '课程名称',
  score INT COMMENT '课程学分',
  admin_id INT COMMENT '管理该课程的管理员ID',
  FOREIGN KEY (admin_id) REFERENCES admin(id)
) COMMENT '课程信息表';

-- 课程班级信息表
DROP TABLE IF EXISTS clazz;
CREATE TABLE clazz(
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) COMMENT '课程班级名称',
  course_id INT COMMENT '所属课程ID',
  teacher_id INT COMMENT '授课教师ID',
  FOREIGN KEY (course_id) REFERENCES course(id),
  FOREIGN KEY (teacher_id) REFERENCES teacher(id)
) COMMENT '课程班级信息表';

-- 学生所属课程班级信息表
DROP TABLE IF EXISTS student_clazz;
CREATE TABLE student_clazz(
  id INT PRIMARY KEY AUTO_INCREMENT,
  student_id INT COMMENT '学生ID',
  clazz_id INT COMMENT '课程班级ID',
  FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
  FOREIGN KEY (clazz_id) REFERENCES clazz(id) ON DELETE CASCADE
) COMMENT '学生所属课程班级信息表';

-- 系统公告表
DROP TABLE IF EXISTS notice;
CREATE TABLE notice(
  id INT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) COMMENT '公告标题',
  content TEXT NULL COMMENT '公告内容',
  time DATETIME COMMENT '发布时间',
  admin_id INT COMMENT '发布的管理员ID',
  FOREIGN KEY (admin_id) REFERENCES admin(id)
) COMMENT '系统公告表';

-- 题型表
DROP TABLE IF EXISTS question_type;
CREATE TABLE question_type(
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) COMMENT "题型名称",
  variety ENUM('choice','text','composite') COMMENT "题型种类"
) COMMENT "题型表";

-- 题目表
DROP TABLE IF EXISTS question;
CREATE TABLE question(
  id INT PRIMARY KEY AUTO_INCREMENT,
  question_type_id INT COMMENT "所属题型ID",
  course_id INT COMMENT '所属课程ID',
  teacher_id INT COMMENT '出题教师ID',
  level DECIMAL(3,2) COMMENT "难度系数",
  question_stem TEXT COMMENT "题干",
  answer TEXT COMMENT "答案",
  flag TINYINT(1) COMMENT "是否可选作独立题目",
  FOREIGN KEY(question_type_id) REFERENCES question_type(id),
  FOREIGN KEY(course_id) REFERENCES course(id),
  FOREIGN KEY(teacher_id) REFERENCES teacher(id)
) COMMENT "题目表";

-- 选择题选项表
DROP TABLE IF EXISTS choice;
CREATE TABLE choice(
  id INT PRIMARY KEY AUTO_INCREMENT,
  question_id INT COMMENT "所属题目id",
  identification VARCHAR(1) COMMENT "选项标识",
  content TEXT COMMENT "选项内容",
  sequence INT COMMENT "选项顺序",
  flag TINYINT(1) COMMENT "是否为正确答案",
  FOREIGN KEY(question_id) REFERENCES question(id) ON DELETE CASCADE
) COMMENT "选择题选项表";

-- 复合题子题目表
DROP TABLE IF EXISTS composite;
CREATE TABLE composite(
  id INT PRIMARY KEY AUTO_INCREMENT,
  question_father_id INT COMMENT "父题目id",
  question_son_id INT COMMENT "子题目id",
  sequence INT COMMENT "子题目顺序",
  FOREIGN KEY(question_father_id) REFERENCES question(id) ON DELETE CASCADE,
  FOREIGN KEY(question_son_id) REFERENCES question(id) ON DELETE CASCADE,
  UNIQUE KEY(question_father_id,question_son_id)
) COMMENT "复合题子题目表";

-- 试卷表
DROP TABLE IF EXISTS test_paper;
CREATE TABLE test_paper(
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) COMMENT '试卷名称',
  course_id INT COMMENT '课程ID',
  teacher_id INT COMMENT '教师ID',
  type VARCHAR(255) COMMENT '组卷类型',
  level DECIMAL(3,2) COMMENT "难度系数",
  FOREIGN KEY(course_id) REFERENCES course(id),
  FOREIGN KEY(teacher_id) REFERENCES teacher(id)
) COMMENT '试卷信息表';

-- 试卷模板表
DROP TABLE IF EXISTS test_paper_template;
CREATE TABLE test_paper_template(
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) COMMENT '模板名称',
  course_id INT COMMENT '课程ID',
  teacher_id INT COMMENT '教师ID',
  choice_num INT DEFAULT 0 COMMENT '单选题数量',
  multi_choice_num INT DEFAULT 0 COMMENT '多选题数量',
  fill_in_num INT DEFAULT 0 COMMENT '填空题数量',
  check_num INT DEFAULT 0 COMMENT '判断题数量',
  text_num INT DEFAULT 0 COMMENT '简答题数量',
  composite_num INT DEFAULT 0 COMMENT '复合题数量',
  choice_score INT DEFAULT 10 COMMENT '单选题每题分值',
  multi_choice_score INT DEFAULT 10 COMMENT '多选题每题分值',
  fill_in_score INT DEFAULT 10 COMMENT '填空题每题分值',
  check_score INT DEFAULT 10 COMMENT '判断题每题分值',
  text_score INT DEFAULT 10 COMMENT '简答题每题分值',
  composite_score INT DEFAULT 10 COMMENT '复合题每题分值',
  FOREIGN KEY(course_id) REFERENCES course(id),
  FOREIGN KEY(teacher_id) REFERENCES teacher(id)
) COMMENT '试卷模板表';

-- 试卷题目表
DROP TABLE IF EXISTS test_paper_question;
CREATE TABLE test_paper_question(
  id INT PRIMARY KEY AUTO_INCREMENT,
  test_paper_id INT COMMENT '考试试卷ID',
  question_id INT COMMENT '题目ID',
  sequence INT COMMENT '题目顺序',
  score INT COMMENT '题目分值',
  type ENUM('common','father','son') COMMENT '题目类型',
  FOREIGN KEY(test_paper_id) REFERENCES test_paper(id),
  FOREIGN KEY(question_id) REFERENCES question(id)
) COMMENT '试卷题目表';

-- 考试信息表
DROP TABLE IF EXISTS test;
CREATE TABLE test(
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) COMMENT '考试名称',
  content TEXT COMMENT '考试安排内容',
  test_paper_id INT COMMENT '考试试卷ID',
  start DATETIME COMMENT '开始时间',
  end DATETIME COMMENT '结束时间',
  duration INT COMMENT '考试时长',
  release_time DATETIME COMMENT '发布时间',
  weather_check TINYINT(1) COMMENT '学生是否可以查看试卷',
  weather_question_unordered TINYINT(1) COMMENT '试题是否乱序',
  weather_choice_unordered TINYINT(1) COMMENT '选择题选项是否乱序',
  weather_copy TINYINT(1) COMMENT '是否开启防复制粘贴',
  weather_face TINYINT(1) COMMENT '是否开启人脸识别',
  weather_dual_camera TINYINT(1) COMMENT '是否开启双机位监考',
  weather_screen_switch TINYINT(1) COMMENT '是否开启切屏监控',
  FOREIGN KEY(test_paper_id) REFERENCES test_paper(id)
) COMMENT '考试信息表';

-- 考试班级关联表
DROP TABLE IF EXISTS test_clazz;
CREATE TABLE test_clazz(
  id INT PRIMARY KEY AUTO_INCREMENT,
  test_id INT COMMENT '考试ID',
  clazz_id INT COMMENT '班级ID',
  FOREIGN KEY(test_id) REFERENCES test(id) ON DELETE CASCADE,
  FOREIGN KEY(clazz_id) REFERENCES clazz(id) ON DELETE CASCADE,
  UNIQUE KEY(test_id, clazz_id)
) COMMENT '考试班级关联表';

-- 学生成绩表
DROP TABLE IF EXISTS score;
CREATE TABLE score(
  id INT PRIMARY KEY AUTO_INCREMENT,
  student_id INT COMMENT '学生ID',
  test_id INT COMMENT '考试ID',
  score INT COMMENT '成绩得分',
  status VARCHAR(255) COMMENT '状态',
  cheat TINYINT(1) COMMENT '是否作弊',
  FOREIGN KEY(student_id) REFERENCES student(id),
  FOREIGN KEY(test_id) REFERENCES test(id)
) COMMENT '学生成绩表';

-- 学生作答表
DROP TABLE IF EXISTS answer;
CREATE TABLE answer(
  id INT PRIMARY KEY AUTO_INCREMENT,
  score_id INT COMMENT '学生总成绩ID',
  test_paper_question_id INT COMMENT '试卷题目ID',
  answer LONGTEXT COMMENT '回答答案',
  score INT COMMENT '作答分数',
  status VARCHAR(255) COMMENT '状态',
  FOREIGN KEY(score_id) REFERENCES score(id),
  FOREIGN KEY(test_paper_question_id) REFERENCES test_paper_question(id)
) COMMENT '学生作答表';

-- 考试答题草稿表
DROP TABLE IF EXISTS exam_draft;
CREATE TABLE exam_draft(
  id INT PRIMARY KEY AUTO_INCREMENT,
  student_id INT COMMENT '学生ID',
  test_id INT COMMENT '考试ID',
  draft_data LONGTEXT COMMENT '草稿数据（JSON格式）',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY(student_id) REFERENCES student(id) ON DELETE CASCADE,
  FOREIGN KEY(test_id) REFERENCES test(id) ON DELETE CASCADE,
  UNIQUE KEY idx_student_test (student_id, test_id)
) COMMENT '考试答题草稿表';

-- 系统日志表
DROP TABLE IF EXISTS system_log;
CREATE TABLE system_log(
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT COMMENT '用户ID',
  role VARCHAR(50) COMMENT '用户角色',
  operation VARCHAR(255) COMMENT '操作内容',
  method VARCHAR(255) COMMENT '请求方法',
  params TEXT COMMENT '请求参数',
  ip VARCHAR(50) COMMENT 'IP地址',
  location VARCHAR(255) COMMENT 'IP所在地',
  time DATETIME COMMENT '操作时间',
  duration BIGINT COMMENT '执行时长(毫秒)'
) COMMENT '系统日志表';

-- 用户表索引（登录查询）
CREATE UNIQUE INDEX idx_admin_username ON admin(username);
CREATE UNIQUE INDEX idx_student_username ON student(username);
CREATE UNIQUE INDEX idx_teacher_username ON teacher(username);

-- 题目表索引
CREATE INDEX idx_question_type ON question(question_type_id);
CREATE INDEX idx_question_course ON question(course_id);
CREATE INDEX idx_question_teacher ON question(teacher_id);
CREATE INDEX idx_question_course_type ON question(course_id, question_type_id);
CREATE INDEX idx_question_course_type_level ON question(course_id, question_type_id, level);
CREATE INDEX idx_question_flag ON question(flag);

-- 选择题选项表索引
CREATE INDEX idx_choice_question ON choice(question_id);

-- 复合题索引
CREATE INDEX idx_composite_father ON composite(question_father_id);
CREATE INDEX idx_composite_son ON composite(question_son_id);

-- 试卷表索引
CREATE INDEX idx_test_paper_course ON test_paper(course_id);
CREATE INDEX idx_test_paper_teacher ON test_paper(teacher_id);
CREATE INDEX idx_test_paper_course_teacher ON test_paper(course_id, teacher_id);

-- 试卷题目表索引
CREATE INDEX idx_tpq_paper ON test_paper_question(test_paper_id);
CREATE INDEX idx_tpq_question ON test_paper_question(question_id);
CREATE INDEX idx_tpq_paper_seq ON test_paper_question(test_paper_id, sequence);

-- 考试表索引
CREATE INDEX idx_test_paper ON test(test_paper_id);
CREATE INDEX idx_test_time ON test(start, end);
CREATE INDEX idx_test_release ON test(release_time);

-- 成绩表索引（高频查询）
CREATE INDEX idx_score_student ON score(student_id);
CREATE INDEX idx_score_test ON score(test_id);
CREATE UNIQUE INDEX idx_score_student_test ON score(student_id, test_id);
CREATE INDEX idx_score_status ON score(status);

-- 作答表索引
CREATE INDEX idx_answer_score ON answer(score_id);
CREATE INDEX idx_answer_tpq ON answer(test_paper_question_id);
CREATE INDEX idx_answer_status ON answer(status);
CREATE INDEX idx_answer_score_tpq ON answer(score_id, test_paper_question_id);

-- 班级表索引
CREATE INDEX idx_clazz_course ON clazz(course_id);
CREATE INDEX idx_clazz_teacher ON clazz(teacher_id);

-- 学生班级关联表索引
CREATE INDEX idx_student_clazz_student ON student_clazz(student_id);
CREATE INDEX idx_student_clazz_clazz ON student_clazz(clazz_id);

-- 考试班级关联表索引
CREATE INDEX idx_test_clazz_test ON test_clazz(test_id);
CREATE INDEX idx_test_clazz_clazz ON test_clazz(clazz_id);

-- 帖子表索引
CREATE INDEX idx_article_student ON article(student_id);
CREATE INDEX idx_article_time ON article(time);
CREATE INDEX idx_article_title ON article(title(100));

-- 公告表索引
CREATE INDEX idx_notice_time ON notice(time);
CREATE INDEX idx_notice_admin ON notice(admin_id);

-- 课程表索引
CREATE INDEX idx_course_admin ON course(admin_id);
CREATE INDEX idx_course_name ON course(name(50));

-- 系统日志表索引
CREATE INDEX idx_log_role ON system_log(role);
CREATE INDEX idx_log_user_id ON system_log(user_id);
CREATE INDEX idx_log_time ON system_log(time DESC);
CREATE INDEX idx_log_role_time ON system_log(role, time DESC);

INSERT INTO admin (username, password, name, role, phone, email) VALUES ('admin', 'admin', '管理员', 'ADMIN', '18899990011', 'admin2@xm.com');
INSERT INTO student (username, password, name, role, phone, email, status) VALUES ('zhangsan', '123456', '张三', 'STUDENT', '18899990000', 'zhangsan@xm.com', '审核通过'),
('lisi', '123456', '李四', 'STUDENT', '18899995555', 'lisi@xm.com', '审核通过'),
('wangwu', '123456', '王五', 'STUDENT', '18844445555', 'wangwu@xm.com', '审核通过'),
('zhaoliu', '123456', 'zhaoliu', 'STUDENT', NULL, NULL, '待审核');
INSERT INTO teacher (username, password, name, role, phone, email) VALUES ('zhang', '123456', '张老师', 'TEACHER', '18800001111', 'zhang@xm.com'),
('li', '123456', '李老师', 'TEACHER', '18877776666', 'li@xm.com'),
('zhao', '123456', 'zhao', 'TEACHER', NULL, NULL);
INSERT INTO notice (title, content, time, admin_id) VALUES
('2025学年开学公告', '各位同学：2025学年将于9月1日正式开学，请按时报到注册。', '2025-08-20 09:00:00', 1),
('期中考试安排通知', '本学期期中考试将于10月20日-10月25日进行，请各位同学做好复习准备。', '2025-09-30 14:30:00', 1),
('课程调整通知', '因教师培训，10月10日的Java程序设计课程改为线上授课。', '2025-10-05 16:00:00', 1);
INSERT INTO course (name, score, admin_id) VALUES
('Java程序设计', 4, 1),
('数据库原理与应用', 3, 1),
('Python编程基础', 2, 1),
('Web前端开发', 3, 1),
('数据结构与算法', 3, 1),
('操作系统原理', 3, 1),
('计算机网络', 3, 1),
('软件工程', 2, 1),
('大学英语', 3, 1);
INSERT INTO test_paper_template (name, course_id, teacher_id, choice_num, multi_choice_num, fill_in_num, check_num, text_num, composite_num, choice_score, multi_choice_score, fill_in_score, check_score, text_score, composite_score) VALUES
('Java程序设计单元测验模板', 1, 1, 2, 1, 1, 1, 1, 1, 10, 10, 10, 10, 10, 40),
('数据库原理章节测试模板', 2, 1, 1, 2, 1, 1, 2, 2, 5, 10, 10, 5, 10, 20),
('大学英语基础小测模板', 9, 1, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 25);
INSERT INTO clazz (name, course_id) VALUES
('Java程序设计默认班级', 1),
('数据库原理与应用默认班级', 2),
('Python编程基础默认班级', 3),
('Web前端开发默认班级', 4),
('数据结构与算法默认班级', 5),
('操作系统原理默认班级', 6),
('计算机网络默认班级', 7),
('软件工程默认班级', 8),
('大学英语默认班级', 9);
INSERT INTO article (title, content, time, student_id) VALUES
('Java学习笔记分享', '本文整理了Java面向对象的核心知识点，包括类、对象、继承、多态等。', '2025-09-05 10:20:00', 1),
('数据库刷题总结', '分享一些数据库面试常考的SQL语句，包括联表查询、分组统计等。', '2025-09-10 15:30:00', 1),
('Python爬虫入门', '使用requests和BeautifulSoup实现简单的网页爬虫，附代码示例。', '2025-09-15 09:10:00', 1),
('Web前端学习心得', '记录了HTML/CSS/JS的学习过程，以及遇到的问题和解决方法。', '2025-09-20 16:40:00', 1);
INSERT INTO question_type (name, variety) VALUES
('单选', 'choice'),
('多选', 'choice'),
('判断', 'choice'),
('填空', 'text'),
('名词解析', 'text'),
('论述', 'text'),
('计算', 'text'),
('程序', 'text'),
('资料', 'composite'),
('完形填空', 'composite'),
('阅读理解', 'composite'),
('综合', 'composite');
INSERT INTO question (question_type_id, course_id, teacher_id, level, question_stem, answer, flag) VALUES
(1, 1, 1, 0.50, 'Java中哪个关键字用于定义类？', 'class', 1),
(1, 1, 1, 0.60, '以下哪个不是Java的基本数据类型？', 'String', 1),
(1, 2, 1, 0.55, '下列哪个SQL语句用于查询数据？', 'SELECT', 1),
(1, 5, 1, 0.65, '以下哪种数据结构遵循先进先出原则？', '队列', 1),
(1, 6, 1, 0.70, 'CPU的主要功能是什么？', '运算和控制', 1),
(1, 7, 1, 0.60, 'OSI模型共有几层？', '7层', 1),
(2, 2, 1, 0.70, 'SQL中常用的聚合函数有哪些？', 'COUNT,SUM,AVG,MAX,MIN', 1),
(2, 2, 1, 0.80, '以下哪些是关系数据库的基本操作？', '选择,投影,连接', 1),
(2, 1, 1, 0.75, 'Java的三大特性包括哪些？', '封装,继承,多态', 1),
(2, 5, 1, 0.72, '以下哪些是线性数据结构？', '数组,链表,栈,队列', 1),
(2, 6, 1, 0.68, '进程的基本状态包括哪些？', '就绪,运行,阻塞', 1),
(2, 4, 1, 0.65, '以下哪些是前端框架？', 'Vue,React,Angular', 1),
(3, 1, 1, 0.40, 'Java是一种编译型语言', '错误', 1),
(3, 2, 1, 0.35, '主键可以为空值', '错误', 1),
(3, 3, 1, 0.45, 'Python是强类型语言', '正确', 1),
(3, 5, 1, 0.50, '栈是先进先出的数据结构', '错误', 1),
(3, 6, 1, 0.42, '死锁的四个必要条件包括互斥条件', '正确', 1),
(3, 7, 1, 0.48, 'TCP协议是无连接的协议', '错误', 1),
(4, 1, 1, 0.55, 'Java的入口方法是____方法', 'main', 1),
(4, 2, 1, 0.60, 'SQL中用于插入数据的关键字是____', 'INSERT', 1),
(4, 3, 1, 0.58, 'Python中定义函数使用____关键字', 'def', 1),
(4, 5, 1, 0.65, '二叉树的遍历方式有前序、中序和____', '后序', 1),
(4, 6, 1, 0.70, '进程间通信的方式包括管道、消息队列和____', '共享内存', 1),
(4, 4, 1, 0.52, 'CSS中用于设置文字颜色的属性是____', 'color', 1),
(5, 3, 1, 0.75, '简述Python中列表和元组的区别。', '列表是可变的（mutable），元组是不可变的（immutable）；列表用[]定义，元组用()定义。', 1),
(5, 3, 1, 0.85, '什么是装饰器？简述其作用。', '装饰器是Python的一种语法糖，用于在不修改原函数代码的情况下，为函数添加额外功能。', 1),
(5, 1, 1, 0.72, '解释Java中的多态性概念。', '多态是指同一个方法调用可以有不同的执行结果，主要通过方法重载和方法重写实现。', 1),
(5, 5, 1, 0.80, '什么是哈希表？简述其特点。', '哈希表是一种基于键值对的数据结构，通过哈希函数快速查找、插入和删除数据，平均时间复杂度为O(1)。', 1),
(5, 7, 1, 0.78, '解释TCP三次握手过程。', 'TCP三次握手：1) 客户端发送SYN；2) 服务器回复SYN+ACK；3) 客户端发送ACK。确保双方都能收发数据。', 1),
(5, 8, 1, 0.82, '什么是软件测试？其目的是什么？', '软件测试是为了发现软件缺陷，验证软件是否满足需求，确保软件质量的过程。', 1),
(6, 2, 1, 0.90, '论述关系数据库的ACID特性及其重要性。', 'ACID包括原子性、一致性、隔离性和持久性。原子性确保事务全部完成或全部回滚；一致性保证数据库从一个一致状态转换到另一个一致状态；隔离性防止并发事务相互干扰；持久性确保已提交事务的修改永久保存。这些特性对数据库的可靠性和正确性至关重要。', 1),
(6, 3, 1, 0.88, '论述面向对象编程的优缺点。', '优点：代码复用性高、可维护性好、符合人类思维习惯、易于扩展。缺点：学习曲线较陡、性能可能不如面向过程、设计不当会导致复杂度增加。', 1),
(6, 5, 1, 0.85, '比较分析栈和队列的应用场景。', '栈适用于函数调用、表达式求值、括号匹配等需要后进先出的场景；队列适用于任务调度、广度优先搜索、消息队列等需要先进先出的场景。', 1),
(6, 6, 1, 0.92, '论述进程和线程的区别及各自的优势。', '进程是资源分配的基本单位，线程是CPU调度的基本单位。进程间独立性强、安全性高但开销大；线程共享资源、通信方便但需要同步机制。', 1),
(6, 8, 1, 0.87, '论述敏捷开发的核心思想和实践方法。', '敏捷开发强调快速迭代、持续交付、客户协作、响应变化。实践包括Scrum、看板、测试驱动开发、持续集成等，通过短周期迭代快速响应需求变化。', 1),
(6, 4, 1, 0.84, '论述前端性能优化的策略和方法。', '性能优化包括：1)减少HTTP请求；2)压缩资源文件；3)使用CDN；4)懒加载；5)代码分割；6)缓存策略；7)优化渲染性能。综合运用这些方法可显著提升用户体验。', 1),
(7, 5, 1, 0.75, '给定数组[3,1,4,1,5,9,2,6]，使用冒泡排序算法排序，写出第一轮排序后的结果。', '[1,3,1,4,5,2,6,9]', 1),
(7, 5, 1, 0.80, '计算二叉树的最大深度：根节点为A，左子树B有左孩子D，右子树C有右孩子E和F。', '3', 1),
(7, 2, 1, 0.72, '某数据库表有1000条记录，执行DELETE删除了200条，再INSERT了50条，此时表中有多少条记录？', '850', 1),
(7, 7, 1, 0.78, '某网络子网掩码为255.255.255.0，计算该子网最多可容纳多少台主机？', '254', 1),
(7, 6, 1, 0.85, '若进程P0需要10个资源，系统现有15个资源，P0已分配7个，还需要多少资源才能完成？', '3', 1),
(7, 1, 1, 0.70, '计算表达式 (5 + 3) * 2 - 4 / 2 的结果。', '14', 1),
(8, 1, 1, 0.88, '编写Java方法实现两个整数的交换（不使用第三个变量）。', 'public static void swap(int a, int b) {\n    a = a + b;\n    b = a - b;\n    a = a - b;\n}', 1),
(8, 3, 1, 0.85, '编写Python函数判断一个字符串是否为回文。', 'def is_palindrome(s):\n    return s == s[::-1]', 1),
(8, 1, 1, 0.90, '编写Java代码实现单例模式（懒汉式）。', 'public class Singleton {\n    private static Singleton instance;\n    private Singleton() {}\n    public static synchronized Singleton getInstance() {\n        if (instance == null) {\n            instance = new Singleton();\n        }\n        return instance;\n    }\n}', 1),
(8, 5, 1, 0.92, '编写C语言函数实现链表反转。', 'struct Node* reverse(struct Node* head) {\n    struct Node *prev = NULL, *curr = head, *next;\n    while (curr != NULL) {\n        next = curr->next;\n        curr->next = prev;\n        prev = curr;\n        curr = next;\n    }\n    return prev;\n}', 1),
(8, 4, 1, 0.82, '编写JavaScript函数实现数组去重。', 'function unique(arr) {\n    return [...new Set(arr)];\n}', 1),
(8, 3, 1, 0.87, '编写Python装饰器记录函数执行时间。', 'import time\ndef timer(func):\n    def wrapper(*args, **kwargs):\n        start = time.time()\n        result = func(*args, **kwargs)\n        end = time.time()\n        print(f"Time: {end-start}s")\n        return result\n    return wrapper', 1),
(9, 2, 1, 0.92, '某公司需要设计一个电商数据库，包含用户、商品、订单等信息。', NULL, 1),
(9, 2, 1, 0.90, '某学校需要设计教务管理系统数据库，包含学生、课程、成绩等信息。', NULL, 1),
(9, 5, 1, 0.88, '某社交平台需要设计好友关系数据库，包含用户、好友、消息等信息。', NULL, 1),
(9, 7, 1, 0.89, '某公司网络拓扑包含路由器、交换机、服务器和终端设备。', NULL, 1),
(9, 6, 1, 0.91, '某操作系统采用银行家算法进行死锁避免，系统有3类资源A、B、C。', NULL, 1),
(12, 2, 1, 0.95, '综合运用数据库知识，设计一个电商系统的数据库，要求：1. 设计ER图 2. 转换为关系模式 3. 编写常用查询SQL 4. 创建必要的索引', NULL, 1),
(12, 1, 1, 0.94, '综合运用Java知识，设计一个图书管理系统，要求：1. 使用面向对象设计 2. 实现CRUD操作 3. 使用集合框架 4. 异常处理', NULL, 1),
(12, 5, 1, 0.93, '综合运用数据结构知识，实现一个LRU缓存，要求：1. 使用哈希表+双向链表 2. 实现get和put方法 3. 时间复杂度O(1)', NULL, 1),
(12, 6, 1, 0.96, '综合运用操作系统知识，分析一个生产者-消费者问题，要求：1. 设计同步机制 2. 避免死锁 3. 代码实现 4. 性能分析', NULL, 1),
(12, 8, 1, 0.92, '综合运用软件工程知识，设计一个在线考试系统，要求：1. 需求分析 2. 系统设计 3. 模块划分 4. 测试用例', NULL, 1),
(10, 9, 1, 0.75, 'Fill in the blanks: The Internet has __1__ our lives in many ways. We can now __2__ with people around the world instantly. Online shopping has made it __3__ to buy products without leaving home. However, we must be __4__ about online security and protect our personal information.', NULL, 1),
(10, 9, 1, 0.78, 'Complete the passage: Climate change is one of the biggest __1__ facing humanity. Scientists warn that we must __2__ action soon to reduce carbon emissions. Many countries are investing in __3__ energy sources like solar and wind power. __4__ efforts are needed to protect our planet.', NULL, 1),
(11, 9, 1, 0.80, 'Read the following passage and answer the questions: Climate change is one of the biggest challenges facing humanity today. Rising temperatures are causing ice caps to melt, sea levels to rise, and weather patterns to become more extreme. Scientists warn that if we do not take action soon, the consequences could be catastrophic. Many countries are now working together to reduce carbon emissions and develop renewable energy sources.', NULL, 1),
(11, 9, 1, 0.82, 'Read the passage about artificial intelligence: Artificial Intelligence (AI) is transforming many industries. From healthcare to finance, AI systems are being used to analyze data, make predictions, and automate tasks. While AI offers many benefits, it also raises important questions about privacy, job displacement, and ethical use of technology. As AI continues to develop, society must carefully consider how to balance innovation with responsibility.', NULL, 1),
(8, 2, 1, 0.88, '设计用户表、商品表、订单表的表结构。', 'CREATE TABLE users (id INT PRIMARY KEY, username VARCHAR(50), email VARCHAR(100));\nCREATE TABLE products (id INT PRIMARY KEY, name VARCHAR(100), price DECIMAL(10,2));\nCREATE TABLE orders (id INT PRIMARY KEY, user_id INT, product_id INT, quantity INT, order_date DATETIME);', 0),
(8, 2, 1, 0.91, '编写查询每个用户的订单总额的SQL语句。', 'SELECT u.username, SUM(o.quantity * p.price) AS total\nFROM users u\nJOIN orders o ON u.id = o.user_id\nJOIN products p ON o.product_id = p.id\nGROUP BY u.id, u.username;', 0),
(8, 2, 1, 0.87, '设计学生表、课程表、选课表的表结构。', 'CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(50), major VARCHAR(50));\nCREATE TABLE courses (id INT PRIMARY KEY, name VARCHAR(100), credits INT);\nCREATE TABLE enrollments (id INT PRIMARY KEY, student_id INT, course_id INT, grade DECIMAL(5,2));', 0),
(4, 2, 1, 0.85, '如何查询每个学生的平均成绩？', 'SELECT student_id, AVG(grade) FROM enrollments GROUP BY student_id;', 0),
(8, 5, 1, 0.86, '设计用户表和好友关系表。', 'CREATE TABLE users (id INT PRIMARY KEY, username VARCHAR(50));\nCREATE TABLE friendships (id INT PRIMARY KEY, user_id1 INT, user_id2 INT, status VARCHAR(20));', 0),
(8, 5, 1, 0.89, '编写查询某用户所有好友的SQL。', 'SELECT u.username FROM users u\nJOIN friendships f ON (u.id = f.user_id2 AND f.user_id1 = ?)\n   OR (u.id = f.user_id1 AND f.user_id2 = ?)\nWHERE f.status = ''accepted'';', 0),
(4, 7, 1, 0.85, '如果路由器R1连接3个子网，需要几个网络接口？', '3', 0),
(5, 7, 1, 0.87, '简述路由器和交换机的区别。', '路由器工作在网络层，用于不同网络间通信；交换机工作在数据链路层，用于同一网络内设备通信。', 0),
(7, 6, 1, 0.90, '如果进程P0已持有资源A:2个, B:1个, C:0个，还需要A:1个, B:2个, C:2个，当前可用资源为A:3个, B:3个, C:2个，能否分配？', '能，可以满足P0的需求。', 0),
(5, 6, 1, 0.92, '解释银行家算法的安全性检查原理。', '通过模拟资源分配，找到一个安全序列，使所有进程都能顺利完成，避免死锁。', 0),
(8, 2, 1, 0.93, '设计ER图并转换为关系模式。', '实体：用户(User)、商品(Product)、订单(Order)\n关系：用户-订单(1:N)、订单-商品(M:N)\n关系模式：Users, Products, Orders, OrderItems', 0),
(8, 2, 1, 0.94, '编写常用查询SQL（订单统计、热门商品等）。', 'SELECT p.name, COUNT(*) as sales FROM products p JOIN order_items oi ON p.id=oi.product_id GROUP BY p.id ORDER BY sales DESC LIMIT 10;', 0),
(8, 1, 1, 0.92, '设计Book和User类及其关系。', 'class Book { private String isbn; private String title; }\nclass User { private int id; private String name; private List<Book> borrowedBooks; }', 0),
(8, 1, 1, 0.93, '实现图书借阅和归还方法。', 'public void borrowBook(User user, Book book) throws Exception {\n    if (book.isAvailable()) {\n        user.addBook(book);\n        book.setAvailable(false);\n    } else {\n        throw new Exception("Book not available");\n    }\n}', 0),
(8, 5, 1, 0.91, '设计LRU缓存的数据结构。', 'class LRUCache {\n    private int capacity;\n    private HashMap<Integer, Node> map;\n    private DoubleLinkedList list;\n}', 0),
(8, 5, 1, 0.92, '实现get和put方法（时间复杂度O(1)）。', 'public int get(int key) {\n    if (!map.containsKey(key)) return -1;\n    Node node = map.get(key);\n    list.moveToHead(node);\n    return node.value;\n}', 0),
(8, 6, 1, 0.94, '使用信号量实现生产者-消费者同步。', 'Semaphore mutex = new Semaphore(1);\nSemaphore empty = new Semaphore(N);\nSemaphore full = new Semaphore(0);\n// Producer: empty.acquire(); mutex.acquire(); ...produce...; mutex.release(); full.release();', 0),
(6, 6, 1, 0.95, '分析可能的死锁情况并提出解决方案。', '死锁可能发生在资源竞争时。解决方案：1)资源有序分配；2)超时机制；3)死锁检测与恢复；4)使用ReentrantLock的tryLock()。', 0),
(6, 8, 1, 0.90, '编写在线考试系统的需求规格说明。', '功能需求：用户管理、题库管理、试卷生成、在线答题、自动评分、成绩查询。非功能需求：并发性能、数据安全、可用性99.9%。', 0),
(6, 8, 1, 0.91, '设计系统架构并绘制模块图。', '采用三层架构：表示层(Vue)、业务层(Spring Boot)、数据层(MySQL)。主要模块：用户模块、题库模块、考试模块、评分模块、统计模块。', 0),
(1, 9, 1, 0.72, 'The Internet has __1__ our lives in many ways.', '互联网在许多方面改变了我们的生活。changed是正确答案，表示"改变"。', 0),
(1, 9, 1, 0.73, 'We can now __2__ with people around the world instantly.', '我们现在可以即时与世界各地的人交流。communicate是正确答案，表示"交流"。', 0),
(1, 9, 1, 0.74, 'Online shopping has made it __3__ to buy products without leaving home.', '网上购物使得不出家门购买产品变得方便。convenient是正确答案，表示"方便的"。', 0),
(1, 9, 1, 0.75, 'However, we must be __4__ about online security.', '然而，我们必须小心网络安全。careful是正确答案，表示"小心的"。', 0),
(1, 9, 1, 0.76, 'Climate change is one of the biggest __1__ facing humanity.', '气候变化是人类面临的最大挑战之一。challenges是正确答案，表示"挑战"。', 0),
(1, 9, 1, 0.77, 'Scientists warn that we must __2__ action soon.', '科学家警告我们必须尽快采取行动。take是正确答案，表示"采取"。', 0),
(1, 9, 1, 0.78, 'Many countries are investing in __3__ energy sources.', '许多国家正在投资可再生能源。renewable是正确答案，表示"可再生的"。', 0),
(1, 9, 1, 0.79, '__4__ efforts are needed to protect our planet.', '需要全球努力来保护我们的星球。Global是正确答案，表示"全球的"。', 0),
(1, 9, 1, 0.78, 'What is the main topic of the passage?', '文章的主题是气候变化及其影响。', 0),
(1, 9, 1, 0.79, 'What are scientists warning about?', '科学家警告如果不采取行动，后果可能是灾难性的。', 0),
(1, 9, 1, 0.80, 'What are countries doing to address climate change?', '各国正在减少碳排放和发展可再生能源。', 0),
(1, 9, 1, 0.80, 'What industries is AI transforming?', 'AI正在改变医疗和金融等多个行业。', 0),
(1, 9, 1, 0.81, 'What concerns does AI raise?', 'AI引发了隐私、工作流失和道德使用等问题。', 0),
(1, 9, 1, 0.82, 'What does the passage suggest about AI development?', '文章建议社会必须在创新与责任之间取得平衡。', 0);

INSERT INTO choice (question_id, identification, content, sequence, flag) VALUES
(1, 'A', 'interface', 1, 0),
(1, 'B', 'class', 2, 1),
(1, 'C', 'extends', 3, 0),
(1, 'D', 'implements', 4, 0),
(2, 'A', 'int', 1, 0),
(2, 'B', 'String', 2, 1),
(2, 'C', 'boolean', 3, 0),
(2, 'D', 'double', 4, 0),
(3, 'A', 'UPDATE', 1, 0),
(3, 'B', 'SELECT', 2, 1),
(3, 'C', 'DELETE', 3, 0),
(3, 'D', 'INSERT', 4, 0),
(4, 'A', '栈', 1, 0),
(4, 'B', '队列', 2, 1),
(4, 'C', '树', 3, 0),
(4, 'D', '图', 4, 0),
(5, 'A', '存储数据', 1, 0),
(5, 'B', '运算和控制', 2, 1),
(5, 'C', '输入输出', 3, 0),
(5, 'D', '显示图像', 4, 0),
(6, 'A', '5层', 1, 0),
(6, 'B', '7层', 2, 1),
(6, 'C', '9层', 3, 0),
(6, 'D', '10层', 4, 0),
(7, 'A', 'COUNT', 1, 1),
(7, 'B', 'SUM', 2, 1),
(7, 'C', 'AVG', 3, 1),
(7, 'D', 'MAX', 4, 1),
(7, 'E', 'MIN', 5, 1),
(8, 'A', '选择', 1, 1),
(8, 'B', '投影', 2, 1),
(8, 'C', '连接', 3, 1),
(8, 'D', '排序', 4, 0),
(9, 'A', '封装', 1, 1),
(9, 'B', '继承', 2, 1),
(9, 'C', '多态', 3, 1),
(9, 'D', '抽象', 4, 0),
(10, 'A', '数组', 1, 1),
(10, 'B', '链表', 2, 1),
(10, 'C', '栈', 3, 1),
(10, 'D', '队列', 4, 1),
(10, 'E', '树', 5, 0),
(11, 'A', '就绪', 1, 1),
(11, 'B', '运行', 2, 1),
(11, 'C', '阻塞', 3, 1),
(11, 'D', '终止', 4, 0),
(12, 'A', 'Vue', 1, 1),
(12, 'B', 'React', 2, 1),
(12, 'C', 'Angular', 3, 1),
(12, 'D', 'jQuery', 4, 0),
(13, 'A', '正确', 1, 0),
(13, 'B', '错误', 2, 1),
(14, 'A', '正确', 1, 0),
(14, 'B', '错误', 2, 1),
(15, 'A', '正确', 1, 1),
(15, 'B', '错误', 2, 0),
(16, 'A', '正确', 1, 0),
(16, 'B', '错误', 2, 1),
(17, 'A', '正确', 1, 1),
(17, 'B', '错误', 2, 0),
(18, 'A', '正确', 1, 0),
(18, 'B', '错误', 2, 1),
(91, 'A', 'Climate change and its effects', 1, 1),
(91, 'B', 'Global warming only', 2, 0),
(91, 'C', 'Renewable energy', 3, 0),
(92, 'A', 'Everything is fine', 1, 0),
(92, 'B', 'The consequences of not taking action could be catastrophic', 2, 1),
(92, 'C', 'No action needed', 3, 0),
(93, 'A', 'Doing nothing', 1, 0),
(93, 'B', 'Reducing carbon emissions and developing renewable energy', 2, 1),
(93, 'C', 'Increasing emissions', 3, 0),
(94, 'A', 'Only manufacturing', 1, 0),
(94, 'B', 'Healthcare and finance, among others', 2, 1),
(94, 'C', 'Only education', 3, 0),
(94, 'D', 'Only retail', 4, 0),
(95, 'A', 'No concerns', 1, 0),
(95, 'B', 'Privacy, job displacement, and ethical use', 2, 1),
(95, 'C', 'Cost only', 3, 0),
(95, 'D', 'Speed only', 4, 0),
(96, 'A', 'Ignore responsibility', 1, 0),
(96, 'B', 'Only focus on profits', 2, 0),
(96, 'C', 'Balance innovation with responsibility', 3, 1),
(96, 'D', 'Stop AI development', 4, 0),
(83, 'A', 'changed', 1, 1),
(83, 'B', 'destroyed', 2, 0),
(83, 'C', 'ignored', 3, 0),
(83, 'D', 'forgotten', 4, 0),
(84, 'A', 'argue', 1, 0),
(84, 'B', 'communicate', 2, 1),
(84, 'C', 'fight', 3, 0),
(84, 'D', 'compete', 4, 0),
(85, 'A', 'difficult', 1, 0),
(85, 'B', 'impossible', 2, 0),
(85, 'C', 'convenient', 3, 1),
(85, 'D', 'dangerous', 4, 0),
(86, 'A', 'careless', 1, 0),
(86, 'B', 'careful', 2, 1),
(86, 'C', 'happy', 3, 0),
(86, 'D', 'angry', 4, 0),
(87, 'A', 'opportunities', 1, 0),
(87, 'B', 'challenges', 2, 1),
(87, 'C', 'advantages', 3, 0),
(87, 'D', 'benefits', 4, 0),
(88, 'A', 'give', 1, 0),
(88, 'B', 'take', 2, 1),
(88, 'C', 'make', 3, 0),
(88, 'D', 'do', 4, 0),
(89, 'A', 'limited', 1, 0),
(89, 'B', 'traditional', 2, 0),
(89, 'C', 'renewable', 3, 1),
(89, 'D', 'expensive', 4, 0),
(90, 'A', 'Local', 1, 0),
(90, 'B', 'Individual', 2, 0),
(90, 'C', 'National', 3, 0),
(90, 'D', 'Global', 4, 1);

INSERT INTO composite (question_father_id, question_son_id, sequence) VALUES
(49, 63, 1),
(49, 64, 2),
(50, 65, 1),
(50, 66, 2),
(51, 67, 1),
(51, 68, 2),
(52, 69, 1),
(52, 70, 2),
(53, 71, 1),
(53, 72, 2),
(54, 73, 1),
(54, 74, 2),
(55, 75, 1),
(55, 76, 2),
(56, 77, 1),
(56, 78, 2),
(57, 79, 1),
(57, 80, 2),
(58, 81, 1),
(58, 82, 2),
(59, 83, 1),
(59, 84, 2),
(59, 85, 3),
(59, 86, 4),
(60, 87, 1),
(60, 88, 2),
(60, 89, 3),
(60, 90, 4),
(61, 91, 1),
(61, 92, 2),
(61, 93, 3),
(62, 94, 1),
(62, 95, 2),
(62, 96, 3);

-- =========================================================
-- 真实场景增强数据：2026 春季学期在线考试业务样例
-- 说明：保留原始题库数据，补充用户、班级、试卷、考试、成绩、作答和系统日志。
-- =========================================================

-- 补全原始默认班级的授课教师，避免教师端筛选时为空
UPDATE clazz SET teacher_id = 1 WHERE id IN (1, 2, 3);
UPDATE clazz SET teacher_id = 2 WHERE id IN (4, 5, 6);
UPDATE clazz SET teacher_id = 3 WHERE id IN (7, 8, 9);

INSERT INTO admin (id, username, password, name, role, phone, email) VALUES
(2, 'academic_admin', '123456', '教务管理员', 'ADMIN', '18801002001', 'academic@xm.edu.cn'),
(3, 'exam_admin', '123456', '考试中心管理员', 'ADMIN', '18801002002', 'exam-center@xm.edu.cn');

INSERT INTO teacher (id, username, password, name, role, phone, email) VALUES
(4, 'chenlei', '123456', '陈磊', 'TEACHER', '13910001001', 'chenlei@xm.edu.cn'),
(5, 'liuna', '123456', '刘娜', 'TEACHER', '13910001002', 'liuna@xm.edu.cn'),
(6, 'wangqiang', '123456', '王强', 'TEACHER', '13910001003', 'wangqiang@xm.edu.cn'),
(7, 'zhaomin', '123456', '赵敏', 'TEACHER', '13910001004', 'zhaomin@xm.edu.cn'),
(8, 'sunying', '123456', '孙颖', 'TEACHER', '13910001005', 'sunying@xm.edu.cn');

INSERT INTO student (id, username, password, name, role, phone, email, status) VALUES
(5, '2023210101', '123456', '王浩宇', 'STUDENT', '13620001001', '2023210101@stu.xm.edu.cn', '审核通过'),
(6, '2023210102', '123456', '刘思涵', 'STUDENT', '13620001002', '2023210102@stu.xm.edu.cn', '审核通过'),
(7, '2023210103', '123456', '陈雨欣', 'STUDENT', '13620001003', '2023210103@stu.xm.edu.cn', '审核通过'),
(8, '2023210104', '123456', '赵子轩', 'STUDENT', '13620001004', '2023210104@stu.xm.edu.cn', '审核通过'),
(9, '2023210105', '123456', '孙佳怡', 'STUDENT', '13620001005', '2023210105@stu.xm.edu.cn', '审核通过'),
(10, '2023210106', '123456', '周明哲', 'STUDENT', '13620001006', '2023210106@stu.xm.edu.cn', '审核通过'),
(11, '2023210107', '123456', '吴泽宇', 'STUDENT', '13620001007', '2023210107@stu.xm.edu.cn', '审核通过'),
(12, '2023210108', '123456', '郑雅婷', 'STUDENT', '13620001008', '2023210108@stu.xm.edu.cn', '审核通过'),
(13, '2023210109', '123456', '黄梓涵', 'STUDENT', '13620001009', '2023210109@stu.xm.edu.cn', '审核通过'),
(14, '2023210110', '123456', '马宇航', 'STUDENT', '13620001010', '2023210110@stu.xm.edu.cn', '审核通过'),
(15, '2023220201', '123456', '林若曦', 'STUDENT', '13620002001', '2023220201@stu.xm.edu.cn', '审核通过'),
(16, '2023220202', '123456', '郭嘉乐', 'STUDENT', '13620002002', '2023220202@stu.xm.edu.cn', '审核通过'),
(17, '2023220203', '123456', '何雨桐', 'STUDENT', '13620002003', '2023220203@stu.xm.edu.cn', '审核通过'),
(18, '2023220204', '123456', '高俊杰', 'STUDENT', '13620002004', '2023220204@stu.xm.edu.cn', '审核通过'),
(19, '2023220205', '123456', '罗欣怡', 'STUDENT', '13620002005', '2023220205@stu.xm.edu.cn', '审核通过'),
(20, '2023220206', '123456', '谢天佑', 'STUDENT', '13620002006', '2023220206@stu.xm.edu.cn', '审核通过'),
(21, '2023220207', '123456', '曹一鸣', 'STUDENT', '13620002007', '2023220207@stu.xm.edu.cn', '审核通过'),
(22, '2023220208', '123456', '许诺', 'STUDENT', '13620002008', '2023220208@stu.xm.edu.cn', '审核通过'),
(23, '2023220209', '123456', '蒋晨曦', 'STUDENT', '13620002009', '2023220209@stu.xm.edu.cn', '审核通过'),
(24, '2023220210', '123456', '韩子墨', 'STUDENT', '13620002010', '2023220210@stu.xm.edu.cn', '待审核');

INSERT INTO clazz (id, name, course_id, teacher_id) VALUES
(10, 'Java程序设计-计科2301班', 1, 4),
(11, 'Java程序设计-软件2302班', 1, 4),
(12, '数据库原理与应用-计科2301班', 2, 5),
(13, '数据库原理与应用-软件2302班', 2, 5),
(14, '数据结构与算法-计科2301班', 5, 6),
(15, 'Web前端开发-软件2302班', 4, 7),
(16, '操作系统原理-计科2301班', 6, 6),
(17, '大学英语-2023级A班', 9, 8),
(18, '软件工程-软件2302班', 8, 2);

INSERT INTO student_clazz (student_id, clazz_id) VALUES
(5,10),(5,12),(5,14),(5,16),(5,17),
(6,10),(6,12),(6,14),(6,16),(6,17),
(7,10),(7,12),(7,14),(7,16),(7,17),
(8,10),(8,12),(8,14),(8,16),(8,17),
(9,10),(9,12),(9,14),(9,16),(9,17),
(10,10),(10,12),(10,14),(10,16),(10,17),
(11,10),(11,12),(11,14),(11,16),(11,17),
(12,10),(12,12),(12,14),(12,16),(12,17),
(13,10),(13,12),(13,14),(13,16),(13,17),
(14,10),(14,12),(14,14),(14,16),(14,17),
(15,11),(15,13),(15,15),(15,17),(15,18),
(16,11),(16,13),(16,15),(16,17),(16,18),
(17,11),(17,13),(17,15),(17,17),(17,18),
(18,11),(18,13),(18,15),(18,17),(18,18),
(19,11),(19,13),(19,15),(19,17),(19,18),
(20,11),(20,13),(20,15),(20,17),(20,18),
(21,11),(21,13),(21,15),(21,17),(21,18),
(22,11),(22,13),(22,15),(22,17),(22,18),
(23,11),(23,13),(23,15),(23,17),(23,18);

INSERT INTO notice (title, content, time, admin_id) VALUES
('2026春季学期线上考试规范', '请各班学生提前完成摄像头、麦克风和浏览器权限检查。考试过程中禁止切屏、复制粘贴和使用外部通讯工具。', '2026-03-01 08:30:00', 3),
('期中考试准考信息发布', '2026春季学期期中考试安排已发布，请登录系统核对考试科目、时间和班级范围。', '2026-04-07 10:00:00', 2),
('五一假期后补考报名提醒', '需参加补考的学生请在2026-05-12前完成报名，逾期将不再开放补报。', '2026-05-06 16:20:00', 3);

INSERT INTO article (title, content, time, student_id) VALUES
('数据库事务隔离级别复习清单', '整理了读未提交、读已提交、可重复读和串行化的差异，并附上脏读、不可重复读、幻读示例。', '2026-04-02 21:12:00', 5),
('Java期中考试错题复盘', '这次主要问题集中在集合框架和多态，建议先画类关系图再判断运行结果。', '2026-04-19 20:35:00', 6),
('前端综合测验备考路线', 'Vue组件通信、路由守卫、异步请求封装和性能优化是近期复习重点。', '2026-05-04 19:40:00', 15),
('英语阅读题答题技巧', '先定位题干关键词，再回到原文找同义替换，长难句可按主谓宾拆解。', '2026-05-08 12:05:00', 19);

INSERT INTO test_paper_template (id, name, course_id, teacher_id, choice_num, multi_choice_num, fill_in_num, check_num, text_num, composite_num, choice_score, multi_choice_score, fill_in_score, check_score, text_score, composite_score) VALUES
(4, 'Java期中标准模板', 1, 4, 2, 1, 1, 1, 2, 0, 5, 10, 10, 5, 20, 0),
(5, '数据库事务章节模板', 2, 5, 1, 2, 1, 1, 2, 1, 5, 10, 10, 5, 20, 30),
(6, '前端综合应用模板', 4, 7, 1, 1, 1, 0, 2, 1, 10, 10, 10, 0, 20, 40),
(7, '英语阅读理解模板', 9, 8, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 25);

INSERT INTO test_paper (id, name, course_id, teacher_id, type, level) VALUES
(1, 'Java程序设计期中试卷-2026春', 1, 4, '固定组卷', 0.68),
(2, '数据库原理章节测验-事务与SQL', 2, 5, '固定组卷', 0.72),
(3, '数据结构与算法月考试卷-线性表与树', 5, 6, '固定组卷', 0.70),
(4, 'Web前端开发综合测验-组件与性能', 4, 7, '固定组卷', 0.66),
(5, '大学英语阅读理解模拟卷-2026春', 9, 8, '固定组卷', 0.62);

INSERT INTO test_paper_question (id, test_paper_id, question_id, sequence, score, type) VALUES
(1,1,1,1,5,'common'),(2,1,2,2,5,'common'),(3,1,9,3,10,'common'),(4,1,13,4,5,'common'),
(5,1,19,5,10,'common'),(6,1,27,6,15,'common'),(7,1,42,7,20,'common'),(8,1,45,8,30,'common'),
(9,2,3,1,5,'common'),(10,2,7,2,10,'common'),(11,2,8,3,10,'common'),(12,2,14,4,5,'common'),
(13,2,20,5,10,'common'),(14,2,31,6,20,'common'),(15,2,39,7,10,'common'),(16,2,49,8,0,'father'),
(17,2,63,9,15,'son'),(18,2,64,10,15,'son'),
(19,3,4,1,5,'common'),(20,3,10,2,10,'common'),(21,3,16,3,5,'common'),(22,3,22,4,10,'common'),
(23,3,28,5,15,'common'),(24,3,33,6,20,'common'),(25,3,37,7,10,'common'),(26,3,38,8,10,'common'),(27,3,46,9,15,'common'),
(28,4,12,1,10,'common'),(29,4,24,2,10,'common'),(30,4,36,3,20,'common'),(31,4,47,4,20,'common'),
(32,4,58,5,0,'father'),(33,4,81,6,20,'son'),(34,4,82,7,20,'son'),
(35,5,59,1,0,'father'),(36,5,83,2,6,'son'),(37,5,84,3,6,'son'),(38,5,85,4,6,'son'),(39,5,86,5,6,'son'),
(40,5,60,6,0,'father'),(41,5,87,7,6,'son'),(42,5,88,8,6,'son'),(43,5,89,9,6,'son'),(44,5,90,10,6,'son'),
(45,5,61,11,0,'father'),(46,5,91,12,8,'son'),(47,5,92,13,8,'son'),(48,5,93,14,8,'son'),
(49,5,62,15,0,'father'),(50,5,94,16,9,'son'),(51,5,95,17,9,'son'),(52,5,96,18,10,'son');

INSERT INTO test (id, name, content, test_paper_id, start, end, duration, release_time, weather_check, weather_question_unordered, weather_choice_unordered, weather_copy, weather_face, weather_dual_camera, weather_screen_switch) VALUES
(1, 'Java程序设计期中考试', '考试范围：Java基础语法、面向对象、异常处理、集合框架。请提前10分钟进入考场。', 1, '2026-04-18 09:00:00', '2026-04-18 11:00:00', 120, '2026-04-10 09:00:00', 1, 1, 1, 1, 1, 0, 1),
(2, '数据库原理章节测验', '考试范围：SQL查询、事务、范式、ER建模。主观题由教师统一批改。', 2, '2026-04-28 14:00:00', '2026-04-28 15:30:00', 90, '2026-04-20 10:00:00', 1, 1, 1, 1, 1, 0, 1),
(3, '数据结构与算法月考', '考试范围：线性表、栈队列、树与排序。允许使用系统内置草稿区。', 3, '2026-05-15 09:30:00', '2026-05-15 11:00:00', 90, '2026-05-01 09:00:00', 0, 1, 1, 1, 1, 0, 1),
(4, 'Web前端开发综合测验', '考试范围：Vue组件、路由、请求封装、前端性能优化。', 4, '2026-05-08 19:00:00', '2026-05-08 20:30:00', 90, '2026-04-30 18:00:00', 1, 0, 1, 1, 1, 1, 1),
(5, '大学英语阅读理解模拟考试', '请按题目要求完成完形填空和阅读理解。', 5, '2026-05-20 14:00:00', '2026-05-20 15:20:00', 80, '2026-05-05 10:00:00', 0, 0, 1, 0, 0, 0, 0);

INSERT INTO test_clazz (test_id, clazz_id) VALUES
(1,10),(1,11),
(2,12),(2,13),
(3,14),
(4,15),
(5,17);

INSERT INTO score (id, student_id, test_id, score, status, cheat) VALUES
(1,5,1,92,'已批改',0),(2,6,1,84,'已批改',0),(3,7,1,76,'已批改',0),(4,8,1,88,'已批改',0),
(5,9,1,69,'已批改',0),(6,10,1,95,'已批改',0),(7,11,1,81,'已批改',0),(8,12,1,0,'待批改',1),
(9,15,1,73,'已批改',0),(10,16,1,90,'已批改',0),(11,17,1,58,'已批改',0),(12,18,1,NULL,'待批改',0),
(13,5,2,86,'已批改',0),(14,6,2,79,'已批改',0),(15,7,2,91,'已批改',0),(16,8,2,64,'已批改',0),
(17,9,2,88,'已批改',0),(18,10,2,NULL,'待批改',0),(19,15,2,82,'已批改',0),(20,16,2,77,'已批改',0),(21,17,2,0,'待批改',1),
(22,15,4,89,'已批改',0),(23,16,4,94,'已批改',0),(24,17,4,68,'已批改',0),(25,18,4,72,'已批改',0),
(26,19,4,85,'已批改',0),(27,20,4,NULL,'待批改',0),(28,21,4,0,'待批改',1),(29,22,4,78,'已批改',0),(30,23,4,81,'已批改',0);

-- 根据试卷题目自动生成作答明细。已批改成绩按总分比例分配，待批改成绩保留待批改状态。
INSERT INTO answer (score_id, test_paper_question_id, answer, score, status)
SELECT
  s.id,
  tpq.id,
  CASE
    WHEN tpq.type = 'father' THEN '已阅读材料题干，子题见对应作答记录'
    WHEN s.status = '待批改' THEN CONCAT('学生作答待阅：', LEFT(COALESCE(q.answer, '见题目要求'), 120))
    WHEN s.score >= 85 THEN COALESCE(q.answer, '答题要点完整，论证清晰')
    WHEN s.score >= 70 THEN CONCAT('要点基本正确：', LEFT(COALESCE(q.answer, '覆盖主要知识点'), 120))
    ELSE CONCAT('回答不完整：', LEFT(COALESCE(q.answer, '部分要点缺失'), 100))
  END AS answer,
  CASE
    WHEN tpq.type = 'father' THEN 0
    WHEN s.status = '待批改' THEN NULL
    ELSE LEAST(tpq.score, GREATEST(0, ROUND(tpq.score * s.score / 100)))
  END AS answer_score,
  CASE WHEN s.status = '待批改' THEN '待批改' ELSE '已批改' END AS answer_status
FROM score s
JOIN test t ON t.id = s.test_id
JOIN test_paper_question tpq ON tpq.test_paper_id = t.test_paper_id
JOIN question q ON q.id = tpq.question_id
WHERE s.cheat = 0;

INSERT INTO exam_draft (student_id, test_id, draft_data, create_time, update_time) VALUES
(5, 3, '{"answers":{"23":"哈希表通过哈希函数定位键值对，平均查找复杂度为O(1)","25":"[1,3,1,4,5,2,6,9]"},"lastSave":"2026-05-09 21:35:12","remainingSeconds":5120}', '2026-05-09 21:30:10', '2026-05-09 21:35:12'),
(6, 3, '{"answers":{"22":"后序","24":"栈用于函数调用，队列用于任务调度"},"lastSave":"2026-05-09 21:42:30","remainingSeconds":4860}', '2026-05-09 21:38:02', '2026-05-09 21:42:30'),
(15, 5, '{"answers":{"36":"changed","37":"communicate","46":"The main topic is climate change."},"lastSave":"2026-05-09 20:18:45","remainingSeconds":3900}', '2026-05-09 20:10:01', '2026-05-09 20:18:45');

INSERT INTO system_log (user_id, role, operation, method, params, ip, location, time, duration) VALUES
(2, 'ADMIN', '发布期中考试安排', 'POST /test/add', '{"name":"Java程序设计期中考试"}', '192.168.10.12', '校内办公网', '2026-04-10 09:02:18', 126),
(4, 'TEACHER', '创建Java期中试卷', 'POST /testPaper/add', '{"courseId":1,"type":"固定组卷"}', '192.168.20.31', '第一教学楼', '2026-04-09 15:22:43', 214),
(5, 'STUDENT', '进入Java程序设计期中考试', 'GET /testPaper/start/1', '{"testId":1}', '10.10.23.101', '学生宿舍区', '2026-04-18 08:57:12', 88),
(12, 'STUDENT', '考试异常上报：切屏次数超限', 'POST /monitor/abnormal', '{"testId":1,"warningCount":3}', '10.10.23.118', '学生宿舍区', '2026-04-18 09:46:55', 53),
(5, 'TEACHER', '批改数据库章节测验主观题', 'POST /score/updateAnswerScore', '{"testId":2,"questionId":31}', '192.168.20.42', '第二教学楼', '2026-04-29 10:15:03', 342),
(15, 'STUDENT', '提交Web前端开发综合测验', 'POST /score/submit', '{"testId":4,"cheat":0}', '10.10.25.205', '学生宿舍区', '2026-05-08 20:26:40', 156),
(21, 'STUDENT', '考试异常上报：人脸核验失败', 'POST /monitor/abnormal', '{"testId":4,"reason":"face verify failed"}', '10.10.25.221', '学生宿舍区', '2026-05-08 19:31:22', 71),
(3, 'ADMIN', '查看异常考试记录', 'GET /score/abnormal', '{"testName":"Web前端"}', '192.168.10.15', '考试中心', '2026-05-09 09:12:06', 97),
(8, 'TEACHER', '发布大学英语阅读模拟考试', 'POST /test/add', '{"name":"大学英语阅读理解模拟考试"}', '192.168.20.55', '外语学院办公室', '2026-05-05 10:04:28', 184),
(6, 'STUDENT', '保存数据结构月考草稿', 'POST /examDraft/save', '{"testId":3}', '10.10.23.102', '学生宿舍区', '2026-05-09 21:42:30', 45);

-- 导入完成后恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;