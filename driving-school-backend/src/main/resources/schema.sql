-- 驾校管理系统数据库
CREATE DATABASE IF NOT EXISTS driving_school DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE driving_school;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    phone VARCHAR(20),
    real_name VARCHAR(50),
    avatar VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    status INT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 操作日志表
CREATE TABLE IF NOT EXISTS sys_log (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    operation VARCHAR(100),
    method VARCHAR(200),
    params TEXT,
    ip VARCHAR(50),
    duration BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学员表
CREATE TABLE IF NOT EXISTS student (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    id_card VARCHAR(20) NOT NULL UNIQUE,
    phone VARCHAR(20),
    gender INT COMMENT '0女 1男',
    birthday DATE,
    address VARCHAR(200),
    photo VARCHAR(500),
    status INT NOT NULL DEFAULT 0 COMMENT '0待审核 1在学 2已毕业 3已退学',
    register_date DATE,
    apply_type VARCHAR(10) COMMENT 'C1/C2/B1/B2/A1',
    user_id BIGINT,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_id_card (id_card),
    INDEX idx_status (status),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 教练表
CREATE TABLE IF NOT EXISTS coach (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    id_card VARCHAR(20),
    gender INT,
    coach_type VARCHAR(10) COMMENT '准教车型',
    license_no VARCHAR(50) COMMENT '教练证编号',
    license_expire DATE COMMENT '教练证有效期',
    experience_years INT COMMENT '从业年限',
    rating DECIMAL(2,1) COMMENT '评分1-5',
    status INT NOT NULL DEFAULT 1 COMMENT '0休假 1在岗 2离职',
    user_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 车辆表
CREATE TABLE IF NOT EXISTS vehicle (
    id BIGINT PRIMARY KEY,
    plate_no VARCHAR(20) NOT NULL UNIQUE,
    brand VARCHAR(50),
    model VARCHAR(50),
    car_type VARCHAR(10) COMMENT 'C1/C2/B1/B2',
    color VARCHAR(20),
    buy_date DATE,
    insurance_expire DATE COMMENT '保险到期',
    inspection_expire DATE COMMENT '年检到期',
    status INT NOT NULL DEFAULT 1 COMMENT '0维修中 1可用 2报废',
    mileage BIGINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_plate_no (plate_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 课程配置表
CREATE TABLE IF NOT EXISTS course (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '科目名',
    apply_type VARCHAR(10) COMMENT '适用车型',
    total_hours INT COMMENT '总学时要求',
    price_per_hour DECIMAL(10,2) COMMENT '课时单价',
    description VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 培训预约表
CREATE TABLE IF NOT EXISTS lesson (
    id BIGINT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    coach_id BIGINT NOT NULL,
    vehicle_id BIGINT,
    course_id BIGINT NOT NULL,
    lesson_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status INT NOT NULL DEFAULT 0 COMMENT '0待确认 1已确认 2进行中 3已完成 4已取消',
    actual_hours DECIMAL(4,2),
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_student_id (student_id),
    INDEX idx_coach_id (coach_id),
    INDEX idx_lesson_date (lesson_date),
    INDEX idx_status (status),
    -- 复合索引：加速冲突检测和日历查询
    INDEX idx_coach_date_status (coach_id, lesson_date, status),
    INDEX idx_student_date_status (student_id, lesson_date, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 考试安排表
CREATE TABLE IF NOT EXISTS exam (
    id BIGINT PRIMARY KEY,
    exam_type VARCHAR(20) NOT NULL COMMENT 'SUBJECT_1/2/3/4',
    exam_date DATE NOT NULL,
    exam_location VARCHAR(200),
    max_candidates INT,
    apply_type VARCHAR(10) COMMENT '适用车型',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_exam_date (exam_date),
    INDEX idx_exam_type (exam_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 考试报名表
CREATE TABLE IF NOT EXISTS exam_enrollment (
    id BIGINT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    apply_time DATETIME,
    status INT NOT NULL DEFAULT 0 COMMENT '0待考 1合格 2不合格 3缺考',
    score INT,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_student_id (student_id),
    INDEX idx_exam_id (exam_id),
    -- 复合索引：加速报名重复检查和名额统计
    INDEX idx_student_exam_status (student_id, exam_id, status),
    INDEX idx_exam_status (exam_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 收费记录表
CREATE TABLE IF NOT EXISTS payment (
    id BIGINT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    type INT NOT NULL COMMENT '1报名费 2课时费 3补考费 4其他',
    amount DECIMAL(10,2) NOT NULL,
    pay_method INT COMMENT '1现金 2微信 3支付宝 4银行卡',
    pay_time DATETIME,
    receipt_no VARCHAR(50),
    remark VARCHAR(500),
    operator_id BIGINT,
    refund_status INT NOT NULL DEFAULT 0 COMMENT '0正常 1已退费',
    refund_time DATETIME COMMENT '退费时间',
    original_payment_id BIGINT COMMENT '关联原支付记录（退费时）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_student_id (student_id),
    INDEX idx_receipt_no (receipt_no),
    INDEX idx_pay_time (pay_time),
    INDEX idx_refund_status (refund_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学员科目进度表
CREATE TABLE IF NOT EXISTS student_progress (
    id BIGINT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    subject_name VARCHAR(20) NOT NULL COMMENT '科目一/科目二/科目三/科目四',
    required_hours INT NOT NULL COMMENT '要求学时',
    completed_hours INT NOT NULL DEFAULT 0 COMMENT '已完成学时',
    status INT NOT NULL DEFAULT 0 COMMENT '0未开始 1进行中 2已完成',
    start_date DATE,
    complete_date DATE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_student_id (student_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 教练学员分配表
CREATE TABLE IF NOT EXISTS coach_student (
    id BIGINT PRIMARY KEY,
    coach_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    subject_name VARCHAR(20) COMMENT '负责科目',
    assign_date DATE NOT NULL,
    status INT NOT NULL DEFAULT 0 COMMENT '0进行中 1已完成 2已解除',
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_coach_id (coach_id),
    INDEX idx_student_id (student_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 教练评价表
CREATE TABLE IF NOT EXISTS coach_evaluation (
    id BIGINT PRIMARY KEY,
    coach_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    lesson_id BIGINT,
    rating INT NOT NULL COMMENT '评分1-5',
    content VARCHAR(500) COMMENT '评价内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_coach_id (coach_id),
    INDEX idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 车辆维修保养记录表
CREATE TABLE IF NOT EXISTS vehicle_maintenance (
    id BIGINT PRIMARY KEY,
    vehicle_id BIGINT NOT NULL,
    type INT NOT NULL COMMENT '1保养 2维修 3年检 4保险',
    description VARCHAR(500),
    cost DECIMAL(10,2),
    maintenance_date DATE NOT NULL,
    next_due_date DATE COMMENT '下次到期日',
    mileage BIGINT COMMENT '当时里程',
    operator VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_vehicle_id (vehicle_id),
    INDEX idx_type (type),
    INDEX idx_next_due_date (next_due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通知公告表
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    type INT NOT NULL DEFAULT 1 COMMENT '1系统通知 2考试通知 3课程通知 4紧急通知',
    target_role VARCHAR(20) COMMENT '目标角色 ALL/STUDENT/COACH/STAFF',
    target_user_id BIGINT COMMENT '指定用户ID（为空则按角色广播）',
    publisher_id BIGINT,
    is_read INT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_target_role (target_role),
    INDEX idx_target_user_id (target_user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 时间段模板表
CREATE TABLE IF NOT EXISTS time_slot_template (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '模板名称',
    day_of_week VARCHAR(20) COMMENT '适用星期 1,2,3,4,5,6,7',
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_capacity INT DEFAULT 1 COMMENT '最大预约人数',
    status INT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== P0: 电子合同表 ======
CREATE TABLE IF NOT EXISTS contract (
    id BIGINT PRIMARY KEY,
    student_id BIGINT NOT NULL COMMENT '学员ID',
    contract_no VARCHAR(50) NOT NULL UNIQUE COMMENT '合同编号',
    title VARCHAR(200) NOT NULL COMMENT '合同标题',
    content TEXT NOT NULL COMMENT '合同内容',
    amount DECIMAL(10,2) NOT NULL COMMENT '合同金额',
    apply_type VARCHAR(10) COMMENT '报名类型 C1/C2',
    status INT NOT NULL DEFAULT 0 COMMENT '0待签署 1已签署 2已作废',
    signed_time DATETIME COMMENT '签署时间',
    sign_data TEXT COMMENT '签署信息（签名/时间戳等）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_student_id (student_id),
    INDEX idx_contract_no (contract_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== P0: 学时签到表 ======
CREATE TABLE IF NOT EXISTS check_in (
    id BIGINT PRIMARY KEY,
    lesson_id BIGINT NOT NULL COMMENT '关联课时ID',
    student_id BIGINT NOT NULL COMMENT '学员ID',
    coach_id BIGINT NOT NULL COMMENT '教练ID',
    check_in_time DATETIME COMMENT '签到时间',
    check_out_time DATETIME COMMENT '签退时间',
    check_in_location VARCHAR(200) COMMENT '签到地点',
    check_in_type INT NOT NULL DEFAULT 1 COMMENT '1扫码签到 2手动签到',
    status INT NOT NULL DEFAULT 0 COMMENT '0待签到 1已签到 2已签退',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_lesson_id (lesson_id),
    INDEX idx_student_id (student_id),
    INDEX idx_coach_id (coach_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== P0: 自动提醒表 ======
CREATE TABLE IF NOT EXISTS reminder (
    id BIGINT PRIMARY KEY,
    target_user_id BIGINT NOT NULL COMMENT '目标用户ID',
    type INT NOT NULL COMMENT '1课前提醒 2考试提醒 3合同到期 4保险到期 5自定义',
    title VARCHAR(200) NOT NULL,
    content TEXT,
    remind_time DATETIME NOT NULL COMMENT '提醒时间',
    related_id BIGINT COMMENT '关联业务ID（课时ID/考试ID等）',
    related_type VARCHAR(20) COMMENT '关联业务类型 lesson/exam/contract',
    status INT NOT NULL DEFAULT 0 COMMENT '0待发送 1已发送 2已取消',
    send_method INT NOT NULL DEFAULT 1 COMMENT '1系统通知 2短信 3微信',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_target_user_id (target_user_id),
    INDEX idx_remind_time (remind_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== P1: 分期付款计划表 ======
CREATE TABLE IF NOT EXISTS installment_plan (
    id BIGINT PRIMARY KEY,
    student_id BIGINT NOT NULL COMMENT '学员ID',
    contract_id BIGINT COMMENT '关联合同ID',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总金额',
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '已付金额',
    installments INT NOT NULL COMMENT '总期数',
    paid_installments INT NOT NULL DEFAULT 0 COMMENT '已付期数',
    status INT NOT NULL DEFAULT 0 COMMENT '0进行中 1已结清 2逾期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_student_id (student_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 分期明细表
CREATE TABLE IF NOT EXISTS installment_detail (
    id BIGINT PRIMARY KEY,
    plan_id BIGINT NOT NULL COMMENT '分期计划ID',
    period INT NOT NULL COMMENT '第几期',
    amount DECIMAL(10,2) NOT NULL COMMENT '本期金额',
    due_date DATE NOT NULL COMMENT '应还日期',
    pay_time DATETIME COMMENT '实际还款时间',
    payment_id BIGINT COMMENT '关联支付记录ID',
    status INT NOT NULL DEFAULT 0 COMMENT '0待还 1已还 2逾期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_plan_id (plan_id),
    INDEX idx_due_date (due_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== P1: 教练绩效考核表 ======
CREATE TABLE IF NOT EXISTS coach_performance (
    id BIGINT PRIMARY KEY,
    coach_id BIGINT NOT NULL COMMENT '教练ID',
    month VARCHAR(7) NOT NULL COMMENT '考核月份 YYYY-MM',
    total_lessons INT NOT NULL DEFAULT 0 COMMENT '总课时数',
    completed_lessons INT NOT NULL DEFAULT 0 COMMENT '完成课时数',
    student_count INT NOT NULL DEFAULT 0 COMMENT '学员数',
    pass_rate DECIMAL(5,2) DEFAULT 0 COMMENT '学员通过率',
    avg_rating DECIMAL(3,2) DEFAULT 0 COMMENT '平均评分',
    complaint_count INT NOT NULL DEFAULT 0 COMMENT '投诉次数',
    score DECIMAL(5,2) DEFAULT 0 COMMENT '综合得分',
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_coach_month (coach_id, month),
    INDEX idx_month (month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== P1: 招生渠道表 ======
CREATE TABLE IF NOT EXISTS channel (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '渠道名称',
    type INT NOT NULL DEFAULT 1 COMMENT '1线上 2线下 3转介绍 4其他',
    contact VARCHAR(100) COMMENT '渠道联系人',
    phone VARCHAR(20) COMMENT '联系电话',
    cost DECIMAL(10,2) DEFAULT 0 COMMENT '渠道费用',
    status INT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学员渠道关联表
CREATE TABLE IF NOT EXISTS student_channel (
    id BIGINT PRIMARY KEY,
    student_id BIGINT NOT NULL COMMENT '学员ID',
    channel_id BIGINT NOT NULL COMMENT '渠道ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_student_id (student_id),
    INDEX idx_channel_id (channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== P2: 题库表 ======
CREATE TABLE IF NOT EXISTS question_bank (
    id BIGINT PRIMARY KEY,
    subject INT NOT NULL COMMENT '1科目一 4科目四',
    question_type INT NOT NULL DEFAULT 1 COMMENT '1单选 2多选 3判断',
    content TEXT NOT NULL COMMENT '题目内容',
    options JSON COMMENT '选项 [{key:A,text:xxx},{key:B,text:xxx}]',
    answer VARCHAR(10) NOT NULL COMMENT '正确答案',
    explanation TEXT COMMENT '解析',
    difficulty INT NOT NULL DEFAULT 1 COMMENT '1简单 2中等 3困难',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_subject (subject),
    INDEX idx_type (question_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学员刷题记录表
CREATE TABLE IF NOT EXISTS practice_record (
    id BIGINT PRIMARY KEY,
    student_id BIGINT NOT NULL COMMENT '学员ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    student_answer VARCHAR(10) COMMENT '学员答案',
    is_correct INT COMMENT '0错误 1正确',
    practice_time DATETIME NOT NULL COMMENT '答题时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_student_id (student_id),
    INDEX idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== P2: 满意度调查表 ======
CREATE TABLE IF NOT EXISTS satisfaction_survey (
    id BIGINT PRIMARY KEY,
    lesson_id BIGINT COMMENT '关联课时ID',
    student_id BIGINT NOT NULL COMMENT '学员ID',
    coach_id BIGINT NOT NULL COMMENT '教练ID',
    teaching_rating INT COMMENT '教学质量 1-5',
    attitude_rating INT COMMENT '服务态度 1-5',
    environment_rating INT COMMENT '教学环境 1-5',
    overall_rating INT COMMENT '总体评价 1-5',
    content TEXT COMMENT '文字评价',
    suggestion TEXT COMMENT '改进建议',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_student_id (student_id),
    INDEX idx_coach_id (coach_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== P2: 学员档案附件表 ======
CREATE TABLE IF NOT EXISTS student_attachment (
    id BIGINT PRIMARY KEY,
    student_id BIGINT NOT NULL COMMENT '学员ID',
    type INT NOT NULL COMMENT '1身份证 2体检报告 3照片 4合同 5其他',
    file_name VARCHAR(200) NOT NULL COMMENT '文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_size BIGINT COMMENT '文件大小(字节)',
    remark VARCHAR(200),
    uploader_id BIGINT COMMENT '上传人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_student_id (student_id),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== 在线报名表 ======
CREATE TABLE IF NOT EXISTS registration (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    id_card VARCHAR(20) NOT NULL COMMENT '身份证号',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    gender INT COMMENT '0女 1男',
    birthday DATE COMMENT '出生日期',
    address VARCHAR(200) COMMENT '联系地址',
    apply_type VARCHAR(10) NOT NULL COMMENT '报考类型 C1/C2/B1/B2',
    class_type VARCHAR(20) COMMENT '班型',
    id_card_front VARCHAR(500) COMMENT '身份证正面URL',
    id_card_back VARCHAR(500) COMMENT '身份证反面URL',
    remark VARCHAR(500) COMMENT '备注',
    status INT NOT NULL DEFAULT 0 COMMENT '0待审核 1已通过 2已驳回',
    reviewer_id BIGINT COMMENT '审核人ID',
    review_comment VARCHAR(500) COMMENT '审核意见',
    review_time DATETIME COMMENT '审核时间',
    student_id BIGINT COMMENT '关联学员ID',
    user_id BIGINT COMMENT '关联用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    INDEX idx_phone (phone),
    INDEX idx_status (status),
    INDEX idx_id_card (id_card),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== AI 聊天记录表 ======
CREATE TABLE IF NOT EXISTS `chat_history` (
  `id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `conversation_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT 'user/assistant',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_conversation_id` (`conversation_id`),
  INDEX `idx_user_conversation_time` (`user_id`, `conversation_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI聊天记录';

-- 初始账号 (密码: admin123/staff123/coach123/student123)
INSERT INTO sys_user (id, username, password, real_name, role, status) VALUES
(1, 'admin', '$2a$10$pkKgKRTitb/iWaF4eENIw.ApiITVvuRbWBZOPM1jkxQXzui1ROm46', 'Admin', 'ADMIN', 1),
(2, 'staff', '$2a$12$FynukNjqOyFPiOlnaOOyP.HfirkUXgqwy.6tc3Ey3djYhqp9V8n5i', 'Staff Zhang', 'STAFF', 1),
(3, 'coach', '$2a$12$XvGLVCujwSjmh1wcZLZa2udvgffMRISm7Y09FJYoZUjgYIGPFmL..', 'Coach Li', 'COACH', 1),
(4, 'student', '$2a$12$ueW9tmCaxKuX8.zGpp4rdutWb5mwBYRUZasSC4aaBsiLhCMrnM9Hu', 'Zhang San', 'STUDENT', 1);

-- 初始课程配置
INSERT INTO course (id, name, apply_type, total_hours, price_per_hour, description) VALUES
(1, '科目一', 'C1', 10, 50.00, '道路交通安全法律、法规和相关知识'),
(2, '科目二', 'C1', 16, 120.00, '场地驾驶技能考试'),
(3, '科目三', 'C1', 24, 150.00, '道路驾驶技能考试'),
(4, '科目四', 'C1', 10, 50.00, '安全文明驾驶常识'),
(5, '科目一', 'C2', 10, 50.00, '道路交通安全法律、法规和相关知识'),
(6, '科目二', 'C2', 16, 130.00, '场地驾驶技能考试'),
(7, '科目三', 'C2', 24, 160.00, '道路驾驶技能考试'),
(8, '科目四', 'C2', 10, 50.00, '安全文明驾驶常识');

-- 初始时间段模板
INSERT INTO time_slot_template (id, name, day_of_week, start_time, end_time, max_capacity, status) VALUES
(1, '上午第一段', '1,2,3,4,5', '08:00:00', '10:00:00', 3, 1),
(2, '上午第二段', '1,2,3,4,5', '10:00:00', '12:00:00', 3, 1),
(3, '下午第一段', '1,2,3,4,5', '13:30:00', '15:30:00', 3, 1),
(4, '下午第二段', '1,2,3,4,5', '15:30:00', '17:30:00', 3, 1),
(5, '周末上午', '6,7', '08:00:00', '12:00:00', 5, 1),
(6, '周末下午', '6,7', '13:30:00', '17:30:00', 5, 1);

-- 示例通知
INSERT INTO notification (id, title, content, type, target_role, publisher_id, is_read) VALUES
(1, '系统上线通知', '驾校管理系统已正式上线，请各岗位人员及时登录并熟悉操作流程。', 1, 'ALL', 1, 0),
(2, '2024年1月考试安排', '科目二考试定于1月15日上午进行，请相关学员提前做好准备。', 2, 'STUDENT', 1, 0);
