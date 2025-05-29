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

public class PostVO implements Serializable {
    private Long id; // 帖子ID
    private String title; // 帖子标题
    private String content; // 帖子内容
    private String summary; // 帖子摘要
    private String authorName; // 作者
    private Long authorId; // 作者ID
    private String categoryName; // 帖子分类
    private Long categoryId; // 分类ID
    private Integer commentCount; // 评论数
    private Integer viewCount; // 浏览量
    private Integer likeCount; // 点赞数
    private Boolean isLiked; // 是否点赞
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime; // 更新时间
}
