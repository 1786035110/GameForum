package com.DDT.javaWeb.service.impl;

import com.DDT.javaWeb.dto.ScoreDTO;
import com.DDT.javaWeb.dto.leaderboardDTO;
import com.DDT.javaWeb.entity.GameLeaderboard;
import com.DDT.javaWeb.entity.User;
import com.DDT.javaWeb.mapper.GameLeaderboardMapper;
import com.DDT.javaWeb.mapper.UserMapper;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.service.IGameLeaderboardService;
import com.DDT.javaWeb.vo.LeaderboardVO;
import com.DDT.javaWeb.vo.RankVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.DDT.javaWeb.constant.RedisConstant.CACHE_USER_PROFILE_KEY;

@Service
@Slf4j
public class GameLeaderboardServiceImpl extends ServiceImpl<GameLeaderboardMapper, GameLeaderboard> implements IGameLeaderboardService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserMapper userMapper;

    private static final String LEADERBOARD_KEY_PREFIX = "leaderboard:snake:";
    private static final String PLAYER_KEY_PREFIX = "leaderboard:snake:player:";
    private static final String LOCK_KEY_PREFIX = "lock:leaderboard:";
    private static final long LOCK_EXPIRE_TIME = 5; // 5秒
    private static final int DEFAULT_LIMIT = 10;

    @Override
    public Result<List<LeaderboardVO>> getLeaderboard(leaderboardDTO dto) {
        // 设置默认值
        String timeRange = dto.getTimeRange() == null ? "all" : dto.getTimeRange();
        int limit = dto.getLimit() == null ? DEFAULT_LIMIT : dto.getLimit();

        // 从Redis获取排行榜数据
        String leaderboardKey = LEADERBOARD_KEY_PREFIX + timeRange;
        Set<ZSetOperations.TypedTuple<String>> leaderboardSet = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(leaderboardKey, 0, limit - 1);

        // 如果Redis中没有数据，从数据库获取并更新Redis
        if (leaderboardSet == null || leaderboardSet.isEmpty()) {
            updateLeaderboard(timeRange);
            leaderboardSet = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(leaderboardKey, 0, limit - 1);
        }

        // 转换为VO对象
        List<LeaderboardVO> leaderboardVOs = new ArrayList<>();
        if (leaderboardSet != null) {
            int rank = 1;
            for (ZSetOperations.TypedTuple<String> tuple : leaderboardSet) {
                String userId = tuple.getValue();
                if (userId == null) continue;

                // 获取玩家详细信息
                Map<Object, Object> playerInfo = stringRedisTemplate.opsForHash().entries(PLAYER_KEY_PREFIX + userId);
                
                // 如果Redis中没有玩家信息，从数据库获取
                if (playerInfo.isEmpty()) {
                    GameLeaderboard gameLeaderboard = getPlayerTopScore(Long.parseLong(userId));
                    if (gameLeaderboard == null) continue;
                    
                    User user = userMapper.selectById(Long.parseLong(userId));
                    if (user == null) continue;
                    
                    LeaderboardVO vo = new LeaderboardVO();
                    vo.setRank(rank);
                    vo.setUsername(user.getUsername());
                    vo.setScore(gameLeaderboard.getScore());
                    vo.setDuration(gameLeaderboard.getDuration());
                    vo.setDate(gameLeaderboard.getCreateTime());
                    leaderboardVOs.add(vo);
                } else {
                    // 从Redis获取玩家信息
                    LeaderboardVO vo = new LeaderboardVO();
                    vo.setRank(rank);
                    
                    // 获取用户名
                    User user = userMapper.selectById(Long.parseLong(userId));
                    vo.setUsername(user != null ? user.getUsername() : "未知用户");
                    
                    vo.setScore(Integer.parseInt(playerInfo.getOrDefault("score", "0").toString()));
                    vo.setDuration(Integer.parseInt(playerInfo.getOrDefault("duration", "0").toString()));
                    
                    String endTimeStr = (String) playerInfo.get("endTime");
                    if (endTimeStr != null) {
                        vo.setDate(LocalDateTime.parse(endTimeStr));
                    }
                    
                    leaderboardVOs.add(vo);
                }
                rank++;
            }
        }

        return Result.success(leaderboardVOs);
    }

    @Override
    public Result<RankVO> submitScore(Long userId, ScoreDTO scoreDTO) {
        // 使用分布式锁防止并发提交
        String lockKey = LOCK_KEY_PREFIX + userId;
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(acquired)) {
            return Result.error("操作过于频繁，请稍后再试");
        }

        try {
            // 无论分数如何，都保存到数据库用于统计个人游戏数据
            GameLeaderboard gameLeaderboard = new GameLeaderboard();
            gameLeaderboard.setUserId(userId);
            gameLeaderboard.setScore(scoreDTO.getScore());
            gameLeaderboard.setDuration(scoreDTO.getDuration());
            gameLeaderboard.setCreateTime(scoreDTO.getEndTime());
            save(gameLeaderboard);

            // 删除用户资料缓存
            stringRedisTemplate.delete(CACHE_USER_PROFILE_KEY + userId);

            // 检查是否是新纪录
            boolean isNewRecord = false;
            Double currentScore = stringRedisTemplate.opsForZSet().score(LEADERBOARD_KEY_PREFIX + "all", userId.toString());
            
            // 如果Redis中没有记录，从数据库检查
            if (currentScore == null) {
                GameLeaderboard existingRecord = getPlayerTopScore(userId);
                currentScore = existingRecord != null ? (double) existingRecord.getScore() : 0.0;
            }

            // 只有更高分才更新排行榜
            if (currentScore == null || scoreDTO.getScore() > currentScore) {
                isNewRecord = true;
                // 更新Redis所有时间范围的排行榜
                updateAllLeaderboards(userId, scoreDTO);
            } else if (scoreDTO.getScore().equals(currentScore.intValue())) {
                // 分数相同，检查时间是否更短
                Map<Object, Object> playerInfo = stringRedisTemplate.opsForHash().entries(PLAYER_KEY_PREFIX + userId);
                int currentDuration = Integer.parseInt(playerInfo.getOrDefault("duration", "0").toString());
                
                if (scoreDTO.getDuration() < currentDuration) {
                    isNewRecord = true;
                    // 更新Redis中的排行榜记录
                    updateAllLeaderboards(userId, scoreDTO);
                }
            }

            // 获取当前排名
            Long rank = stringRedisTemplate.opsForZSet().reverseRank(LEADERBOARD_KEY_PREFIX + "all", userId.toString());
            int playerRank = (rank != null) ? rank.intValue() + 1 : 0;

            RankVO rankVO = new RankVO(playerRank, isNewRecord);
            return Result.success(rankVO);
        } finally {
            // 释放锁
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public void refreshLeaderboard(String timeRange) {
        updateLeaderboard(timeRange);
    }

    /**
     * 更新所有时间范围的排行榜
     */
    public void updateAllLeaderboards(Long userId, ScoreDTO scoreDTO) {
        // 更新全局排行榜
        stringRedisTemplate.opsForZSet().add(LEADERBOARD_KEY_PREFIX + "all", userId.toString(), scoreDTO.getScore());
        
        // 更新周排行榜
        stringRedisTemplate.opsForZSet().add(LEADERBOARD_KEY_PREFIX + "week", userId.toString(), scoreDTO.getScore());
        
        // 更新月排行榜
        stringRedisTemplate.opsForZSet().add(LEADERBOARD_KEY_PREFIX + "month", userId.toString(), scoreDTO.getScore());
        
        // 更新玩家信息
        Map<String, String> playerInfo = new HashMap<>();
        playerInfo.put("score", scoreDTO.getScore().toString());
        playerInfo.put("duration", scoreDTO.getDuration().toString());
        playerInfo.put("endTime", scoreDTO.getEndTime().toString());
        stringRedisTemplate.opsForHash().putAll(PLAYER_KEY_PREFIX + userId, playerInfo);

    }

    /**
     * 根据时间范围更新排行榜
     */
    private void updateLeaderboard(String timeRange) {
        LocalDateTime startTime;
        LocalDateTime endTime = LocalDateTime.now();
        
        // 根据时间范围确定查询起始时间
        switch (timeRange) {
            case "week":
                startTime = endTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                break;
            case "month":
                startTime = endTime.withDayOfMonth(1);
                break;
            default: // "all"
                startTime = LocalDateTime.of(2000, 1, 1, 0, 0);
                break;
        }
        
        // 从数据库查询排行榜数据
        LambdaQueryWrapper<GameLeaderboard> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(GameLeaderboard::getCreateTime, startTime)
                   .le(GameLeaderboard::getCreateTime, endTime)
                   .orderByDesc(GameLeaderboard::getScore);
        
        List<GameLeaderboard> leaderboards = list(queryWrapper);
        
        // 更新Redis
        String leaderboardKey = LEADERBOARD_KEY_PREFIX + timeRange;
        
        // 先清除旧数据
        stringRedisTemplate.delete(leaderboardKey);
        
        // 批量添加新数据
        if (!leaderboards.isEmpty()) {
            // 为每个用户只保留最高分
            Map<Long, GameLeaderboard> userBestScores = new HashMap<>();
            for (GameLeaderboard leaderboard : leaderboards) {
                Long userId = leaderboard.getUserId();
                if (!userBestScores.containsKey(userId) || 
                    userBestScores.get(userId).getScore() < leaderboard.getScore()) {
                    userBestScores.put(userId, leaderboard);
                }
            }
            
            // 创建一个Set<ZSetOperations.TypedTuple<String>>对象
            Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
            
            // 添加到排行榜
            for (Map.Entry<Long, GameLeaderboard> entry : userBestScores.entrySet()) {
                Long userId = entry.getKey();
                GameLeaderboard leaderboard = entry.getValue();
                
                // 创建TypedTuple对象并添加到集合
                ZSetOperations.TypedTuple<String> tuple = 
                    new DefaultTypedTuple<>(userId.toString(), (double) leaderboard.getScore());
                tuples.add(tuple);
                
                // 更新玩家信息
                Map<String, String> playerInfo = new HashMap<>();
                playerInfo.put("score", leaderboard.getScore().toString());
                playerInfo.put("duration", leaderboard.getDuration().toString());
                playerInfo.put("endTime", leaderboard.getCreateTime().toString());
                stringRedisTemplate.opsForHash().putAll(PLAYER_KEY_PREFIX + userId, playerInfo);
            }
            
            // 批量添加到排行榜
            if (!tuples.isEmpty()) {
                stringRedisTemplate.opsForZSet().add(leaderboardKey, tuples);
            }
        }
    }

    /**
     * 获取玩家最高分记录
     */
    private GameLeaderboard getPlayerTopScore(Long userId) {
        LambdaQueryWrapper<GameLeaderboard> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GameLeaderboard::getUserId, userId)
                   .orderByDesc(GameLeaderboard::getScore)
                   .last("LIMIT 1");
        return getOne(queryWrapper);
    }
}
