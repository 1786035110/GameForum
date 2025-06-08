package com.DDT.javaWeb.service.impl;

import com.DDT.javaWeb.entity.Follow;
import com.DDT.javaWeb.mapper.FollowMapper;
import com.DDT.javaWeb.mapper.UserMapper;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.service.IFollowService;
import com.DDT.javaWeb.utils.UserHolder;
import com.DDT.javaWeb.vo.FriendVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.DDT.javaWeb.constant.FriendConstant.FRIEND_DELETE_FAIL;
import static com.DDT.javaWeb.constant.FriendConstant.FRIEND_DELETE_SUCCESS;
import static com.DDT.javaWeb.constant.RedisConstant.CACHE_FRIEND_LIST_KEY;

@Service
@Slf4j
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result<List<FriendVO>> getFriendList() {
        Long userId = UserHolder.getUser().getUserID();

        // 先查询缓存
        String friendListKey = CACHE_FRIEND_LIST_KEY + userId;
        Map<Object, Object> friendList = stringRedisTemplate.opsForHash().entries(friendListKey);
        if (!friendList.isEmpty()) {
            log.info("从缓存中获取好友列表");
            // 缓存中有好友列表，直接返回
            List<FriendVO> friendVOList = friendList.entrySet().stream()
                    .map(entry -> {
                        FriendVO friendVO = new FriendVO();
                        friendVO.setId(Long.valueOf(entry.getKey().toString()));
                        friendVO.setUsername(entry.getValue().toString());
                        return friendVO;
                    })
                    .collect(Collectors.toList());
            return Result.success(friendVOList);
        }

        LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Follow::getUserId, userId);
        List<Long> followIdList = this.list(queryWrapper).stream()
                .map(Follow::getFollowUserId)
                .collect(Collectors.toList());

        if (followIdList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        List<FriendVO> friendVOList = userMapper.selectBatchIds(followIdList).stream()
                .map(user -> {
                    FriendVO friendVO = new FriendVO();
                    friendVO.setId(user.getId());
                    friendVO.setUsername(user.getUsername());
                    return friendVO;
                })
                .collect(Collectors.toList());

        // 将好友列表存入缓存
        Map<String, String> friendMap = friendVOList.stream()
                .collect(Collectors.toMap(friend -> friend.getId().toString(), FriendVO::getUsername));
        stringRedisTemplate.opsForHash().putAll(friendListKey, friendMap);

        return Result.success(friendVOList);
    }

    @Override
    public Result deleteFriend(Long friendId) {
        // 1. 获取当前登录用户的ID
        Long userId = UserHolder.getUser().getUserID();
        boolean success = false;

        // 2. 删除好友关系
        LambdaQueryWrapper<Follow> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, friendId);
        int count1 = this.baseMapper.delete(queryWrapper1);

        LambdaQueryWrapper<Follow> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.eq(Follow::getUserId, friendId)
                .eq(Follow::getFollowUserId, userId);
        int count2 = this.baseMapper.delete(queryWrapper2);

        // 只要有一方删除成功就算成功
        if (count1 > 0 || count2 > 0) {
            success = true;
        }

        // 3. 删除缓存中的好友记录
        String userFriendListKey = CACHE_FRIEND_LIST_KEY + userId;
        if (stringRedisTemplate.hasKey(userFriendListKey)) {
            stringRedisTemplate.opsForHash().delete(userFriendListKey, friendId.toString());
            log.info("删除用户{}缓存中的好友: {}", userId, friendId);
        }

        // 4. 删除好友的缓存中的当前用户
        String friendListKey = CACHE_FRIEND_LIST_KEY + friendId;
        if (stringRedisTemplate.hasKey(friendListKey)) {
            stringRedisTemplate.opsForHash().delete(friendListKey, userId.toString());
            log.info("删除好友{}缓存中的用户: {}", friendId, userId);
        }

        return success ? Result.success(FRIEND_DELETE_SUCCESS) : Result.error(FRIEND_DELETE_FAIL);
    }
}
