package com.DDT.javaWeb.controller;

import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.service.IFollowService;
import com.DDT.javaWeb.service.IFriendRequestService;
import com.DDT.javaWeb.vo.FriendVO;
import com.DDT.javaWeb.vo.RequestVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/friends")
@Slf4j
@Api(tags = "好友相关接口")
public class FriendsController {

    @Resource
    private IFollowService  followService;

    @Resource
    private IFriendRequestService friendRequestService;

    @GetMapping("")
    @ApiOperation(value = "获取好友列表")
    public Result<List<FriendVO>> getFriendList() {
        log.info("获取好友列表");
        return followService.getFriendList();
    }

    @GetMapping("/requests")
    @ApiOperation(value = "获取好友请求列表")
    public Result<List<RequestVO>> getFriendRequestList() {
        log.info("获取好友请求列表");
        return friendRequestService.getFriendRequestList();
    }

    @PostMapping("/request")
    @ApiOperation(value = "发送好友请求")
    public Result sendFriendRequest(@RequestParam String username) {
        log.info("发送好友请求, username: {}", username);
        return friendRequestService.sendFriendRequest(username);
    }

    @PostMapping("/requests/{id}/accept")
    @ApiOperation(value = "接受好友请求")
    public Result acceptFriendRequest(@PathVariable("id") Long requestId) {
        log.info("接受好友请求, requestId: {}", requestId);
        return friendRequestService.acceptFriendRequest(requestId);
    }

    @PostMapping("/requests/{id}/reject")
    @ApiOperation(value = "拒绝好友请求")
    public Result rejectFriendRequest(@PathVariable("id") Long requestId) {
        log.info("拒绝好友请求, requestId: {}", requestId);
        return friendRequestService.rejectFriendRequest(requestId);
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除好友")
    public Result deleteFriend(@PathVariable("id") Long friendId) {
        log.info("删除好友, friendId: {}", friendId);
        return followService.deleteFriend(friendId);
    }
}
