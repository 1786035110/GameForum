package com.DDT.javaWeb.mapper;

import com.DDT.javaWeb.entity.Post;
import com.DDT.javaWeb.vo.PostVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.ArrayList;
import java.util.List;

public interface PostMapper extends BaseMapper<Post> {


    IPage<PostVO> getForumPosts(IPage<PostVO> pageDTO);

    IPage<PostVO> getPostsByCategory(Long categoryId, IPage<PostVO> pageDTO);


    IPage<PostVO> getPostsByFriends(IPage<PostVO> pageDTO, List<Long> followIdList);

    void updateViewCount(Long postId, int count);
}
