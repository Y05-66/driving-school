package com.drivingschool.common.enums;

import lombok.Getter;

/**
 * 教练状态枚举。
 * <p>
 * 定义教练在系统中的所有可能状态，用于教练管理和排课调度。
 * 状态流转：在岗(ACTIVE) 和 休假(ON_LEAVE) 之间可以相互切换，
 * 离职(RESIGNED) 为终态。
 * </p>
 */
@Getter
public enum CoachStatus {

    /** 休假：教练暂时不在岗，不能安排新课程 */
    ON_LEAVE(0, "休假"),

    /** 在岗：教练正常工作，可以接受课程安排 */
    ACTIVE(1, "在岗"),

    /** 离职：教练已离职，不再参与任何业务 */
    RESIGNED(2, "离职");

    /** 状态编码，存储在数据库中 */
    private final int code;

    /** 状态中文名称，用于前端展示 */
    private final String name;

    CoachStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }
}
