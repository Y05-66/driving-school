package com.drivingschool.common.enums;

import lombok.Getter;

/**
 * 收费类型枚举。
 * <p>
 * 定义驾校业务中的所有收费项目类型，用于财务管理和收费记录。
 * </p>
 */
@Getter
public enum PaymentType {

    /** 报名费：学员初次报名时缴纳的基础费用 */
    REGISTRATION(1, "报名费"),

    /** 课时费：学员每次练车课程产生的费用 */
    LESSON(2, "课时费"),

    /** 补考费：学员考试未通过后重新报名考试的费用 */
    RETAKE(3, "补考费"),

    /** 其他：不属于以上类别的其他收费项目（如教材费、体检费等） */
    OTHER(4, "其他");

    /** 类型编码，存储在数据库中 */
    private final int code;

    /** 类型中文名称，用于前端展示 */
    private final String name;

    PaymentType(int code, String name) {
        this.code = code;
        this.name = name;
    }
}
