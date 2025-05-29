package com.DDT.javaWeb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeVO implements Serializable {
    private Integer likeCount; // 点赞数量
    private Boolean isLiked; // 是否已点赞
}
