package com.drivingschool.statistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drivingschool.coach.entity.Coach;
import com.drivingschool.coach.mapper.CoachMapper;
import com.drivingschool.course.entity.Lesson;
import com.drivingschool.course.mapper.LessonMapper;
import com.drivingschool.exam.mapper.ExamEnrollmentMapper;
import com.drivingschool.finance.entity.Payment;
import com.drivingschool.finance.mapper.PaymentMapper;
import com.drivingschool.notification.mapper.NotificationMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import com.drivingschool.student.entity.Student;
import com.drivingschool.student.entity.StudentProgress;
import com.drivingschool.student.mapper.StudentMapper;
import com.drivingschool.student.mapper.StudentProgressMapper;
import com.drivingschool.vehicle.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计分析服务类
 * <p>
 * 负责驾校运营数据的统计与分析，包括：
 * - 系统总览数据（学员总数、本月收入、通过率等）
 * - 收入趋势分析（按月/按季度）
 * - 学员报名趋势分析
 * - 考试通过率统计（按科目/教练分组）
 * - 教练排名（按评分排序，含学员数统计）
 * - 车辆使用率统计
 * - 各角色仪表盘数据（管理员、工作人员、教练、学员）
 * - 数据导出（学员列表、财务数据）
 * </p>
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    /** 学员数据访问层 */
    private final StudentMapper studentMapper;
    /** 支付记录数据访问层 */
    private final PaymentMapper paymentMapper;
    /** 考试报名数据访问层 */
    private final ExamEnrollmentMapper enrollmentMapper;
    /** 教练数据访问层 */
    private final CoachMapper coachMapper;
    /** 课程预约数据访问层 */
    private final LessonMapper lessonMapper;
    /** 车辆数据访问层 */
    private final VehicleMapper vehicleMapper;
    /** 学员进度数据访问层 */
    private final StudentProgressMapper studentProgressMapper;
    /** 通知数据访问层 */
    private final NotificationMapper notificationMapper;

    /**
     * 获取系统总览数据
     * <p>
     * 返回内容包括：
     * - totalStudents：学员总数
     * - studyingStudents：在学学员数（状态为1）
     * - monthRevenue：本月收入总额
     * - passRate：考试通过率（百分比，保留两位小数）
     * </p>
     *
     * @return 包含总览数据的Map
     */
    @Cacheable(value = "overview", key = "'all'")
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new HashMap<>();

        // 学员统计
        long totalStudents = studentMapper.selectCount(null);
        long studyingStudents = studentMapper.countByStatus(1);

        // 本月至今收入统计（从本月1日0点到明天0点，包含今天全天）
        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = today.plusDays(1).atStartOfDay();
        BigDecimal monthRevenue = paymentMapper.sumAmountByDateRange(monthStart, monthEnd);

        // 考试通过率统计
        long totalEnrollments = enrollmentMapper.selectCount(null);
        long passedCount = enrollmentMapper.countByStatus(1);
        double passRate = totalEnrollments == 0 ? 0 : (double) passedCount / totalEnrollments * 100;

        overview.put("totalStudents", totalStudents);
        overview.put("monthRevenue", monthRevenue);
        overview.put("studyingStudents", studyingStudents);
        // 保留两位小数
        overview.put("passRate", Math.round(passRate * 100.0) / 100.0);

        return overview;
    }

    /**
     * 获取收入趋势数据
     * <p>
     * 根据period参数返回不同粒度的趋势数据：
     * - "quarter"：按季度汇总，格式如"2024Q1"
     * - 其他值或null：按月汇总，格式如"2024-01"
     * </p>
     *
     * @param period 统计粒度（"quarter"表示按季度，其他值按月）
     * @return 趋势数据列表，每项包含period和amount
     */
    @Cacheable(value = "revenueTrend", key = "#period")
    public List<Map<String, Object>> getRevenueTrend(String period) {
        // 先从数据库获取按月分组的收入数据
        List<Map<String, Object>> monthlyData = paymentMapper.sumGroupByMonth();

        // 如果是按季度统计，将月度数据聚合为季度数据
        if ("quarter".equals(period)) {
            Map<String, BigDecimal> quarterly = new LinkedHashMap<>();
            for (Map<String, Object> row : monthlyData) {
                String monthKey = (String) row.get("period");
                int year = Integer.parseInt(monthKey.substring(0, 4));
                int month = Integer.parseInt(monthKey.substring(5, 7));
                // 计算季度标识：(月份-1)/3 + 1
                String quarterKey = year + "Q" + ((month - 1) / 3 + 1);
                quarterly.merge(quarterKey, (BigDecimal) row.get("amount"), BigDecimal::add);
            }
            return quarterly.entrySet().stream()
                    .map(e -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("period", e.getKey());
                        item.put("amount", e.getValue());
                        return item;
                    })
                    .collect(Collectors.toList());
        }

        // 按月返回
        return monthlyData.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("period", row.get("period"));
                    item.put("amount", row.get("amount"));
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取学员报名趋势数据（按月统计新增学员数）
     *
     * @return 趋势数据列表，每项包含period（月份）和count（新增人数）
     */
    @Cacheable(value = "enrollmentTrend", key = "'all'")
    public List<Map<String, Object>> getEnrollmentTrend() {
        return studentMapper.countGroupByMonth().stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("period", row.get("period"));
                    item.put("count", row.get("count"));
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取考试通过率统计
     * <p>
     * 根据groupBy参数按不同维度统计通过率：
     * - 默认按考试（科目）分组
     * - "coach"按教练分组
     * 返回值为key-value形式，key为分组名称，value为通过率百分比（保留两位小数）
     * </p>
     *
     * @param groupBy 分组维度（"coach"按教练分组，其他值按考试/科目分组）
     * @return 分组名称与通过率的Map
     */
    @Cacheable(value = "passRate", key = "#groupBy")
    public Map<String, Object> getPassRate(String groupBy) {
        List<Map<String, Object>> data = enrollmentMapper.countGroupByExamAndStatus();
        // grouped的value为long[2]：[0]=总数，[1]=通过数
        Map<String, long[]> grouped = new HashMap<>();

        for (Map<String, Object> row : data) {
            // 按考试类型分组（examId 对应考试安排，examType 为科目类型）
            String key = "科目" + row.get("examId");
            long count = ((Number) row.get("count")).longValue();
            int status = ((Number) row.get("status")).intValue();

            long[] arr = grouped.computeIfAbsent(key, k -> new long[]{0, 0});
            arr[0] += count; // 累加总数
            if (status == 1) {
                arr[1] += count; // 累加通过数
            }
        }

        // 计算各分组的通过率
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, long[]> entry : grouped.entrySet()) {
            long total = entry.getValue()[0];
            long passed = entry.getValue()[1];
            double rate = total == 0 ? 0 : (double) passed / total * 100;
            result.put(entry.getKey(), Math.round(rate * 100.0) / 100.0);
        }

        return result;
    }

    /**
     * 获取教练排名列表
     * <p>
     * 排名数据包括：
     * - 教练基本信息（ID、姓名、评分、经验年限）
     * - 关联学员数（通过课程预约记录统计）
     * - 按评分降序排列
     * </p>
     *
     * @return 教练排名数据列表
     */
    @Cacheable(value = "coachRanking", key = "'all'")
    public List<Map<String, Object>> getCoachRanking() {
        // 查询所有状态为启用的教练
        List<Coach> coaches = coachMapper.selectList(
                new LambdaQueryWrapper<Coach>().eq(Coach::getStatus, 1)
        );
        if (coaches.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询所有教练的学员数（避免 N+1 查询）
        List<Long> coachIds = coaches.stream().map(Coach::getId).collect(Collectors.toList());
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Lesson> qw = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        qw.select("coach_id", "COUNT(DISTINCT student_id) as studentCount")
          .in("coach_id", coachIds)
          .ne("status", 4)
          .groupBy("coach_id");
        List<Map<String, Object>> coachStudentCounts = lessonMapper.selectMaps(qw);
        // 构建 coachId -> studentCount 映射
        Map<Long, Long> studentCountMap = new HashMap<>();
        for (Map<String, Object> row : coachStudentCounts) {
            Long coachId = ((Number) row.get("coach_id")).longValue();
            Long count = ((Number) row.get("studentCount")).longValue();
            studentCountMap.put(coachId, count);
        }

        return coaches.stream()
                .map(c -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("coachId", c.getId());
                    item.put("name", c.getName());
                    item.put("rating", c.getRating());
                    item.put("experienceYears", c.getExperienceYears());
                    item.put("studentCount", studentCountMap.getOrDefault(c.getId(), 0L));
                    return item;
                })
                // 按评分降序排列，评分为空的排在最后
                .sorted((a, b) -> {
                    BigDecimal ra = (BigDecimal) a.get("rating");
                    BigDecimal rb = (BigDecimal) b.get("rating");
                    if (ra == null && rb == null) return 0;
                    if (ra == null) return 1;
                    if (rb == null) return -1;
                    return rb.compareTo(ra);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取车辆使用率统计
     * <p>
     * 返回内容包括：
     * - total：车辆总数
     * - available：可用车辆数（状态为1）
     * - maintenance：维修中车辆数（状态为0）
     * - usageRate：使用率百分比（可用数/总数，保留两位小数）
     * </p>
     *
     * @return 包含车辆使用统计数据的Map
     */
    public Map<String, Object> getVehicleUsage() {
        long totalVehicles = vehicleMapper.selectCount(null);
        long availableVehicles = vehicleMapper.countByStatus(1);
        long maintenanceVehicles = vehicleMapper.countByStatus(0);

        Map<String, Object> usage = new HashMap<>();
        usage.put("total", totalVehicles);
        usage.put("available", availableVehicles);
        usage.put("maintenance", maintenanceVehicles);
        // 使用率 = 可用数/总数 * 100%，保留两位小数
        usage.put("usageRate", totalVehicles == 0 ? 0 : Math.round((double) availableVehicles / totalVehicles * 10000.0) / 100.0);

        return usage;
    }

    /**
     * 根据角色返回对应的仪表盘数据
     * <p>
     * 不同角色看到的仪表盘内容不同：
     * - ADMIN（管理员）：总览 + 车辆使用率 + 今日课程数 + 待处理考试
     * - STAFF（工作人员）：今日课程数 + 待审核学员 + 学员总数 + 未读通知
     * - COACH（教练）：教练信息 + 评分 + 今日课程数 + 名下学员数
     * - STUDENT（学员）：个人信息 + 科目进度 + 待上课时 + 已完成课时
     * </p>
     *
     * @param role   用户角色
     * @param userId 用户ID（教练和学员需要用于查询关联数据）
     * @return 仪表盘数据Map，角色无效时返回空Map
     */
    @Cacheable(value = "dashboard", key = "#role + ':' + #userId")
    public Map<String, Object> getDashboard(String role, Long userId) {
        if (role == null) return new HashMap<>();
        return switch (role) {
            case "ADMIN" -> getAdminDashboard();
            case "STAFF" -> getStaffDashboard();
            case "COACH" -> getCoachDashboard(userId);
            case "STUDENT" -> getStudentDashboard(userId);
            default -> new HashMap<>();
        };
    }

    /**
     * 管理员仪表盘（私有方法）
     * 包含：系统总览、车辆使用率、今日课程数、待处理考试数
     */
    private Map<String, Object> getAdminDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("overview", getOverview());
        dashboard.put("vehicleUsage", getVehicleUsage());
        dashboard.put("todayLessons", getTodayLessonCount());
        dashboard.put("pendingExams", getPendingExamCount());
        return dashboard;
    }

    /**
     * 工作人员仪表盘（私有方法）
     * 包含：今日课程数、待审核学员数、学员总数、未读通知数
     */
    private Map<String, Object> getStaffDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("todayLessons", getTodayLessonCount());
        dashboard.put("pendingStudents", studentMapper.countByStatus(0));
        dashboard.put("totalStudents", studentMapper.selectCount(null));
        dashboard.put("unreadNotifications", notificationMapper.countUnread(null, "STAFF"));
        return dashboard;
    }

    /**
     * 教练仪表盘（私有方法）
     * 包含：教练基本信息、评分、今日课程数、名下学员数
     */
    private Map<String, Object> getCoachDashboard(Long userId) {
        Map<String, Object> dashboard = new HashMap<>();
        // 根据用户ID查找教练信息
        Coach coach = coachMapper.selectOne(
                new LambdaQueryWrapper<Coach>().eq(Coach::getUserId, userId)
        );
        if (coach == null) {
            return dashboard;
        }

        dashboard.put("coachId", coach.getId());
        dashboard.put("coachName", coach.getName());
        dashboard.put("rating", coach.getRating());

        // 今日课程数（排除已取消的预约）
        long todayLessons = lessonMapper.selectCount(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getCoachId, coach.getId())
                        .eq(Lesson::getLessonDate, LocalDate.now())
                        .ne(Lesson::getStatus, 4)
        );
        dashboard.put("todayLessons", todayLessons);

        // 名下学员数（通过预约记录按学员ID去重统计）
        List<Object> studentIds = lessonMapper.selectObjs(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getCoachId, coach.getId())
                        .ne(Lesson::getStatus, 4)
                        .groupBy(Lesson::getStudentId)
                        .select(Lesson::getStudentId)
        );
        dashboard.put("studentCount", studentIds.size());

        return dashboard;
    }

    /**
     * 学员仪表盘（私有方法）
     * 包含：学员个人信息、各科进度、待上课时数、已完成课时数
     */
    private Map<String, Object> getStudentDashboard(Long userId) {
        Map<String, Object> dashboard = new HashMap<>();
        // 根据用户ID查找学员信息
        Student student = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId)
        );
        if (student == null) {
            return dashboard;
        }

        dashboard.put("studentId", student.getId());
        dashboard.put("studentName", student.getName());
        dashboard.put("status", student.getStatus());
        dashboard.put("applyType", student.getApplyType());

        // 查询各科目学习进度
        List<StudentProgress> progressList = studentProgressMapper.selectList(
                new LambdaQueryWrapper<StudentProgress>()
                        .eq(StudentProgress::getStudentId, student.getId())
                        .orderByAsc(StudentProgress::getSubjectName)
        );
        dashboard.put("progress", progressList);

        // 待上课时数（状态为0-待确认或1-已确认的预约）
        long pendingLessons = lessonMapper.selectCount(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getStudentId, student.getId())
                        .le(Lesson::getStatus, 1)
        );
        dashboard.put("pendingLessons", pendingLessons);

        // 已完成课时数（状态为3-已完成）
        long completedLessons = lessonMapper.selectCount(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getStudentId, student.getId())
                        .eq(Lesson::getStatus, 3)
        );
        dashboard.put("completedLessons", completedLessons);

        return dashboard;
    }

    /**
     * 获取今日课程数量（私有方法）
     * 统计今天所有未取消的课程预约数
     */
    private long getTodayLessonCount() {
        return lessonMapper.selectCount(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getLessonDate, LocalDate.now())
                        .ne(Lesson::getStatus, 4)
        );
    }

    /**
     * 获取待处理考试报名数量（私有方法）
     * 统计状态为0（待审核）的考试报名记录数
     */
    private long getPendingExamCount() {
        return enrollmentMapper.selectCount(
                new LambdaQueryWrapper<com.drivingschool.exam.entity.ExamEnrollment>()
                        .eq(com.drivingschool.exam.entity.ExamEnrollment::getStatus, 0)
        );
    }

    /**
     * 导出学员列表数据（供Excel使用）
     * <p>
     * 支持按姓名、状态、报名类型筛选，导出字段包括：
     * ID、姓名、身份证号、电话、性别、报名类型、状态、注册日期
     * </p>
     *
     * @param name      学员姓名（模糊查询，可选）
     * @param status    学员状态（可选）
     * @param applyType 报名类型（可选）
     * @return 学员数据列表，每项为一个LinkedHashMap保持字段顺序
     */
    public List<Map<String, Object>> getStudentExportData(String name, Integer status, String applyType) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(name != null, Student::getName, name)
                .eq(status != null, Student::getStatus, status)
                .eq(applyType != null, Student::getApplyType, applyType)
                .orderByDesc(Student::getCreateTime);
        List<Student> students = studentMapper.selectList(wrapper);

        // 将学员实体转换为导出用的Map格式
        return students.stream().map(s -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ID", s.getId());
            row.put("姓名", s.getName());
            row.put("身份证号", s.getIdCard());
            row.put("电话", s.getPhone());
            row.put("性别", s.getGender() != null ? (s.getGender() == 1 ? "男" : "女") : "");
            row.put("报名类型", s.getApplyType());
            row.put("状态", getStatusName(s.getStatus()));
            row.put("注册日期", s.getRegisterDate());
            return row;
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 导出财务数据（供Excel使用）
     * <p>
     * 支持按日期范围筛选，导出字段包括：
     * 单号、学员ID、类型、金额、支付方式、支付时间、退费状态、备注
     * </p>
     *
     * @param startDate 开始日期（可选）
     * @param endDate   结束日期（可选）
     * @return 财务数据列表，每项为一个LinkedHashMap保持字段顺序
     */
    public List<Map<String, Object>> getFinanceExportData(LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null) {
            wrapper.ge(Payment::getPayTime, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(Payment::getPayTime, endDate.atTime(23, 59, 59));
        }
        wrapper.orderByDesc(Payment::getPayTime);
        List<com.drivingschool.finance.entity.Payment> payments = paymentMapper.selectList(wrapper);

        // 将支付记录转换为导出用的Map格式
        return payments.stream().map(p -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("单号", p.getReceiptNo());
            row.put("学员ID", p.getStudentId());
            row.put("类型", getPaymentTypeName(p.getType()));
            row.put("金额", p.getAmount());
            row.put("支付方式", getPayMethodName(p.getPayMethod()));
            row.put("支付时间", p.getPayTime());
            row.put("退费状态", p.getRefundStatus() != null && p.getRefundStatus() == 1 ? "已退费" : "正常");
            row.put("备注", p.getRemark());
            return row;
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 学员状态码转中文名称（私有方法）
     * 0-待审核 1-在学 2-已毕业 3-已退学
     */
    private String getStatusName(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "在学";
            case 2 -> "已毕业";
            case 3 -> "已退学";
            default -> "未知";
        };
    }

    /**
     * 支付类型码转中文名称（私有方法）
     * 1-报名费 2-课时费 3-补考费 4-其他
     */
    private String getPaymentTypeName(Integer type) {
        if (type == null) return "";
        return switch (type) {
            case 1 -> "报名费";
            case 2 -> "课时费";
            case 3 -> "补考费";
            case 4 -> "其他";
            default -> "未知";
        };
    }

    /**
     * 支付方式码转中文名称（私有方法）
     * 1-现金 2-微信 3-支付宝 4-银行卡
     */
    private String getPayMethodName(Integer method) {
        if (method == null) return "";
        return switch (method) {
            case 1 -> "现金";
            case 2 -> "微信";
            case 3 -> "支付宝";
            case 4 -> "银行卡";
            default -> "未知";
        };
    }
}
