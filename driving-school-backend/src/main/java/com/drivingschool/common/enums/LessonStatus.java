package com.drivingschool.common.enums;

import lombok.Getter;

/**
 * 课程（课时）状态枚举。
 * <p>
 * 定义练车课程的所有可能状态，用于课程预约和管理业务流程。
 * 状态流转：待确认(PENDING) -> 已确认(CONFIRMED) -> 进行中(IN_PROGRESS) -> 已完成(COMPLETED)
 * 课程也可能被取消(CANCELLED)。
 * </p>
 */
@Getter
public enum LessonStatus {

    /** 待确认：学员已预约课程，等待教练或系统确认 */
    PENDING(0, "待确认"),

    /** 已确认：教练已确认课程安排，等待上课 */
    CONFIRMED(1, "已确认"),

    /** 进行中：学员正在练车过程中 */
    IN_PROGRESS(2, "进行中"),

    /** 已完成：本次课程已结束 */
    COMPLETED(3, "已完成"),

    /** 已取消：课程被取消（可能由学员或教练取消） */
    CANCELLED(4, "已取消");

    /** 状态编码，存储在数据库中 */
    private final int code;

    /** 状态中文名称，用于前端展示 */
    private final String name;

    LessonStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }
}
