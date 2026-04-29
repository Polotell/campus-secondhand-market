package com.campus.market.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 二手商品新旧程度枚举
 */
public enum ConditionLevel {

    NEW("NEW",         "全新"),
    NINETY("NINETY",   "9 成新"),
    EIGHTY("EIGHTY",   "8 成新"),
    SEVENTY("SEVENTY", "7 成新"),
    OTHER("OTHER",     "其他");

    @EnumValue
    @JsonValue
    private final String code;
    private final String label;

    ConditionLevel(String code, String label) { this.code = code; this.label = label; }
    public String getCode()  { return code; }
    public String getLabel() { return label; }
}
