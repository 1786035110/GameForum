package com.DDT.javaWeb.controller;

import com.DDT.javaWeb.dto.PostDTO;
import com.DDT.javaWeb.entity.Post;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.service.ICategoryService;
import com.DDT.javaWeb.service.ICommentService;
import com.DDT.javaWeb.service.IPostService;
import com.DDT.javaWeb.vo.CommentVO;
import com.DDT.javaWeb.vo.ForumCategoryVO;
import com.DDT.javaWeb.vo.LikeVO;
import com.DDT.javaWeb.vo.PostVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/forum")
@Slf4j
@Api(tags = "论坛模块")
public class ForumController {

    @Resource
    private ICategoryService categoryService;

    @Resource
    private IPostService postService;

    @Resource
    private ICommentService commentService;

    @GetMapping("/categories")
    @ApiOperation(value = "获取论坛分类")
    public Result<List<ForumCategoryVO>> getForumCategories() {
        log.info("获取论坛分类");
        return categoryService.getForumCategories();
    }

    @GetMapping("/posts")
    @ApiOperation(value = "获取论坛帖子")
    public Result<IPage<PostVO>> getForumPosts(@RequestParam Integer page, @RequestParam Integer size) {
        log.info("获取论坛帖子");
        IPage<PostVO> pageDTO = new Page <>(page, size);
        IPage<PostVO> list = postService.getForumPosts(pageDTO);
        return Result.success(list);
    }

    @GetMapping("/categories/{id}/posts")
    @ApiOperation(value = "获取指定分类的帖子")
    public Result<IPage<PostVO>> getPostsByCategory(@PathVariable("id") Long categoryId, @RequestParam Integer page, @RequestParam Integer size) {
        log.info("获取指定分类的帖子, categoryId: {}", categoryId);
        IPage<PostVO> pageDTO = new Page <>(page, size);
        IPage<PostVO> list = postService.getPostsByCategory(categoryId, pageDTO);
        return Result.success(list);
    }

    @GetMapping("/friends/posts")
    @ApiOperation(value = "获取好友的帖子")
    public Result<IPage<PostVO>> getFriendsPosts(@RequestParam Integer page, @RequestParam Integer size) {
        log.info("获取好友的帖子");
        IPage<PostVO> pageDTO = new Page <>(page, size);
        IPage<PostVO> list = postService.getPostsByFriends(pageDTO);
        return Result.success(list);
    }

    @GetMapping("/posts/{id}")
    @ApiOperation(value = "获取指定帖子的详情")
    public Result<PostVO> getPostById(@PathVariable("id") Long postId) {
        log.info("获取指定帖子的详情, postId: {}", postId);
        PostVO post = postService.getPostById(postId);
        return Result.success(post);
    }

    @PostMapping("/posts")
    @ApiOperation(value = "创建新的帖子")
    public Result<Long> createPost(@RequestBody PostDTO postDTO) {
        log.info("创建新的帖子, post: {}", postDTO);
        return Result.success(postService.createPost(postDTO));
    }

    @PostMapping("/posts/{id}/comments")
    @ApiOperation(value = "发表评论")
    public Result<CommentVO> createComment(@PathVariable("id") Long postId, @RequestParam String content) {
        log.info("发表评论, postId: {}, content: {}", postId, content);
        return commentService.createComment(postId, content);
    }

    @GetMapping("/posts/{id}/comments")
    @ApiOperation(value = "获取指定帖子的评论")
    public Result<List<CommentVO>> getCommentsByPostId(@PathVariable("id") Long postId) {
        log.info("获取指定帖子的评论, postId: {}", postId);
        return commentService.getCommentsByPostId(postId);
    }

    @PostMapping("/posts/{id}/like")
    @ApiOperation(value = "点赞帖子")
    public Result<LikeVO> likePost(@PathVariable("id") Long postId) {
        log.info("点赞帖子, postId: {}", postId);
        return postService.likePost(postId);
    }
}
