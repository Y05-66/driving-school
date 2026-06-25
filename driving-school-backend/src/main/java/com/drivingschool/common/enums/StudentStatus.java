package com.drivingschool.common.enums;

import lombok.Getter;

/**
 * 学员状态枚举。
 * <p>
 * 定义学员在系统中的所有可能状态，用于学员管理业务流程控制。
 * 学员状态流转：待审核(PENDING) -> 在学(STUDYING) -> 已毕业(GRADUATED)
 * 也可能中途退学(DROPPED)。
 * </p>
 */
@Getter
public enum StudentStatus {

    /** 待审核：学员已提交报名信息，等待管理员审核通过 */
    PENDING(0, "待审核"),

    /** 在学：学员已通过审核，正在驾校学习中 */
    STUDYING(1, "在学"),

    /** 已毕业：学员已完成所有培训课程和考试，顺利结业 */
    GRADUATED(2, "已毕业"),

    /** 已退学：学员主动退学或因其他原因退出培训 */
    DROPPED(3, "已退学");

    /** 状态编码，存储在数据库中 */
    private final int code;

    /** 状态中文名称，用于前端展示 */
    private final String name;

    StudentStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据状态编码获取对应的枚举实例。
     *
     * @param code 状态编码
     * @return 对应的 StudentStatus 枚举实例
     * @throws IllegalArgumentException 当编码无效时抛出异常
     */
    public static StudentStatus of(int code) {
        for (StudentStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Invalid student status: " + code);
    }
}
