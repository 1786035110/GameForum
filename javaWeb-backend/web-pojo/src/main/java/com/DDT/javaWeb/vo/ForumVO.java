package com.DDT.javaWeb.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ForumVO implements Serializable {
    private Long id; // 论坛ID
    private String title; // 帖子标题
    private String summary; // 帖子摘要
    private String authorName; // 作者名称
    private String categoryName; // 分类
    private String username; // 用户名
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间
    private Integer likeCount; // 点赞数
    private Integer commentCount; // 评论数
    private Integer viewCount; // 浏览数
}
