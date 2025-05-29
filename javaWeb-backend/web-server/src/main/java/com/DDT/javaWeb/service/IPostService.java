package com.DDT.javaWeb.service;

import com.DDT.javaWeb.dto.PostDTO;
import com.DDT.javaWeb.entity.Post;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.vo.LikeVO;
import com.DDT.javaWeb.vo.PostVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IPostService extends IService<Post> {

    IPage<PostVO> getForumPosts(IPage<PostVO> pageDTO);

    IPage<PostVO> getPostsByCategory(Long categoryId, IPage<PostVO> pageDTO);

    IPage<PostVO> getPostsByFriends(IPage<PostVO> pageDTO);

    PostVO getPostById(Long postId);

    Long createPost(PostDTO postDTO);

    Result<LikeVO> likePost(Long postId);
}
