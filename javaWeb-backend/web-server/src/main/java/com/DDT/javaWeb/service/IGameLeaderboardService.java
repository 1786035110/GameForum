package com.DDT.javaWeb.service;

import com.DDT.javaWeb.dto.ScoreDTO;
import com.DDT.javaWeb.dto.leaderboardDTO;
import com.DDT.javaWeb.entity.GameLeaderboard;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.vo.LeaderboardVO;
import com.DDT.javaWeb.vo.RankVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IGameLeaderboardService extends IService<GameLeaderboard> {
    /**
     * 获取排行榜数据
     * @param leaderboardDTO 排行榜参数
     * @return 排行榜数据
     */
    Result<List<LeaderboardVO>> getLeaderboard(leaderboardDTO leaderboardDTO);

    /**
     * 提交分数
     * @param userId 用户ID
     * @param scoreDTO 分数信息
     * @return 排名结果
     */
    Result<RankVO> submitScore(Long userId, ScoreDTO scoreDTO);

    void refreshLeaderboard(String timeRange);
}
