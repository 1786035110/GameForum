package com.DDT.javaWeb.service;

import com.DDT.javaWeb.entity.Follow;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.vo.FriendVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IFollowService extends IService<Follow> {
    Result<List<FriendVO>> getFriendList();

    Result deleteFriend(Long friendId);
}
