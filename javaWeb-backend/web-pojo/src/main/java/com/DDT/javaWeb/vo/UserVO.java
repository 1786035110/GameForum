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
public class UserVO implements Serializable {
//避免返回敏感信息
    private Long userID; // 用户ID
    private String username; // 用户名
    private String token; // 用户token
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime; // 上次登录时间
}
