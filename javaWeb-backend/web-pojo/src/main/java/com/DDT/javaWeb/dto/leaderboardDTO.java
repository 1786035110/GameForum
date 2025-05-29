package com.DDT.javaWeb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class leaderboardDTO implements Serializable {
    private String timeRange; // 时间范围：all/week/month
    private Integer limit; // 限制返回记录数量
}
