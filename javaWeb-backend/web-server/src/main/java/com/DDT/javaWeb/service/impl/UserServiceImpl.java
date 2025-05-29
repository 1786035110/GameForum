package com.DDT.javaWeb.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.DDT.javaWeb.dto.LoginDTO;
import com.DDT.javaWeb.dto.UserDTO;
import com.DDT.javaWeb.entity.GameLeaderboard;
import com.DDT.javaWeb.entity.User;
import com.DDT.javaWeb.mapper.GameLeaderboardMapper;
import com.DDT.javaWeb.mapper.UserMapper;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.service.IUserService;
import com.DDT.javaWeb.utils.UserHolder;
import com.DDT.javaWeb.vo.GameHistoryVO;
import com.DDT.javaWeb.vo.UserProfileVO;
import com.DDT.javaWeb.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.DDT.javaWeb.constant.RedisConstant.*;
import static com.DDT.javaWeb.constant.UserConstant.*;
import static com.DDT.javaWeb.utils.PasswordEncoder.encode;
import static com.DDT.javaWeb.utils.PasswordEncoder.matches;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GameLeaderboardMapper gameLeaderboardMapper;

    @Override
    public Result register(UserDTO userDTO) {
        // 检查用户名是否已存在
        User username = query().eq("username", userDTO.getUsername()).one();
        if (username != null) {
            log.info(USER_EXIST);
            return Result.error(USER_EXIST);
        }
        // 检查密码格式
        if (userDTO.getPassword() == null || userDTO.getPassword().length() < 6) {
            return Result.error(USER_PASSWORD_FORMAT_ERROR);
        }

        // 创建新用户
        User user = new User();
        BeanUtil.copyProperties(userDTO, user);
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setPassword(encode(userDTO.getPassword()));
        save(user);
        return Result.success();
    }

    @Override
    public Result login(LoginDTO loginDTO) {
        // 检查用户名是否存在
        User user = query().eq("username", loginDTO.getUsername()).one();
        if (user == null) {
            log.info(USER_NOT_EXIST);
            return Result.error(USER_NOT_EXIST);
        }
        // 检查密码是否正确
        if (!matches(user.getPassword(), loginDTO.getPassword())) {
            log.info(USER_PASSWORD_WRONG);
            return Result.error(USER_PASSWORD_WRONG);
        }

//        // 更新用户的登录时间
//        user.setUpdateTime(LocalDateTime.now());
//        updateById(user);

        // 生成token
        String token = UUID.randomUUID().toString(true);

        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(loginDTO, userVO);
        userVO.setUserID(user.getId());
        userVO.setToken(token);
        userVO.setLastLoginTime(LocalDateTime.now());

        // 将userVO转化为HashMap
        Map<String, Object> userMap = BeanUtil.beanToMap(userVO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        // 将token和userVO存入Redis
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 设置过期时间
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        return Result.success(userVO);
    }

    @Override
    public Result logout() {
        // 获取当前登录用户的token
        UserVO user = UserHolder.getUser();
        String token = user.getToken();
        if (token != null) {
            LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(User::getId, user.getUserID())
                    .set(User::getUpdateTime, user.getLastLoginTime());
            // 更新用户的登录时间
            update(updateWrapper);

            // 删除Redis中的token
            stringRedisTemplate.delete(LOGIN_USER_KEY + token);
            return Result.success();
        }
        // 如果token不存在，返回错误信息
        log.info(USER_NOT_LOGIN);
        return Result.error(USER_NOT_LOGIN);
    }

    @Override
    public Result<UserProfileVO> getUserProfile() {
        // 获取当前登录用户
        Long userID = UserHolder.getUser().getUserID();

        // 先查询缓存
        String key = CACHE_USER_PROFILE_KEY + userID;
        String profileJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(profileJson)) {
            // 如果缓存存在，直接返回缓存中的数据

            UserProfileVO userProfileVO = JSONUtil.toBean(profileJson, UserProfileVO.class);

            log.info("从缓存中获取用户信息: {}", userProfileVO);
            return Result.success(userProfileVO);
        }


        // 查询用户信息
        User user = getById(userID);
        String username = user.getUsername();

        // 查询用户的总分数
        // 创建查询条件
        QueryWrapper<GameLeaderboard> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("IFNULL(sum(score), 0) as total_score")
                .eq("user_id", userID);
        // 执行查询并获取结果
        Map<String, Object> result = gameLeaderboardMapper.selectMaps(queryWrapper).get(0);
        Integer scoreSum = ((Number) result.get("total_score")).intValue();

        // 查询最后一次登录时间
        LocalDateTime lastLoginTime = user.getUpdateTime();

        // 查询用户的游戏历史
        List<GameLeaderboard> leaderboards = gameLeaderboardMapper.selectList(
                new LambdaQueryWrapper<GameLeaderboard>()
                        .eq(GameLeaderboard::getUserId, userID)
        );

        // 将GameLeaderboard转换为GameHistoryVO
        List<GameHistoryVO> gameHistoryList = leaderboards.stream()
                .map(leaderboard -> {
                    GameHistoryVO historyVO = new GameHistoryVO();
                    BeanUtil.copyProperties(leaderboard, historyVO);
                    historyVO.setDate(leaderboard.getCreateTime());
                    return historyVO;
                })
                .collect(Collectors.toList());

        // 封装用户信息
        UserProfileVO userProfileVO = UserProfileVO.builder()
                .username(username)
                .score(scoreSum)
                .lastLogin(lastLoginTime)
                .gameHistories(gameHistoryList)
                .build();

        // 将用户信息存入缓存
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(userProfileVO), CACHE_USER_PROFILE_TTL, TimeUnit.MINUTES);

        return Result.success(userProfileVO);
    }

}
