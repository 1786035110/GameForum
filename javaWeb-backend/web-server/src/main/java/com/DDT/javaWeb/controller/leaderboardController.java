package com.DDT.javaWeb.controller;

import com.DDT.javaWeb.dto.ScoreDTO;
import com.DDT.javaWeb.dto.leaderboardDTO;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.service.IGameLeaderboardService;
import com.DDT.javaWeb.utils.UserHolder;
import com.DDT.javaWeb.vo.LeaderboardVO;
import com.DDT.javaWeb.vo.RankVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/leaderboard")
@Slf4j
@Api(tags = "排行榜相关接口")
public class leaderboardController {

    @Resource
    private IGameLeaderboardService gameLeaderboardService;

    @GetMapping
    @ApiOperation("获取排行榜数据")
    public Result<List<LeaderboardVO>> getLeaderboard(leaderboardDTO leaderboardDTO) {
        log.info("获取排行榜数据，参数：{}", leaderboardDTO);
        return gameLeaderboardService.getLeaderboard(leaderboardDTO);
    }

    @PostMapping("/submit")
    @ApiOperation("提交分数")
    public Result<RankVO> submitScore(@RequestBody ScoreDTO scoreDTO) {
        log.info("提交分数，参数：{}", scoreDTO);
        // 从token中获取用户ID
        Long userId = UserHolder.getUser().getUserID();
        return gameLeaderboardService.submitScore(userId, scoreDTO);
    }
}
