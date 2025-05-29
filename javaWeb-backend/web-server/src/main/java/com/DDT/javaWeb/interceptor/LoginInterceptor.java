package com.DDT.javaWeb.interceptor;

import cn.hutool.core.bean.BeanUtil;
import com.DDT.javaWeb.utils.UserHolder;
import com.DDT.javaWeb.vo.UserVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;

import static com.DDT.javaWeb.constant.RedisConstant.LOGIN_USER_KEY;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 获取请求头中的token
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }

        // 2. 提取实际token值
        token = token.substring(7); // 去掉"Bearer "

        // 3. 从Redis获取用户
        String key = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);

        // 4. 判断用户是否存在
        if (userMap.isEmpty()) {
            response.setStatus(401);
            return false;
        }

        // 5. 将用户信息转换为User对象
        UserVO userVO = BeanUtil.fillBeanWithMap(userMap, new UserVO(), false);

        // 6. 存入ThreadLocal
        UserHolder.saveUser(userVO);

        // 验证通过
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理ThreadLocal
        UserHolder.removeUser();
    }
}
