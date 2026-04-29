package com.campus.market.common;

/**
 * 全局常量定义（禁止在业务代码里写魔法字符串/数字）
 */
public final class Constants {

    private Constants() {}

    /** JWT 中存放用户 ID 的 claim 键名 */
    public static final String JWT_CLAIM_USER_ID  = "uid";
    /** JWT 中存放用户角色的 claim 键名 */
    public static final String JWT_CLAIM_ROLE     = "role";
    /** JWT 中存放用户名的 claim 键名 */
    public static final String JWT_CLAIM_USERNAME = "username";

    /** 请求头中携带当前登录用户 ID（由 JwtUtil 解析后放入） */
    public static final String HEADER_USER_ID = "X-User-Id";

    /** 平台中间账户（托管）的固定行 id，见 data.sql */
    public static final Long PLATFORM_ESCROW_ID = 1L;
    /** 平台手续费收入账户的固定行 id */
    public static final Long PLATFORM_FEE_ID    = 2L;
}
