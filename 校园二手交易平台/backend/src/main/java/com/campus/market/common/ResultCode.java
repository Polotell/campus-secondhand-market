package com.campus.market.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务状态码枚举（与 /backend/doc/API.md 的"全局错误码"表保持一致）
 * <p>
 * 命名规则：
 * <ul>
 *   <li>0       —— 成功</li>
 *   <li>4xx/5xx —— HTTP 语义相关</li>
 *   <li>1xxxx   —— 账号相关业务错误</li>
 *   <li>2xxxx   —— 商品相关业务错误</li>
 *   <li>3xxxx   —— 钱包/积分业务错误</li>
 *   <li>4xxxx   —— 订单业务错误</li>
 *   <li>5xxxx   —— 权限/拉黑业务错误</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(0, "success"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限执行此操作"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 账号相关
    ACCOUNT_PENDING_AUDIT(10001, "账号尚未通过审核"),
    ACCOUNT_BANNED(10002, "账号已被封禁"),
    CAPTCHA_INVALID(10003, "验证码错误或已过期"),
    USERNAME_EXIST(10004, "用户名已存在"),
    USERNAME_OR_PASSWORD_WRONG(10005, "用户名或密码错误"),
    ACCOUNT_REJECTED(10006, "账号审核未通过"),
    AUDIT_STATUS_ILLEGAL(10007, "当前状态不允许此审核操作"),

    // 商品相关
    PRODUCT_STOCK_NOT_ENOUGH(20001, "商品库存不足"),
    PRODUCT_NOT_ON_SALE(20002, "商品已下架或未上架"),
    PRODUCT_NOT_EXIST(20003, "商品不存在"),
    REVIEW_NOT_ALLOWED(20004, "当前不可评价"),
    REVIEW_DUPLICATE(20005, "已评价过，不能重复提交"),

    // 钱包/积分相关
    BALANCE_NOT_ENOUGH(30001, "钱包余额不足"),
    POINTS_NOT_ENOUGH(30002, "积分余额不足"),

    // 订单相关
    ORDER_STATUS_ILLEGAL(40001, "订单状态不允许此操作"),
    RETURN_DEADLINE_EXCEEDED(40002, "已超过 24 小时退货时限"),
    ORDER_NOT_COMPLETED(40003, "订单未完成，不能评价"),
    ORDER_NOT_EXIST(40004, "订单不存在"),
    CART_EMPTY(40005, "购物车没有勾选任何商品"),
    CART_MULTI_MERCHANT(40006, "一次下单仅支持同一家商家的商品，请分别结算"),
    CART_ITEM_NOT_EXIST(40007, "购物车条目不存在或不属于当前用户"),
    CANNOT_BUY_OWN_PRODUCT(40008, "不能购买自己店铺的商品"),
    RETURN_RECORD_NOT_EXIST(40009, "退货记录不存在"),
    RETURN_RECORD_DUPLICATE(40010, "该订单已申请退货，不可重复提交"),
    RETURN_STATUS_ILLEGAL(40011, "退货当前状态不允许此操作"),

    // 权限 / 黑名单
    BLACKLISTED(50001, "您已被商家或平台拉黑，无法购买"),
    ;

    private final Integer code;
    private final String message;
}
