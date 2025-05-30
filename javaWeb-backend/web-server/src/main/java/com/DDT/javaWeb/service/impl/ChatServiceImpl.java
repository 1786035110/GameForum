package com.DDT.javaWeb.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.DDT.javaWeb.entity.ChatMessage;
import com.DDT.javaWeb.entity.User;
import com.DDT.javaWeb.mapper.ChatMessageMapper;
import com.DDT.javaWeb.mapper.UserMapper;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.service.IChatService;
import com.DDT.javaWeb.vo.ChatMessageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements IChatService {

    @Resource
    private UserMapper userMapper;

    @Override
    public void savePublicMessage(Long senderId, String content) {
        ChatMessage message = ChatMessage.builder()
                .senderId(senderId)
                .content(content)
                .messageType("public")
                .createTime(LocalDateTime.now())
                .build();

        this.save(message);
    }

    @Override
    public void savePrivateMessage(Long senderId, Long receiverId, String content) {
        ChatMessage message = ChatMessage.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .messageType("private")
                .createTime(LocalDateTime.now())
                .build();

        this.save(message);
    }

    @Override
    public Result<List<ChatMessageVO>> getPublicMessages(Integer page, Integer size) {
        IPage<ChatMessage> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getMessageType, "public")
                .orderByDesc(ChatMessage::getCreateTime);

        IPage<ChatMessage> messageList = this.page(pageParam, queryWrapper);

        List<ChatMessageVO> messageVOList = messageList.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Result.success(messageVOList);
    }

    @Override
    public Result<List<ChatMessageVO>> getPrivateMessages(Long userId1, Long userId2, Integer page, Integer size) {
        IPage<ChatMessage> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getMessageType, "private")
                .and(wrapper -> wrapper
                        .and(w -> w.eq(ChatMessage::getSenderId, userId1)
                                .eq(ChatMessage::getReceiverId, userId2))
                        .or(w -> w.eq(ChatMessage::getSenderId, userId2)
                                .eq(ChatMessage::getReceiverId, userId1)))
                .orderByDesc(ChatMessage::getCreateTime);

        IPage<ChatMessage> messageList = this.page(pageParam, queryWrapper);

        List<ChatMessageVO> messageVOList = messageList.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Result.success(messageVOList);
    }

    private ChatMessageVO convertToVO(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        BeanUtil.copyProperties(message, vo);

        // 获取发送者信息
        User sender = userMapper.selectById(message.getSenderId());
        if (sender != null) {
            vo.setSenderName(sender.getUsername());
        }

        // 如果是私聊消息，获取接收者信息
        if ("private".equals(message.getMessageType()) && message.getReceiverId() != null) {
            User receiver = userMapper.selectById(message.getReceiverId());
            if (receiver != null) {
                vo.setReceiverName(receiver.getUsername());
            }
        }

        return vo;
    }
}