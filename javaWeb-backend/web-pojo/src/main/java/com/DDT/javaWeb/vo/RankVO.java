package com.DDT.javaWeb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankVO implements Serializable {
    private Integer rank; // 排名
    private Boolean isNewRecord; // 是否新纪录
}
