package com.DDT.javaWeb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_chat_message")
public class ChatMessage implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long senderId; // 发送者ID

    private Long receiverId; // 接收者ID（私聊时使用）

    private String content; // 消息内容

    private String messageType; // 消息类型：public/private

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间
}