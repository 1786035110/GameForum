package com.DDT.javaWeb.service;

import com.DDT.javaWeb.entity.ChatMessage;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.vo.ChatMessageVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IChatService extends IService<ChatMessage> {

    /**
     * 保存公共聊天消息
     */
    void savePublicMessage(Long senderId, String content);

    /**
     * 保存私聊消息
     */
    void savePrivateMessage(Long senderId, Long receiverId, String content);

    /**
     * 获取公共聊天记录
     */
    Result<List<ChatMessageVO>> getPublicMessages(Integer page, Integer size);

    /**
     * 获取私聊记录
     */
    Result<List<ChatMessageVO>> getPrivateMessages(Long userId1, Long userId2, Integer page, Integer size);
}