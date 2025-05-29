package com.DDT.javaWeb.service;

import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.vo.RequestVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.DDT.javaWeb.entity.FriendRequest;

import java.util.List;

public interface IFriendRequestService extends IService<FriendRequest> {

    Result<List<RequestVO>> getFriendRequestList();

    Result sendFriendRequest(String username);

    Result acceptFriendRequest(Long requestId);

    Result rejectFriendRequest(Long requestId);
}
