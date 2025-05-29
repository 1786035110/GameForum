package com.DDT.javaWeb.Task;

import com.DDT.javaWeb.mapper.PostMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;

import static com.DDT.javaWeb.constant.RedisConstant.POST_VIEW_COUNT_KEY;

@Component
@EnableScheduling
public class ViewCountTask {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private PostMapper postMapper;

    @Scheduled(fixedDelay = 300000) // 每5分钟执行一次
    public void syncViewCountToDatabase() {
        // 获取所有浏览计数的key
        Set<String> keys = stringRedisTemplate.keys(POST_VIEW_COUNT_KEY + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            String postIdStr = key.replace(POST_VIEW_COUNT_KEY, "");
            Long postId = Long.valueOf(postIdStr);
            String countStr = stringRedisTemplate.opsForValue().get(key);

            if (countStr != null) {
                int count = Integer.parseInt(countStr);
                postMapper.updateViewCount(postId, count);

            }
        }
    }
}
