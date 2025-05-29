package com.DDT.javaWeb.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.DDT.javaWeb.dto.UserDTO;
import com.DDT.javaWeb.utils.UserHolder;
import com.DDT.javaWeb.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.DDT.javaWeb.constant.RedisConstant.LOGIN_USER_KEY;
import static com.DDT.javaWeb.constant.RedisConstant.LOGIN_USER_TTL;

@Component
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1.获取请求头中的token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            return true;
        }
        // 处理Bearer前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        // 2.基于TOKEN获取redis中的用户
        String key = LOGIN_USER_KEY + token;  // 使用常量，与LoginInterceptor保持一致
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        // 3.判断用户是否存在
        if (userMap.isEmpty()) {
            return true;
        }
        // 5.将查询到的hash数据转为UserVO
        UserVO userVO =  BeanUtil.fillBeanWithMap(userMap, new UserVO(), false);  // 与LoginInterceptor保持一致的转换方式

        // 6.存在，保存用户信息到 ThreadLocal
        UserHolder.saveUser(userVO);
        // 7.刷新token有效期
        stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 8.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 移除用户
        UserHolder.removeUser();
    }
}
