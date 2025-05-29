package com.DDT.javaWeb.service;

import com.DDT.javaWeb.dto.LoginDTO;
import com.DDT.javaWeb.dto.UserDTO;
import com.DDT.javaWeb.entity.User;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.vo.UserProfileVO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

public interface IUserService extends IService<User> {

    Result register(UserDTO userDTO);

    Result login(LoginDTO loginDTO);

    Result logout();

    Result<UserProfileVO> getUserProfile();
}
