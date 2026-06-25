package com.drivingschool.common.enums;

import lombok.Getter;

/**
 * 支付方式枚举。
 * <p>
 * 定义系统支持的所有支付方式，用于收费记录和财务对账。
 * </p>
 */
@Getter
public enum PayMethod {

    /** 现金支付：线下收取现金 */
    CASH(1, "现金"),

    /** 微信支付：通过微信扫码或小程序支付 */
    WECHAT(2, "微信"),

    /** 支付宝支付：通过支付宝扫码或 APP 支付 */
    ALIPAY(3, "支付宝"),

    /** 银行卡支付：通过银行卡刷卡或转账支付 */
    BANK_CARD(4, "银行卡");

    /** 方式编码，存储在数据库中 */
    private final int code;

    /** 方式中文名称，用于前端展示 */
    private final String name;

    PayMethod(int code, String name) {
        this.code = code;
        this.name = name;
    }
}
