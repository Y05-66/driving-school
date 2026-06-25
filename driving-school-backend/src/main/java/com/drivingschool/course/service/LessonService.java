package com.drivingschool.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.constant.Constants;
import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.coach.entity.Coach;
import com.drivingschool.coach.mapper.CoachMapper;
import com.drivingschool.course.entity.Course;
import com.drivingschool.course.entity.Lesson;
import com.drivingschool.course.entity.TimeSlotTemplate;
import com.drivingschool.course.mapper.CourseMapper;
import com.drivingschool.course.mapper.LessonMapper;
import com.drivingschool.course.mapper.TimeSlotTemplateMapper;
import com.drivingschool.student.entity.Student;
import com.drivingschool.student.mapper.StudentMapper;
import com.drivingschool.student.service.StudentProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 课程预约服务类
 * <p>
 * 负责驾校课程预约的全生命周期管理，包括：
 * - 课程预约的创建、确认、完成、取消等状态流转
 * - 预约冲突检测（教练、车辆、学员三方时间冲突校验）
 * - 每日课时上限控制
 * - 学员课时完成后自动更新学员科目进度
 * - 基于时间段模板的可预约时段查询
 * - 批量创建连续预约
 * - 时间段模板的增删改查管理
 * - 日历视图数据查询
 * </p>
 */
@Service
@RequiredArgsConstructor
public class LessonService {

    /** 课程预约数据访问层 */
    private final LessonMapper lessonMapper;
    /** 学员数据访问层 */
    private final StudentMapper studentMapper;
    /** 教练数据访问层 */
    private final CoachMapper coachMapper;
    /** 课程数据访问层 */
    private final CourseMapper courseMapper;
    /** 时间段模板数据访问层 */
    private final TimeSlotTemplateMapper timeSlotTemplateMapper;
    /** 学员进度服务（用于课时完成后更新学员科目进度） */
    private final StudentProgressService studentProgressService;

    /**
     * 分页查询课程预约列表
     *
     * @param page        分页参数对象
     * @param studentId   学员ID（可选筛选条件）
     * @param coachId     教练ID（可选筛选条件）
     * @param vehicleId   车辆ID（可选筛选条件）
     * @param status      预约状态（可选筛选条件：0-待确认 1-已确认 2-进行中 3-已完成 4-已取消）
     * @param lessonDate  课程日期（可选筛选条件）
     * @return 分页结果，按课程日期降序、开始时间升序排列
     */
    public Page<Lesson> page(Page<Lesson> page, Long studentId, Long coachId, Long vehicleId,
                             Integer status, LocalDate lessonDate) {
        LambdaQueryWrapper<Lesson> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(studentId != null, Lesson::getStudentId, studentId)
                .eq(coachId != null, Lesson::getCoachId, coachId)
                .eq(vehicleId != null, Lesson::getVehicleId, vehicleId)
                .eq(status != null, Lesson::getStatus, status)
                .eq(lessonDate != null, Lesson::getLessonDate, lessonDate)
                .orderByDesc(Lesson::getLessonDate)
                .orderByAsc(Lesson::getStartTime);
        return lessonMapper.selectPage(page, wrapper);
    }

    /**
     * 根据ID获取课程预约详情
     *
     * @param id 预约记录ID
     * @return 预约实体，不存在时返回null
     */
    public Lesson getById(Long id) {
        return lessonMapper.selectById(id);
    }

    /**
     * 创建课程预约
     * <p>
     * 业务规则：
     * 1. 检测教练、车辆、学员的时间冲突
     * 2. 校验学员当日预约课时数不超过上限（Constants.MAX_LESSONS_PER_DAY）
     * 3. 新建预约默认状态为0（待确认）
     * </p>
     *
     * @param lesson 预约实体信息
     * @throws BusinessException 存在时间冲突或超出每日课时上限时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(Lesson lesson) {
        // 第一步：检测三方冲突（教练、车辆、学员在同一时间段是否已有预约）
        checkConflict(lesson);

        // 第二步：检查学员当日已预约课时数是否达到上限（排除已取消的预约）
        long dailyCount = lessonMapper.selectCount(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getStudentId, lesson.getStudentId())
                        .eq(Lesson::getLessonDate, lesson.getLessonDate())
                        .ne(Lesson::getStatus, 4)
        );
        if (dailyCount >= Constants.MAX_LESSONS_PER_DAY) {
            throw new BusinessException("每日最多" + Constants.MAX_LESSONS_PER_DAY + "课时");
        }

        // 第三步：设置初始状态为待确认并持久化
        lesson.setStatus(0);
        lessonMapper.insert(lesson);
    }

    /**
     * 确认预约（教练或管理员操作）
     * <p>
     * 仅允许状态为"待确认(0)"的预约被确认，确认后状态变更为"已确认(1)"
     * </p>
     *
     * @param id 预约记录ID
     * @throws BusinessException 预约不存在或状态不允许确认时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) {
            throw new BusinessException("预约不存在");
        }
        // 只有待确认状态才能执行确认操作
        if (lesson.getStatus() != 0) {
            throw new BusinessException("当前状态不允许确认");
        }
        lesson.setStatus(1);
        lessonMapper.updateById(lesson);
    }

    /**
     * 完成课时（需先确认再完成）
     * <p>
     * 仅允许状态为"已确认(1)"的预约被标记为完成，完成后状态变更为"已完成(3)"，
     * 并自动更新学员对应科目的学习进度（累加已学课时）
     * </p>
     *
     * @param id          预约记录ID
     * @param actualHours 实际授课时长（小时）
     * @throws BusinessException 预约不存在或状态不允许完成时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id, java.math.BigDecimal actualHours) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) {
            throw new BusinessException("预约不存在");
        }
        // 只有已确认状态才能标记完成
        if (lesson.getStatus() != 1) {
            throw new BusinessException("请先确认预约再完成");
        }
        lesson.setStatus(3);
        lesson.setActualHours(actualHours);
        lessonMapper.updateById(lesson);

        // 更新学员科目进度
        updateStudentProgress(lesson);
    }

    /**
     * 直接确认并完成课时（跳过中间确认状态，一步到位）
     * <p>
     * 仅允许状态为"待确认(0)"的预约直接标记为完成，
     * 适用于简化流程的场景（如教练直接确认并结束课程）
     * </p>
     *
     * @param id          预约记录ID
     * @param actualHours 实际授课时长（小时）
     * @throws BusinessException 预约不存在或状态不允许操作时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmAndComplete(Long id, java.math.BigDecimal actualHours) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) {
            throw new BusinessException("预约不存在");
        }
        // 只有待确认状态才能执行一步确认完成操作
        if (lesson.getStatus() != 0) {
            throw new BusinessException("当前状态不允许确认完成");
        }
        lesson.setStatus(3);
        lesson.setActualHours(actualHours);
        lessonMapper.updateById(lesson);

        // 更新学员科目进度
        updateStudentProgress(lesson);
    }

    /**
     * 更新学员科目学习进度（私有方法）
     * <p>
     * 当课时完成时，根据该课时所属的课程名称，自动累加学员对应科目的已完成学时。
     * 如果课程ID或学员ID为空则跳过；如果课程不存在也跳过。
     * 实际课时数以actualHours为准，默认为1小时。
     * </p>
     *
     * @param lesson 已完成的课程预约实体
     */
    private void updateStudentProgress(Lesson lesson) {
        if (lesson.getCourseId() == null || lesson.getStudentId() == null) {
            return;
        }
        Course course = courseMapper.selectById(lesson.getCourseId());
        if (course == null) {
            return;
        }
        // 获取实际课时数，若为空则默认为1小时
        // 使用CEILING向上取整：例如0.5小时按1小时计算，确保学员进度不会被低估
        int hours = lesson.getActualHours() != null ? lesson.getActualHours().setScale(0, java.math.RoundingMode.CEILING).intValue() : 1;
        studentProgressService.addHours(lesson.getStudentId(), course.getName(), hours);
    }

    /**
     * 取消预约
     * <p>
     * 仅允许状态小于3（即待确认、已确认、进行中）的预约被取消，
     * 已完成(3)或已取消(4)的预约不能再次取消
     * </p>
     *
     * @param id 预约记录ID
     * @throws BusinessException 预约不存在或状态不允许取消时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) {
            throw new BusinessException("预约不存在");
        }
        // 已完成或已取消的预约不允许取消
        if (lesson.getStatus() >= 3) {
            throw new BusinessException("当前状态不允许取消");
        }
        lesson.setStatus(4);
        lessonMapper.updateById(lesson);
    }

    /**
     * 获取日历视图数据
     * <p>
     * 查询指定日期范围内、指定教练或学员的课程预约列表（排除已取消的预约），
     * 用于前端日历组件展示
     * </p>
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param coachId   教练ID（可选，用于筛选特定教练）
     * @param studentId 学员ID（可选，用于筛选特定学员）
     * @return 预约列表，按日期升序、开始时间升序排列
     */
    public List<Lesson> getCalendarData(LocalDate startDate, LocalDate endDate, Long coachId, Long studentId) {
        LambdaQueryWrapper<Lesson> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Lesson::getLessonDate, startDate)
                .le(Lesson::getLessonDate, endDate)
                .eq(coachId != null, Lesson::getCoachId, coachId)
                .eq(studentId != null, Lesson::getStudentId, studentId)
                .ne(Lesson::getStatus, 4)
                .orderByAsc(Lesson::getLessonDate)
                .orderByAsc(Lesson::getStartTime);
        return lessonMapper.selectList(wrapper);
    }

    /**
     * 获取指定教练的所有有效预约列表
     *
     * @param coachId 教练ID
     * @return 预约列表（排除已取消），按日期升序、开始时间升序排列
     */
    public List<Lesson> getByCoachId(Long coachId) {
        LambdaQueryWrapper<Lesson> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Lesson::getCoachId, coachId)
                .ne(Lesson::getStatus, 4)
                .orderByAsc(Lesson::getLessonDate)
                .orderByAsc(Lesson::getStartTime);
        return lessonMapper.selectList(wrapper);
    }

    /**
     * 获取指定学员的所有有效预约列表
     *
     * @param studentId 学员ID
     * @return 预约列表（排除已取消），按日期降序、开始时间升序排列
     */
    public List<Lesson> getByStudentId(Long studentId) {
        LambdaQueryWrapper<Lesson> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Lesson::getStudentId, studentId)
                .ne(Lesson::getStatus, 4)
                .orderByDesc(Lesson::getLessonDate)
                .orderByAsc(Lesson::getStartTime);
        return lessonMapper.selectList(wrapper);
    }

    /**
     * 获取指定教练在指定日期的已有预约（用于查看可用时段）
     *
     * @param coachId 教练ID
     * @param date    查询日期
     * @return 该教练在指定日期的所有有效预约列表
     */
    public List<Lesson> getAvailableSlots(Long coachId, LocalDate date) {
        LambdaQueryWrapper<Lesson> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Lesson::getCoachId, coachId)
                .eq(Lesson::getLessonDate, date)
                .ne(Lesson::getStatus, 4);
        return lessonMapper.selectList(wrapper);
    }

    /**
     * 获取教练名下所有学员（通过课程预约记录关联查询）
     *
     * @param coachId 教练ID
     * @return 该教练关联的学员列表
     */
    public List<Student> getStudentsByCoachId(Long coachId) {
        // 查询该教练所有有效预约中不重复的学员ID
        List<Lesson> lessons = lessonMapper.selectList(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getCoachId, coachId)
                        .ne(Lesson::getStatus, 4)
                        .select(Lesson::getStudentId)
                        .groupBy(Lesson::getStudentId)
        );
        List<Long> studentIds = lessons.stream()
                .map(Lesson::getStudentId)
                .collect(Collectors.toList());
        if (studentIds.isEmpty()) return Collections.emptyList();
        return studentMapper.selectBatchIds(studentIds);
    }

    /**
     * 获取学员关联的教练（通过课程预约记录关联查询）
     * <p>
     * 返回该学员预约课时最多的教练（主教教练）。
     * 如学员有多位教练，则返回课时数最多的那位。
     * </p>
     *
     * @param studentId 学员ID
     * @return 教练实体，无关联教练时返回null
     */
    public Coach getCoachByStudentId(Long studentId) {
        // 查询该学员所有有效预约，按教练分组统计课时数，取课时最多的教练
        List<Lesson> allLessons = lessonMapper.selectList(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getStudentId, studentId)
                        .ne(Lesson::getStatus, 4)
                        .select(Lesson::getCoachId)
        );
        if (allLessons.isEmpty()) return null;

        // 找出课时数最多的教练ID
        Long mostFrequentCoachId = allLessons.stream()
                .filter(l -> l.getCoachId() != null)
                .collect(java.util.stream.Collectors.groupingBy(Lesson::getCoachId, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(null);

        if (mostFrequentCoachId == null) return null;
        return coachMapper.selectById(mostFrequentCoachId);
    }

    /**
     * 基于时间段模板查询可预约时段
     * <p>
     * 逻辑说明：
     * 1. 根据查询日期的星期几，获取当天适用的时段模板（模板中dayOfWeek字段包含该星期值）
     * 2. 查询指定教练在该日期的已有预约
     * 3. 遍历每个模板，计算已预约数量与剩余容量
     * 4. 仅返回仍有剩余容量的时段
     * </p>
     *
     * @param coachId 教练ID
     * @param date    查询日期
     * @return 可预约时段列表，每项包含模板ID、名称、开始/结束时间、最大容量、已预约数、剩余数
     */
    public List<Map<String, Object>> getAvailableTimeSlots(Long coachId, LocalDate date) {
        // 获取当天适用的模板（根据星期几匹配）
        int dayOfWeek = date.getDayOfWeek().getValue();
        List<TimeSlotTemplate> templates = timeSlotTemplateMapper.selectList(
                new LambdaQueryWrapper<TimeSlotTemplate>()
                        .eq(TimeSlotTemplate::getStatus, 1)
                        .like(TimeSlotTemplate::getDayOfWeek, String.valueOf(dayOfWeek))
        );

        // 获取当天已有预约（排除已取消）
        List<Lesson> existingLessons = lessonMapper.selectList(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getCoachId, coachId)
                        .eq(Lesson::getLessonDate, date)
                        .ne(Lesson::getStatus, 4)
        );

        // 遍历模板，计算每个时段的剩余容量
        List<Map<String, Object>> availableSlots = new ArrayList<>();
        for (TimeSlotTemplate template : templates) {
            // 统计与当前模板时间段有重叠的已预约数量
            long bookedCount = existingLessons.stream()
                    .filter(l -> l.getStartTime().isBefore(template.getEndTime()) &&
                                 l.getEndTime().isAfter(template.getStartTime()))
                    .count();

            int remaining = template.getMaxCapacity() - (int) bookedCount;
            if (remaining > 0) {
                Map<String, Object> slot = new HashMap<>();
                slot.put("templateId", template.getId());
                slot.put("name", template.getName());
                slot.put("startTime", template.getStartTime());
                slot.put("endTime", template.getEndTime());
                slot.put("maxCapacity", template.getMaxCapacity());
                slot.put("bookedCount", bookedCount);
                slot.put("remaining", remaining);
                availableSlots.add(slot);
            }
        }
        return availableSlots;
    }

    /**
     * 批量创建连续预约
     * <p>
     * 根据提供的日期列表，为每个日期创建一条预约记录，
     * 其余信息（学员、教练、车辆、课程、时间段、备注）复用传入的lesson对象
     * </p>
     *
     * @param lesson 预约模板信息（学员、教练、车辆等信息将被复用）
     * @param dates  需要创建预约的日期列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchCreate(Lesson lesson, List<LocalDate> dates) {
        // 验证日期列表
        if (dates == null || dates.isEmpty()) {
            throw new BusinessException("日期列表不能为空");
        }
        if (dates.size() > 30) {
            throw new BusinessException("单次批量创建不能超过30天");
        }
        LocalDate today = LocalDate.now();
        for (LocalDate date : dates) {
            if (date.isBefore(today)) {
                throw new BusinessException("不能预约过去的日期：" + date);
            }
        }

        for (LocalDate date : dates) {
            Lesson batchLesson = new Lesson();
            batchLesson.setStudentId(lesson.getStudentId());
            batchLesson.setCoachId(lesson.getCoachId());
            batchLesson.setVehicleId(lesson.getVehicleId());
            batchLesson.setCourseId(lesson.getCourseId());
            batchLesson.setLessonDate(date);
            batchLesson.setStartTime(lesson.getStartTime());
            batchLesson.setEndTime(lesson.getEndTime());
            batchLesson.setRemark(lesson.getRemark());
            create(batchLesson);
        }
    }

    /**
     * 获取所有时间段模板列表
     *
     * @return 模板列表，按开始时间升序排列
     */
    public List<TimeSlotTemplate> getTimeSlotTemplates() {
        return timeSlotTemplateMapper.selectList(
                new LambdaQueryWrapper<TimeSlotTemplate>()
                        .orderByAsc(TimeSlotTemplate::getStartTime)
        );
    }

    /**
     * 创建时间段模板
     * <p>
     * 新建模板默认状态为1（启用）
     * </p>
     *
     * @param template 模板实体
     */
    public void createTimeSlotTemplate(TimeSlotTemplate template) {
        template.setStatus(1);
        timeSlotTemplateMapper.insert(template);
    }

    /**
     * 更新时间段模板
     *
     * @param template 模板实体（需包含ID）
     */
    public void updateTimeSlotTemplate(TimeSlotTemplate template) {
        timeSlotTemplateMapper.updateById(template);
    }

    /**
     * 删除时间段模板
     *
     * @param id 模板ID
     */
    public void deleteTimeSlotTemplate(Long id) {
        timeSlotTemplateMapper.deleteById(id);
    }

    /**
     * 检查预约冲突（私有方法）
     * <p>
     * 依次检查三方冲突：
     * 1. 教练冲突：同一教练在同一日期、时间段有重叠的未取消预约
     * 2. 车辆冲突：同一车辆在同一日期、时间段有重叠的未取消预约（仅在指定了车辆ID时检查）
     * 3. 学员冲突：同一学员在同一日期、时间段有重叠的未取消预约
     * 时间段重叠判断逻辑：已有预约的开始时间 < 新预约的结束时间 AND 已有预约的结束时间 > 新预约的开始时间
     * </p>
     *
     * @param lesson 待创建的预约实体
     * @throws BusinessException 存在任何冲突时抛出，错误码1002
     */
    private void checkConflict(Lesson lesson) {
        // 检查教练时间冲突
        Long count = lessonMapper.selectCount(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getCoachId, lesson.getCoachId())
                        .eq(Lesson::getLessonDate, lesson.getLessonDate())
                        .ne(Lesson::getStatus, 4)
                        .lt(Lesson::getStartTime, lesson.getEndTime())
                        .gt(Lesson::getEndTime, lesson.getStartTime())
        );
        if (count > 0) {
            throw new BusinessException(1002, "教练此时段已有课程");
        }

        // 检查车辆时间冲突（仅在指定了车辆时检查）
        if (lesson.getVehicleId() != null) {
            count = lessonMapper.selectCount(
                    new LambdaQueryWrapper<Lesson>()
                            .eq(Lesson::getVehicleId, lesson.getVehicleId())
                            .eq(Lesson::getLessonDate, lesson.getLessonDate())
                            .ne(Lesson::getStatus, 4)
                            .lt(Lesson::getStartTime, lesson.getEndTime())
                            .gt(Lesson::getEndTime, lesson.getStartTime())
            );
            if (count > 0) {
                throw new BusinessException(1002, "车辆此时段已被占用");
            }
        }

        // 检查学员时间冲突
        count = lessonMapper.selectCount(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getStudentId, lesson.getStudentId())
                        .eq(Lesson::getLessonDate, lesson.getLessonDate())
                        .ne(Lesson::getStatus, 4)
                        .lt(Lesson::getStartTime, lesson.getEndTime())
                        .gt(Lesson::getEndTime, lesson.getStartTime())
        );
        if (count > 0) {
            throw new BusinessException(1002, "学员此时段已有课程");
        }
    }
}
