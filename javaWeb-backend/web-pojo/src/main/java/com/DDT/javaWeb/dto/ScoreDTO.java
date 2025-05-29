package com.DDT.javaWeb.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScoreDTO implements Serializable {
    private Integer score; // 分数
    private Integer duration; // 游戏时长
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "GMT+8")
    private LocalDateTime endTime; // 结束时间，接收ISO 8601格式
}
