package com.DDT.javaWeb.constant;

public class RedisConstant {
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;

    public static final String BLOG_LIKED_KEY = "blog:liked:";

    // cache
    public static final String CACHE_USER_PROFILE_KEY = "cache:user:profile:";
    public static final String CACHE_FRIEND_LIST_KEY = "cache:friend:list:";
    public static final String CACHE_FRIEND_REQUEST_LIST_KEY = "cache:friend:request:list:";
    public static final String CACHE_FORUM_CATEGORIES_KEY = "cache:forum:categories:";
    public static final String CACHE_POST_COMMENTS_KEY = "cache:post:comments:";
    public static final String CACHE_POST_KEY = "cache:post:";

    public static final String POST_VIEW_COUNT_KEY = "post:view:count:";

    public static final Long CACHE_POST_TTL = 1440L;
    public static final Long CACHE_POST_COMMENTS_TTL = 24L;
    public static final Long CACHE_USER_PROFILE_TTL = 1440L;
}
