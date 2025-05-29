package com.DDT.javaWeb.controller;

import com.DDT.javaWeb.dto.LoginDTO;
import com.DDT.javaWeb.dto.UserDTO;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.service.IUserService;
import com.DDT.javaWeb.vo.UserProfileVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/user")
@Api(tags = "用户接口")
@Slf4j
public class UserController {

    @Resource
    private IUserService userService;

    @PostMapping("/register")
    @ApiOperation("用户注册")
    public Result register(@RequestBody UserDTO userDTO) {
        log.info("用户注册：{}", userDTO);
        return userService.register(userDTO);
    }

    @PostMapping("/login")
    @ApiOperation("用户登录")
    public Result login(@RequestBody LoginDTO loginDTO) {
        log.info("用户登录：{}", loginDTO);
        return userService.login(loginDTO);
    }

    @PostMapping("/logout")
    @ApiOperation("用户登出")
    public Result logout() {
        log.info("用户登出");
        return userService.logout();
    }

    @GetMapping("/profile")
    @ApiOperation("获取用户信息")
    public Result<UserProfileVO> getUserProfile() {
        log.info("获取用户信息");
        return userService.getUserProfile();
    }

}
