package com.haiapi.common.constant;

public class SystemConstant {
    public static final String ROLE_SUPER_ADMIN = "super_admin";
    public static final String ROLE_ORG_ADMIN = "org_admin";
    public static final String ROLE_USER = "user";
    public static final String TOKEN_PREFIX = "sk-hai-";
    public static final String JWT_SECRET = "hai-api-secret-key-change-in-production";
    public static final long JWT_EXPIRATION = 86400000L;
    public static final String USER_STATUS_ACTIVE = "active";
    public static final String USER_STATUS_DISABLED = "disabled";
    public static final String TOKEN_STATUS_ACTIVE = "active";
    public static final String TOKEN_STATUS_DISABLED = "disabled";
    public static final String CHANNEL_STATUS_NORMAL = "active";
    public static final String CHANNEL_STATUS_DISABLED = "disabled";
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final long CACHE_EXPIRE_TIME = 300L;

    private SystemConstant() {}
}
