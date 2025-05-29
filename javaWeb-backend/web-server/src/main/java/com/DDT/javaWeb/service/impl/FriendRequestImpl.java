package com.DDT.javaWeb.service.impl;

import cn.hutool.json.JSONUtil;
import com.DDT.javaWeb.entity.Follow;
import com.DDT.javaWeb.entity.FriendRequest;
import com.DDT.javaWeb.entity.User;
import com.DDT.javaWeb.mapper.FollowMapper;
import com.DDT.javaWeb.mapper.FriendRequestMapper;
import com.DDT.javaWeb.mapper.UserMapper;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.service.IFriendRequestService;
import com.DDT.javaWeb.utils.UserHolder;
import com.DDT.javaWeb.vo.RequestVO;
import com.DDT.javaWeb.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.DDT.javaWeb.constant.FriendRequestConstant.*;
import static com.DDT.javaWeb.constant.RedisConstant.*;
import static com.DDT.javaWeb.constant.UserConstant.USER_NOT_EXIST;

@Service
@Slf4j
public class FriendRequestImpl extends ServiceImpl<FriendRequestMapper, FriendRequest> implements IFriendRequestService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private FollowMapper followMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result<List<RequestVO>> getFriendRequestList() {
        // 1. 获取当前用户的ID
        Long userId = UserHolder.getUser().getUserID();

        // 先查询缓存
        String friendRequestListKey = CACHE_FRIEND_REQUEST_LIST_KEY + userId;
        Map<Object, Object> requestMap = stringRedisTemplate.opsForHash().entries(friendRequestListKey);
        if (!requestMap.isEmpty()) {
            // 如果缓存中有好友请求列表，直接返回
            List<RequestVO> requestList = requestMap.entrySet().stream()
                    .map(entry -> JSONUtil.toBean(entry.getValue().toString(), RequestVO.class))
                    .collect(Collectors.toList());
            return Result.success(requestList);
        }


        LambdaQueryWrapper<FriendRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FriendRequest::getToUserId, userId);

        // 2. 查询当前用户的好友请求列表
        List<RequestVO> requestList = this.list(queryWrapper)
                .stream()
                .map(FriendRequest-> {
                    RequestVO requestVO = RequestVO.builder()
                            .id(FriendRequest.getFromUserId())
                            .username(userMapper.selectOne(new LambdaQueryWrapper<User>()
                                    .eq(User::getId, FriendRequest.getFromUserId()))
                                    .getUsername())
                            .requestTime(FriendRequest.getCreateTime())
                            .requestId(FriendRequest.getId())
                            .build();
                    return requestVO;
                })
                .collect(Collectors.toList());
        if (requestList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 将好友请求列表存入缓存
        requestList.forEach(request -> {
            String jsonStr = JSONUtil.toJsonStr(request);
            stringRedisTemplate.opsForHash().put(friendRequestListKey,
                    request.getRequestId().toString(), jsonStr);
            log.info("将好友请求存入缓存: {}", jsonStr);
        });
        stringRedisTemplate.expire(friendRequestListKey, CACHE_USER_PROFILE_TTL, TimeUnit.MINUTES);

        return Result.success(requestList);
    }

    @Override
    public Result sendFriendRequest(String username) {
        // 1. 判断要添加的用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User toUser = userMapper.selectOne(queryWrapper);
        if (toUser == null) {
            return Result.error(USER_NOT_EXIST);
        }

        // 2. 获取要添加的用户的ID
        Long toUserId = toUser.getId();

        // 3. 获取当前用户的ID
        UserVO user = UserHolder.getUser();
        Long userId = user.getUserID();

        // 4. 判断要添加的用户是否是自己
        if (userId.equals(toUserId)) {
            return Result.error("不能添加自己为好友");
        }

        // 5. 判断要添加的用户是否已经是好友
        QueryWrapper<Follow> followQueryWrapper = new QueryWrapper<>();
        followQueryWrapper.eq("user_id", userId)
                .eq("follow_user_id", toUserId);

        int count = followMapper.selectCount(followQueryWrapper);
        if (count > 0) {
            return Result.error(REQUEST_ALREADY_FRIEND);
        }

        // 6. 判断是否已经发送好友请求
        LambdaQueryWrapper<FriendRequest> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(FriendRequest::getFromUserId, userId)
                .eq(FriendRequest::getToUserId, toUserId);
        count = this.count(lambdaQueryWrapper);
        if (count > 0) {
            return Result.error("请勿重复发送好友请求");
        }

        // 7. 判断对方是否已经发送好友请求
        lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(FriendRequest::getFromUserId, toUserId)
                .eq(FriendRequest::getToUserId, userId);
        count = this.count(lambdaQueryWrapper);
        if (count > 0) {
            return Result.error("对方已经向你发送好友请求，请处理");
        }

        // 8. 创建好友请求
        FriendRequest friendRequest = FriendRequest.builder()
                .FromUserId(userId)
                .ToUserId(toUserId)
                .createTime(LocalDateTime.now())
                .build();
        this.save(friendRequest);

        // 9. 将好友请求存入缓存
        String friendRequestListKey = CACHE_FRIEND_REQUEST_LIST_KEY + toUserId;
        RequestVO requestVO = RequestVO.builder()
                .id(userId)
                .username(user.getUsername())
                .requestTime(friendRequest.getCreateTime())
                .requestId(friendRequest.getId())
                .build();
        String jsonStr = JSONUtil.toJsonStr(requestVO);
        stringRedisTemplate.opsForHash().put(friendRequestListKey, friendRequest.getId().toString(), jsonStr);
        stringRedisTemplate.expire(friendRequestListKey, CACHE_USER_PROFILE_TTL, TimeUnit.MINUTES);

        return Result.success(REQUEST_ALREADY_SENT);
    }

    @Override
    public Result acceptFriendRequest(Long requestId) {
        UserVO user = UserHolder.getUser();
        Long userId = user.getUserID();

        // 1. 查询请求是否存在
        FriendRequest request = this.getOne(new LambdaQueryWrapper<FriendRequest>()
                .eq(FriendRequest::getFromUserId, requestId)
                .eq(FriendRequest::getToUserId, userId));
        if (request == null) {
            return Result.error(REQUEST_NOT_EXIST);
        }

        // 2. 验证当前用户身份
        if (!request.getToUserId().equals(userId)) {
            return Result.error(REQUEST_NOT_AUTHORITY_TO_PROCESS);
        }

        // 3. 删除好友请求
        this.remove(new LambdaQueryWrapper<FriendRequest>()
                .eq(FriendRequest::getFromUserId, requestId)
                .eq(FriendRequest::getToUserId, userId));

        // 4. 建立双向好友关系
        Follow follow1 = new Follow();
        follow1.setUserId(userId);
        follow1.setFollowUserId(request.getFromUserId());
        follow1.setCreateTime(LocalDateTime.now());
        followMapper.insert(follow1);

        Follow follow2 = new Follow();
        follow2.setUserId(request.getFromUserId());
        follow2.setFollowUserId(userId);
        follow2.setCreateTime(LocalDateTime.now());
        followMapper.insert(follow2);

        // 将新加的好友存入缓存
        String friendListKey = CACHE_FRIEND_LIST_KEY + userId;
        stringRedisTemplate.opsForHash()
                .put(friendListKey, request.getFromUserId().toString(), userMapper.selectById(request.getFromUserId()).getUsername());

        // 删除缓存中的好友请求
        String friendRequestListKey = CACHE_FRIEND_REQUEST_LIST_KEY + userId;
        stringRedisTemplate.opsForHash().delete(friendRequestListKey, request.getId().toString());

        return Result.success(REQUEST_ALREADY_ACCEPTED);
    }

    @Override
    public Result rejectFriendRequest(Long requestId) {
        UserVO user = UserHolder.getUser();
        Long userId = user.getUserID();

        // 1. 查询请求是否存在
        LambdaQueryWrapper <FriendRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FriendRequest::getFromUserId, requestId)
                .eq(FriendRequest::getToUserId, userId);

        FriendRequest request = this.getOne(queryWrapper);
        if (request == null) {
            return Result.error(REQUEST_NOT_EXIST);
        }

        // 2. 验证当前用户身份
        if (!request.getToUserId().equals(userId)) {
            return Result.error(REQUEST_NOT_AUTHORITY_TO_PROCESS);
        }

        // 3. 删除好友请求
        this.remove(new LambdaQueryWrapper<FriendRequest>()
                .eq(FriendRequest::getFromUserId, requestId)
                .eq(FriendRequest::getToUserId, userId));

        // 4. 删除缓存中的好友请求
        String friendRequestListKey = CACHE_FRIEND_REQUEST_LIST_KEY + userId;
        stringRedisTemplate.opsForHash().delete(friendRequestListKey, request.getId().toString());
        log.info("删除好友请求缓存: {}", requestId);
        log.info("userid: {}", userId);
        return Result.success(REQUEST_ALREADY_REJECTED);
    }
}
