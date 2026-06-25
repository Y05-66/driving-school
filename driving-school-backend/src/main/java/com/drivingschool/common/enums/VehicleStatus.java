package com.drivingschool.common.enums;

import lombok.Getter;

/**
 * 车辆状态枚举。
 * <p>
 * 定义驾校车辆的所有可能状态，用于车辆管理和排课调度。
 * 只有可用(AVAILABLE)状态的车辆才能被安排到课程中。
 * </p>
 */
@Getter
public enum VehicleStatus {

    /** 维修中：车辆正在维修或保养，暂时不能使用 */
    MAINTENANCE(0, "维修中"),

    /** 可用：车辆状态正常，可以安排到练车课程中 */
    AVAILABLE(1, "可用"),

    /** 报废：车辆已报废，不再使用 */
    SCRAPPED(2, "报废");

    /** 状态编码，存储在数据库中 */
    private final int code;

    /** 状态中文名称，用于前端展示 */
    private final String name;

    VehicleStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }
}
