package com.drivingschool.common.constant;

/**
 * 系统业务常量
 * 集中管理所有跨模块使用的业务规则参数
 */
public class Constants {

    // ====== JWT 相关 ======

    /** Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** Redis 中 Token 黑名单的 key 前缀（登出时使用） */
    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /** Redis 中限流的 key 前缀（预留） */
    public static final String RATE_LIMIT_PREFIX = "rate_limit:";

    // ====== 教练-学员业务规则 ======

    /** 每个教练最多带多少学员 */
    public static final int MAX_STUDENTS_PER_COACH = 30;

    /** 每个学员每天最多预约课时数 */
    public static final int MAX_LESSONS_PER_DAY = 4;

    /** 取消预约需要提前多少小时 */
    public static final int CANCEL_HOURS_BEFORE = 2;

    // ====== 考试业务规则 ======

    /** 同科目最大补考次数 */
    public static final int MAX_RETAKE_COUNT = 5;

    /** 两次补考之间的最小间隔天数 */
    public static final int RETAKE_INTERVAL_DAYS = 10;

    // ====== 各科目及格分数 ======

    /** 科目一及格分（道路交通安全法律法规） */
    public static final int SUBJECT_1_PASS_SCORE = 90;

    /** 科目二及格分（场地驾驶技能） */
    public static final int SUBJECT_2_PASS_SCORE = 80;

    /** 科目三及格分（道路驾驶技能） */
    public static final int SUBJECT_3_PASS_SCORE = 90;

    /** 科目四及格分（安全文明驾驶常识） */
    public static final int SUBJECT_4_PASS_SCORE = 90;

    /** 考试满分 */
    public static final int EXAM_FULL_SCORE = 100;

    /** 私有构造函数，防止实例化 */
    private Constants() {}
}
