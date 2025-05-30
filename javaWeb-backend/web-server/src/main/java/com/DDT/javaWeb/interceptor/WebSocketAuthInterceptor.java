package com.DDT.javaWeb.interceptor;

import cn.hutool.core.bean.BeanUtil;
import com.DDT.javaWeb.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import java.util.Map;

import static com.DDT.javaWeb.constant.RedisConstant.LOGIN_USER_KEY;

@Component
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        log.info("WebSocket握手请求: {}", request.getURI());

        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            log.warn("WebSocket握手失败：没有提供token");
            return false;
        }

        // 验证token并获取用户信息
        String key = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);

        if (userMap.isEmpty()) {
            log.warn("WebSocket握手失败：token无效");
            return false;
        }

        // 将用户信息存储到WebSocket会话属性中
        UserVO userVO = BeanUtil.fillBeanWithMap(userMap, new UserVO(), false);
        attributes.put("userId", userVO.getUserID());
        attributes.put("username", userVO.getUsername());

        log.info("用户 {} (ID: {}) 建立WebSocket连接", userVO.getUsername(), userVO.getUserID());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket握手后发生异常", exception);
        }
    }

    private String extractToken(ServerHttpRequest request) {
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }
        return null;
    }
}