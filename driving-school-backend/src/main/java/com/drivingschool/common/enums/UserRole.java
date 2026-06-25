package com.drivingschool.common.enums;

import lombok.Getter;

/**
 * 用户角色枚举。
 * <p>
 * 定义系统中所有用户角色类型，用于权限控制和角色管理。
 * 不同角色拥有不同的系统访问权限：
 * - ADMIN：系统管理员，拥有最高权限
 * - STAFF：前台工作人员，负责日常业务操作
 * - COACH：教练，管理教学相关业务
 * - STUDENT：学员，使用学员端功能
 * </p>
 * <p>
 * 角色编码（code）与数据库中的 role 字段对应，
 * 同时也用于 Spring Security 的角色权限判断（需加 ROLE_ 前缀）。
 * </p>
 */
@Getter
public enum UserRole {

    /** 系统管理员，拥有所有功能的访问权限 */
    ADMIN("ADMIN", "管理员"),

    /** 前台工作人员，负责学员报名、预约管理等日常业务操作 */
    STAFF("STAFF", "前台"),

    /** 教练，管理课程安排、学员练车等教学相关业务 */
    COACH("COACH", "教练"),

    /** 学员，使用在线学习、预约练车、查看成绩等功能 */
    STUDENT("STUDENT", "学员");

    /** 角色编码，存储在数据库中 */
    private final String code;

    /** 角色中文名称，用于前端展示 */
    private final String name;

    UserRole(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
