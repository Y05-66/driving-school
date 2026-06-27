/**
 * @description API 接口统一管理模块
 * 集中定义了系统所有后端接口的调用方法，按业务模块分组：
 * 1. 认证接口：登录、登出、获取用户信息、修改密码
 * 2. 学员接口：学员 CRUD、状态管理、进度查询
 * 3. 教练接口：教练 CRUD、状态管理、学员分配、评价查询
 * 4. 车辆接口：车辆 CRUD、状态管理、维修保养记录
 * 5. 课程接口：课程列表与详情
 * 6. 课时预约接口：预约 CRUD、确认/完成/取消、日历数据、时间段模板
 * 7. 考试接口：考试安排、报名、成绩录入
 * 8. 财务接口：收费记录、退费、财务汇总、日报、教练结算
 * 9. 统计接口：总览、收入趋势、报名趋势、通过率、排行
 * 10. 通知接口：通知列表、已读标记、发布通知
 * 11. 用户管理接口：用户 CRUD、重置密码、状态管理
 * 12. 操作日志接口：日志查询
 *
 * 所有方法均调用 request 实例发送 HTTP 请求，返回 Promise
 */

import request from '../utils/request'

// ====== 认证相关接口 ======

/**
 * @description 用户登录
 * @param {object} data - { username: string, password: string }
 * @returns {Promise} 包含 accessToken 和 refreshToken
 */
export const login = (data) => request.post('/auth/login', data)

/**
 * @description 上传头像
 * @param {FormData} formData - 包含 file 字段的表单数据
 * @returns {Promise} 头像访问URL
 */
export const uploadAvatar = (formData) =>
  request.post('/auth/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

/**
 * @description 用户登出
 * @returns {Promise}
 */
export const logout = () => request.post('/auth/logout')

/**
 * @description 获取当前登录用户信息
 * @returns {Promise} 用户信息对象（含角色、姓名等）
 */
export const getUserInfo = () => request.get('/auth/info')

/**
 * @description 修改当前用户密码
 * @param {object} data - { oldPassword: string, newPassword: string }
 * @returns {Promise}
 */
export const changePassword = (data) => request.put('/auth/change-password', data)

// ====== 学员管理接口 ======

/**
 * @description 获取学员列表（分页）
 * @param {object} params - 查询参数：name, phone, status, applyType, pageNum, pageSize
 * @returns {Promise} 分页数据 { records: [], total: number }
 */
export const getStudents = (params) => request.get('/students', { params })

/**
 * @description 获取学员统计数据
 * @returns {Promise} { total, pending, studying, graduated, withdrawn }
 */
export const getStudentStats = () => request.get('/students/stats')

/**
 * @description 获取单个学员详情
 * @param {number|string} id - 学员ID
 * @returns {Promise} 学员详细信息
 */
export const getStudent = (id) => request.get(`/students/${id}`)

/**
 * @description 新增学员（学员报名）
 * @param {object} data - 学员信息（姓名、身份证、电话等）
 * @returns {Promise}
 */
export const createStudent = (data) => request.post('/students', data)

/**
 * @description 更新学员信息
 * @param {number|string} id - 学员ID
 * @param {object} data - 需要更新的字段
 * @returns {Promise}
 */
export const updateStudent = (id, data) => request.put(`/students/${id}`, data)

/**
 * @description 更新学员状态（审核通过/退学等）
 * @param {number|string} id - 学员ID
 * @param {number} status - 新状态值（0:待审核, 1:在学, 2:已毕业, 3:已退学）
 * @returns {Promise}
 */
export const updateStudentStatus = (id, status) => request.put(`/students/${id}/status`, null, { params: { status } })

/**
 * @description 获取指定学员的科目进度详情
 * @param {number|string} id - 学员ID
 * @returns {Promise} 各科目的学时完成情况
 */
export const getStudentProgress = (id) => request.get(`/students/${id}/progress/detail`)

/**
 * @description 获取当前登录学员自己的进度（学员角色使用）
 * @returns {Promise}
 */
export const getMyProgress = () => request.get('/students/me/progress')

// ====== 教练管理接口 ======

/**
 * @description 获取教练列表（分页）
 * @param {object} params - 查询参数：name, status, pageNum, pageSize
 * @returns {Promise} 分页数据
 */
export const getCoaches = (params) => request.get('/coaches', { params })

/**
 * @description 获取单个教练详情
 * @param {number|string} id - 教练ID
 * @returns {Promise} 教练详细信息
 */
export const getCoach = (id) => request.get(`/coaches/${id}`)

/**
 * @description 新增教练
 * @param {object} data - 教练信息（姓名、电话、准教车型等）
 * @returns {Promise}
 */
export const createCoach = (data) => request.post('/coaches', data)

/**
 * @description 更新教练信息
 * @param {number|string} id - 教练ID
 * @param {object} data - 需要更新的字段
 * @returns {Promise}
 */
export const updateCoach = (id, data) => request.put(`/coaches/${id}`, data)

/**
 * @description 更新教练状态（在岗/休假/离职）
 * @param {number|string} id - 教练ID
 * @param {number} status - 新状态值（0:休假, 1:在岗, 2:离职）
 * @returns {Promise}
 */
export const updateCoachStatus = (id, status) => request.put(`/coaches/${id}/status`, null, { params: { status } })

/**
 * @description 为教练分配学员
 * @param {number|string} coachId - 教练ID
 * @param {number|string} studentId - 学员ID
 * @param {string} subjectName - 负责科目（科目一/二/三/四）
 * @returns {Promise}
 */
export const assignCoach = (coachId, studentId, subjectName) =>
  request.post(`/coaches/${coachId}/assign`, null, { params: { studentId, subjectName } })

/**
 * @description 获取教练的学员评价列表
 * @param {number|string} id - 教练ID
 * @param {object} params - 分页参数
 * @returns {Promise} 评价列表
 */
export const getCoachEvaluations = (id, params) => request.get(`/coaches/${id}/evaluations`, { params })

// ====== 车辆管理接口 ======

/**
 * @description 获取车辆列表（分页）
 * @param {object} params - 查询参数：plateNo, status, pageNum, pageSize
 * @returns {Promise} 分页数据
 */
export const getVehicles = (params) => request.get('/vehicles', { params })

/**
 * @description 获取单个车辆详情
 * @param {number|string} id - 车辆ID
 * @returns {Promise}
 */
export const getVehicle = (id) => request.get(`/vehicles/${id}`)

/**
 * @description 新增车辆
 * @param {object} data - 车辆信息（车牌号、品牌、型号等）
 * @returns {Promise}
 */
export const createVehicle = (data) => request.post('/vehicles', data)

/**
 * @description 更新车辆信息
 * @param {number|string} id - 车辆ID
 * @param {object} data - 需要更新的字段
 * @returns {Promise}
 */
export const updateVehicle = (id, data) => request.put(`/vehicles/${id}`, data)

/**
 * @description 更新车辆状态（可用/维修中/报废）
 * @param {number|string} id - 车辆ID
 * @param {number} status - 新状态值
 * @returns {Promise}
 */
export const updateVehicleStatus = (id, status) => request.put(`/vehicles/${id}/status`, null, { params: { status } })

/**
 * @description 获取指定车型的可用车辆列表
 * @param {string} carType - 车型（C1/C2）
 * @returns {Promise} 可用车辆列表
 */
export const getAvailableVehicles = (carType) => request.get('/vehicles/available', { params: { carType } })

/**
 * @description 获取车辆的维修保养记录
 * @param {number|string} vehicleId - 车辆ID
 * @returns {Promise} 维修记录列表
 */
export const getVehicleMaintenance = (vehicleId) => request.get(`/vehicles/${vehicleId}/maintenance`)

/**
 * @description 新增车辆维修保养记录
 * @param {number|string} vehicleId - 车辆ID
 * @param {object} data - 维修记录信息
 * @returns {Promise}
 */
export const createVehicleMaintenance = (vehicleId, data) => request.post(`/vehicles/${vehicleId}/maintenance`, data)

/**
 * @description 获取即将到期的维修/保险/年检记录
 * @param {number} daysAhead - 提前天数（如 30 表示查询 30 天内到期的记录）
 * @returns {Promise} 即将到期的记录列表
 */
export const getExpiringMaintenance = (daysAhead) => request.get('/vehicles/maintenance/expiring', { params: { daysAhead } })

// ====== 课程接口 ======

/**
 * @description 获取课程列表
 * @param {object} params - 查询参数
 * @returns {Promise} 课程列表
 */
export const getCourses = (params) => request.get('/courses', { params })

/**
 * @description 获取课程详情
 * @param {number|string} id - 课程ID
 * @returns {Promise}
 */
export const getCourse = (id) => request.get(`/courses/${id}`)

// ====== 课时预约接口 ======

/**
 * @description 获取课时预约列表（分页）
 * @param {object} params - 查询参数：status, lessonDate, pageNum, pageSize
 * @returns {Promise} 分页数据
 */
export const getLessons = (params) => request.get('/lessons', { params })

/**
 * @description 新建课时预约
 * @param {object} data - 预约信息（学员ID、教练ID、日期时间等）
 * @returns {Promise}
 */
export const createLesson = (data) => request.post('/lessons', data)

/**
 * @description 批量创建课时预约
 * @param {object} data - 批量预约数据
 * @returns {Promise}
 */
export const batchCreateLessons = (data) => request.post('/lessons/batch', data)

/**
 * @description 确认课时预约
 * @param {number|string} id - 预约ID
 * @returns {Promise}
 */
export const confirmLesson = (id) => request.put(`/lessons/${id}/confirm`)

/**
 * @description 完成课时预约（记录实际学时）
 * @param {number|string} id - 预约ID
 * @param {number} actualHours - 实际学时
 * @returns {Promise}
 */
export const completeLesson = (id, actualHours) => request.put(`/lessons/${id}/complete`, { actualHours })

/**
 * @description 取消课时预约
 * @param {number|string} id - 预约ID
 * @returns {Promise}
 */
export const cancelLesson = (id) => request.put(`/lessons/${id}/cancel`)

/**
 * @description 获取日历视图数据（按月展示预约情况）
 * @param {object} params - 查询参数（如年月）
 * @returns {Promise} 日历数据
 */
export const getCalendarData = (params) => request.get('/lessons/calendar', { params })

/**
 * @description 获取指定教练在某日期的可用时间段
 * @param {number|string} coachId - 教练ID
 * @param {string} date - 日期（YYYY-MM-DD）
 * @returns {Promise} 可用时间段列表
 */
export const getAvailableTimeSlots = (coachId, date) =>
  request.get('/lessons/available-time-slots', { params: { coachId, date } })

/**
 * @description 获取时间段模板列表
 * @returns {Promise} 模板列表
 */
export const getTimeSlotTemplates = () => request.get('/lessons/time-slot-templates')

/**
 * @description 新增时间段模板
 * @param {object} data - 模板数据
 * @returns {Promise}
 */
export const createTimeSlotTemplate = (data) => request.post('/lessons/time-slot-templates', data)

// ====== 考试管理接口 ======

/**
 * @description 获取考试安排列表（分页）
 * @param {object} params - 查询参数
 * @returns {Promise} 分页数据
 */
export const getExams = (params) => request.get('/exams', { params })

/**
 * @description 创建考试安排
 * @param {object} data - 考试信息（类型、日期、地点等）
 * @returns {Promise}
 */
export const createExam = (data) => request.post('/exams', data)

/**
 * @description 学员报名考试
 * @param {number|string} examId - 考试安排ID
 * @param {number|string} studentId - 学员ID
 * @returns {Promise}
 */
export const enrollExam = (examId, studentId) => request.post(`/exams/${examId}/enroll`, null, { params: { studentId } })

/**
 * @description 更新考试成绩
 * @param {number|string} id - 报名记录ID
 * @param {number} score - 考试分数
 * @param {number} status - 考试结果状态
 * @returns {Promise}
 */
export const updateExamResult = (id, score, status) =>
  request.put(`/exams/enrollments/${id}/result`, null, { params: { score, status } })

/**
 * @description 获取考试报名记录列表
 * @param {object} params - 查询参数
 * @returns {Promise} 报名记录列表
 */
export const getExamEnrollments = (params) => request.get('/exams/enrollments', { params })

// ====== 财务管理接口 ======

/**
 * @description 获取收费记录列表（分页）
 * @param {object} params - 查询参数：type, payMethod, startTime, endTime, pageNum, pageSize
 * @returns {Promise} 分页数据
 */
export const getPayments = (params) => request.get('/finance/payments', { params })

/**
 * @description 新增收费记录
 * @param {object} data - 收费信息（学员ID、类型、金额、支付方式等）
 * @returns {Promise}
 */
export const createPayment = (data) => request.post('/finance/payments', data)

/**
 * @description 退费操作
 * @param {number|string} id - 收费记录ID
 * @returns {Promise}
 */
export const refundPayment = (id) => request.post(`/finance/payments/${id}/refund`)

/**
 * @description 获取指定学员的缴费记录
 * @param {number|string} id - 学员ID
 * @returns {Promise} 缴费记录列表
 */
export const getStudentPayments = (id) => request.get(`/finance/payments/student/${id}`)

/**
 * @description 获取当前登录学员自己的缴费记录
 * @returns {Promise} 缴费记录列表
 */
export const getMyPayments = () => request.get('/finance/payments/my')

/**
 * @description 获取财务汇总数据（今日收入、本月收入等）
 * @returns {Promise} 汇总数据
 */
export const getFinanceSummary = () => request.get('/finance/summary')

/**
 * @description 获取财务日报
 * @param {string} date - 日期（YYYY-MM-DD）
 * @returns {Promise} 日报数据
 */
export const getDailyReport = (date) => request.get('/finance/daily', { params: { date } })

/**
 * @description 获取教练结算数据
 * @param {object} params - 查询参数
 * @returns {Promise} 结算数据
 */
export const getCoachSettlement = (params) => request.get('/finance/coach-settlement', { params })

// ====== 数据统计接口 ======

/**
 * @description 获取统计总览数据
 * @returns {Promise} 总览数据
 */
export const getOverview = () => request.get('/statistics/overview')

/**
 * @description 获取收入趋势数据
 * @param {string} period - 时间周期（如 'month'）
 * @returns {Promise} 趋势数据数组
 */
export const getRevenueTrend = (period) => request.get('/statistics/revenue', { params: { period } })

/**
 * @description 获取报名趋势数据
 * @returns {Promise} 趋势数据数组
 */
export const getEnrollmentTrend = () => request.get('/statistics/enrollment')

/**
 * @description 获取考试通过率统计
 * @param {string} groupBy - 分组维度（如 'subject' 按科目分组）
 * @returns {Promise} 通过率数据
 */
export const getPassRate = (groupBy) => request.get('/statistics/pass-rate', { params: { groupBy } })

/**
 * @description 获取教练排行榜
 * @returns {Promise} 教练排行数据
 */
export const getCoachRanking = () => request.get('/statistics/coach-ranking')

/**
 * @description 获取车辆使用率数据
 * @returns {Promise} 可用/维修中等状态的车辆数量
 */
export const getVehicleUsage = () => request.get('/statistics/vehicle-usage')

/**
 * @description 获取仪表盘数据（根据角色返回不同统计项）
 * @returns {Promise} 仪表盘数据
 */
export const getDashboard = () => request.get('/statistics/dashboard')

// ====== 通知公告接口 ======

/**
 * @description 获取通知列表（分页）
 * @param {object} params - 查询参数：pageNum, pageSize
 * @returns {Promise} 分页数据
 */
export const getNotifications = (params) => request.get('/notifications', { params })

/**
 * @description 获取未读通知数量
 * @returns {Promise} { count: number }
 */
export const getUnreadCount = () => request.get('/notifications/unread-count')

/**
 * @description 标记单条通知为已读
 * @param {number|string} id - 通知ID
 * @returns {Promise}
 */
export const markRead = (id) => request.put(`/notifications/${id}/read`)

/**
 * @description 标记所有通知为已读
 * @returns {Promise}
 */
export const markAllRead = () => request.put('/notifications/read-all')

/**
 * @description 删除通知
 * @param {number|string} id - 通知ID
 * @returns {Promise}
 */
export const deleteNotification = (id) => request.delete(`/notifications/${id}`)

/**
 * @description 发布新通知
 * @param {object} data - 通知信息（标题、内容、类型、目标角色等）
 * @returns {Promise}
 */
export const publishNotification = (data) => request.post('/notifications', data)

// ====== 用户管理接口 ======

/**
 * @description 获取用户列表（分页）
 * @param {object} params - 查询参数：username, role, pageNum, pageSize
 * @returns {Promise} 分页数据
 */
export const getUsers = (params) => request.get('/users', { params })

/**
 * @description 新增用户
 * @param {object} data - 用户信息（用户名、密码、角色等）
 * @returns {Promise}
 */
export const createUser = (data) => request.post('/users', data)

/**
 * @description 更新用户信息
 * @param {number|string} id - 用户ID
 * @param {object} data - 需要更新的字段
 * @returns {Promise}
 */
export const updateUser = (id, data) => request.put(`/users/${id}`, data)

/**
 * @description 重置用户密码（管理员操作）
 * @param {number|string} id - 用户ID
 * @param {string} newPassword - 新密码
 * @returns {Promise}
 */
export const resetPassword = (id, newPassword) => request.put(`/users/${id}/reset-password`, { newPassword })

/**
 * @description 更新用户状态（启用/禁用）
 * @param {number|string} id - 用户ID
 * @param {number} status - 新状态值（0:禁用, 1:启用）
 * @returns {Promise}
 */
export const updateUserStatus = (id, status) => request.put(`/users/${id}/status`, null, { params: { status } })

// ====== 操作日志接口 ======

/**
 * @description 获取操作日志列表（分页）
 * @param {object} params - 查询参数：pageNum, pageSize
 * @returns {Promise} 分页数据
 */
export const getLogs = (params) => request.get('/logs', { params })

// ====== 分期付款接口 ======

/**
 * @description 创建分期计划
 * @param {object} data - 分期计划信息：{ studentId, contractId, totalAmount, totalPeriods }
 * @returns {Promise} 创建结果
 */
export const createInstallmentPlan = (data) => request.post('/finance/installments/plan', data)

/**
 * @description 获取学员分期计划
 * @param {number|string} studentId - 学员ID
 * @returns {Promise} 学员的分期计划列表
 */
export const getStudentInstallments = (studentId) => request.get(`/finance/installments/plan/student/${studentId}`)

/**
 * @description 获取分期明细
 * @param {number|string} planId - 分期计划ID
 * @returns {Promise} 分期明细列表
 */
export const getInstallmentDetails = (planId) => request.get(`/finance/installments/detail/${planId}`)

/**
 * @description 支付某期分期
 * @param {number|string} detailId - 分期明细ID
 * @returns {Promise} 支付结果
 */
export const payInstallment = (detailId) => request.post(`/finance/installments/detail/${detailId}/pay`)

// ====== 教练绩效接口 ======

/**
 * @description 计算月度绩效
 * @param {string} month - 月份（YYYY-MM 格式）
 * @returns {Promise} 计算结果
 */
export const calculatePerformance = (month) => request.post('/coaches/performance/calculate', null, { params: { month } })

/**
 * @description 获取绩效排名
 * @param {string} month - 月份（YYYY-MM 格式）
 * @returns {Promise} 绩效排名列表
 */
export const getPerformanceRanking = (month) => request.get('/coaches/performance/ranking', { params: { month } })

/**
 * @description 获取教练绩效详情
 * @param {number|string} coachId - 教练ID
 * @returns {Promise} 教练绩效详情
 */
export const getCoachPerformance = (coachId) => request.get(`/coaches/performance/coach/${coachId}`)

// ====== 招生渠道接口 ======

/**
 * @description 获取渠道列表（分页）
 * @param {object} params - 查询参数：name, type, pageNum, pageSize
 * @returns {Promise} 分页数据
 */
export const getChannels = (params) => request.get('/channels', { params })

/**
 * @description 创建招生渠道
 * @param {object} data - 渠道信息：{ name, type, contactPerson, contactPhone, cost, remark }
 * @returns {Promise} 创建结果
 */
export const createChannel = (data) => request.post('/channels', data)

/**
 * @description 绑定学员到渠道
 * @param {object} data - { channelId, studentId }
 * @returns {Promise} 绑定结果
 */
export const bindChannelStudent = (data) => request.post('/channels/bind', data)

/**
 * @description 获取渠道统计数据
 * @returns {Promise} 各渠道招生数量和转化率
 */
export const getChannelStats = () => request.get('/channels/stats')

// ====== 在线刷题接口 ======

/**
 * @description 获取随机题目（在线刷题）
 * @param {object} params - { subject: number, count: number }
 * @returns {Promise} 题目列表
 */
export const getPracticeQuestions = (params) => request.get('/practice/questions', { params })

/**
 * @description 提交答题答案
 * @param {object} data - { questionId: number, studentAnswer: string }
 * @returns {Promise} 答题结果（正确/错误 + 解析）
 */
export const submitPracticeAnswer = (data) => request.post('/practice/answer', data)

/**
 * @description 获取刷题统计数据
 * @returns {Promise} { totalAnswered, correctCount, wrongCount, accuracy }
 */
export const getPracticeStats = () => request.get('/practice/stats')

/**
 * @description 获取错题本列表
 * @returns {Promise} 错题列表
 */
export const getWrongQuestions = () => request.get('/practice/wrong-questions')

// ====== 满意度调查接口 ======

/**
 * @description 提交满意度调查
 * @param {object} data - { coachId, teachingQuality, serviceAttitude, teachingEnvironment, overallRating, comment, suggestion }
 * @returns {Promise}
 */
export const submitSurvey = (data) => request.post('/surveys', data)

/**
 * @description 获取教练评价列表
 * @param {number|string} coachId - 教练ID
 * @param {object} params - 分页参数
 * @returns {Promise} 评价列表
 */
export const getCoachSurveys = (coachId, params) => request.get(`/surveys/coach/${coachId}`, { params })

/**
 * @description 获取教练平均评分
 * @param {number|string} coachId - 教练ID
 * @returns {Promise} 平均评分数据
 */
export const getCoachAvgRating = (coachId) => request.get(`/surveys/coach/${coachId}/avg`)

// ====== 学员附件接口 ======

/**
 * @description 上传学员附件
 * @param {number|string} studentId - 学员ID
 * @param {FormData} formData - 包含 file, attachmentType, remark 的表单数据
 * @returns {Promise}
 */
export const uploadAttachment = (studentId, formData) =>
  request.post(`/students/${studentId}/attachments/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

/**
 * @description 获取学员附件列表
 * @param {number|string} studentId - 学员ID
 * @returns {Promise} 附件列表
 */
export const getAttachments = (studentId) => request.get(`/students/${studentId}/attachments`)

/**
 * @description 删除学员附件
 * @param {number|string} studentId - 学员ID
 * @param {number|string} attachmentId - 附件ID
 * @returns {Promise}
 */
export const deleteAttachment = (studentId, attachmentId) =>
  request.delete(`/students/${studentId}/attachments/${attachmentId}`)

// ====== 报名审核接口 ======

/**
 * @description 获取报名列表（分页）
 * @param {object} params - 查询参数：pageNum, pageSize, status, keyword
 * @returns {Promise} 分页数据
 */
export const getRegistrations = (params) => request.get('/registrations', { params })

/**
 * @description 获取报名统计数据
 * @returns {Promise} { total, pending, approved, rejected }
 */
export const getRegistrationStats = () => request.get('/registrations/stats')

/**
 * @description 审核报名
 * @param {number|string} id - 报名ID
 * @param {object} data - { approved: boolean, comment: string }
 * @returns {Promise}
 */
export const reviewRegistration = (id, data) => request.put(`/registrations/${id}/review`, data)

// ====== 学时打卡接口 ======

/**
 * @description 学员签到
 * @param {number|string} lessonId - 课时ID
 * @param {object} data - { studentId, coachId?, location?, checkInType? }
 * @returns {Promise} 签到记录
 */
export const checkIn = (lessonId, data) => request.post(`/check-in/${lessonId}/check-in`, data)

/**
 * @description 学员签退
 * @param {number|string} lessonId - 课时ID
 * @param {object} data - { studentId }
 * @returns {Promise} 签退记录
 */
export const checkOut = (lessonId, data) => request.put(`/check-in/${lessonId}/check-out`, data)

/**
 * @description 获取指定课时的签到记录
 * @param {number|string} lessonId - 课时ID
 * @returns {Promise} 签到记录列表
 */
export const getCheckInRecords = (lessonId) => request.get(`/check-in/${lessonId}`)

// ====== AI 助手接口 ======

/**
 * @description 获取用户的会话列表
 * @returns {Promise} 会话列表
 */
export const getAiConversations = () => request.get('/ai/conversations')

/**
 * @description 获取可用模型列表
 * @returns {Promise} 模型列表 [{id, name, description}]
 */
export const getAiModels = () => request.get('/ai/models')

/**
 * @description 获取指定会话的聊天历史
 * @param {string} conversationId - 会话ID
 * @returns {Promise} 聊天记录列表
 */
export const getAiConversationHistory = (conversationId) => request.get(`/ai/conversations/${conversationId}`)

/**
 * @description 删除会话
 * @param {string} conversationId - 会话ID
 * @returns {Promise}
 */
export const deleteAiConversation = (conversationId) => request.delete(`/ai/conversations/${conversationId}`)
