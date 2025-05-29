package com.DDT.javaWeb.Task;

import com.DDT.javaWeb.service.IGameLeaderboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@EnableScheduling
@Slf4j
public class RefreshLeaderboardTask {

    @Resource
    private IGameLeaderboardService gameLeaderboardService;

    // 每5分钟刷新一次排行榜
    @Scheduled(fixedRate = 300000)
    public void refreshLeaderboards() {
        log.info("开始刷新游戏排行榜...");
        try {
            // 刷新全部时间排行榜
            gameLeaderboardService.refreshLeaderboard("all");
            // 刷新周排行榜
            gameLeaderboardService.refreshLeaderboard("week");
            // 刷新月排行榜
            gameLeaderboardService.refreshLeaderboard("month");
            log.info("排行榜刷新完成");
        } catch (Exception e) {
            log.error("排行榜刷新失败", e);
        }
    }
}