package com.DDT.javaWeb.service;

import com.DDT.javaWeb.entity.Comment;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.vo.CommentVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ICommentService extends IService<Comment> {

    Result<CommentVO> createComment(Long postId, String content);

    Result<List<CommentVO>> getCommentsByPostId(Long postId);
}
