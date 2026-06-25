package com.drivingschool.common.enums;

import lombok.Getter;

/**
 * 考试报名状态枚举。
 * <p>
 * 定义学员考试报名的所有可能状态，用于考试管理和成绩记录。
 * 状态流转：待考(PENDING) -> 合格(PASSED) / 不合格(FAILED) / 缺考(ABSENT)
 * 不合格的学员可以重新报名参加补考。
 * </p>
 */
@Getter
public enum ExamEnrollStatus {

    /** 待考：学员已报名考试，尚未参加或成绩未出 */
    PENDING(0, "待考"),

    /** 合格：学员通过考试 */
    PASSED(1, "合格"),

    /** 不合格：学员未通过考试，可报名补考 */
    FAILED(2, "不合格"),

    /** 缺考：学员报名但未参加考试 */
    ABSENT(3, "缺考");

    /** 状态编码，存储在数据库中 */
    private final int code;

    /** 状态中文名称，用于前端展示 */
    private final String name;

    ExamEnrollStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }
}
