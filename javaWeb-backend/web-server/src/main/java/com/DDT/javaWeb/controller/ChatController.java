package com.DDT.javaWeb.controller;

import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.service.IChatService;
import com.DDT.javaWeb.vo.ChatMessageVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/chat")
@Slf4j
@Api(tags = "聊天相关接口")
public class ChatController {

    @Resource
    private IChatService chatService;

    @GetMapping("/public/messages")
    @ApiOperation(value = "获取公共聊天记录")
    public Result<List<ChatMessageVO>> getPublicMessages(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer size) {
        return chatService.getPublicMessages(page, size);
    }

    @GetMapping("/private/messages")
    @ApiOperation(value = "获取私聊记录")
    public Result<List<ChatMessageVO>> getPrivateMessages(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer size) {
        // 这里需要获取当前登录用户ID，与传入的userId进行私聊记录查询
        // Long currentUserId = UserHolder.getUser().getUserID();
        // return chatService.getPrivateMessages(currentUserId, userId, page, size);
        return Result.success(null); // 临时返回
    }
}