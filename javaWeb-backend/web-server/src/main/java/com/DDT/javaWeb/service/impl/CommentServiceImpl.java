package com.DDT.javaWeb.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.DDT.javaWeb.entity.Comment;
import com.DDT.javaWeb.entity.Post;
import com.DDT.javaWeb.entity.User;
import com.DDT.javaWeb.mapper.CommentMapper;
import com.DDT.javaWeb.mapper.PostMapper;
import com.DDT.javaWeb.mapper.UserMapper;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.service.ICommentService;
import com.DDT.javaWeb.utils.UserHolder;
import com.DDT.javaWeb.vo.CommentVO;
import com.DDT.javaWeb.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.DDT.javaWeb.constant.RedisConstant.CACHE_POST_COMMENTS_KEY;
import static com.DDT.javaWeb.constant.RedisConstant.CACHE_POST_COMMENTS_TTL;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService {

    @Resource
    private UserMapper userMapper;
    
    @Resource
    private PostMapper postMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result<CommentVO> createComment(Long postId, String content) {
        // 1. 获取当前登录用户的ID
        UserVO user = UserHolder.getUser();

        // 2. 创建评论对象
        Comment comment = Comment.builder()
                .postId(postId)
                .userId(user.getUserID())
                .content(content)
                .createTime(LocalDateTime.now())
                .build();

        // 3. 保存评论到数据库
        this.save(comment);

        // 4. 构建 CommentVO 对象
        CommentVO commentVO = CommentVO.builder()
                .id(comment.getId())
                .content(content)
                .authorName(user.getUsername())
                .userId(user.getUserID())
                .createTime(comment.getCreateTime())
                .build();
        
        // 5. 更新帖子评论数
        Post post = postMapper.selectById(postId);
        post.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(post);

        String cacheKey = CACHE_POST_COMMENTS_KEY + postId;
        stringRedisTemplate.delete(cacheKey);
                
        return Result.success(commentVO);
    }

    @Override
    public Result<List<CommentVO>> getCommentsByPostId(Long postId) {

        // 先查缓存
        String cacheKey = CACHE_POST_COMMENTS_KEY + postId;
        String commentsJson = stringRedisTemplate.opsForValue().get(cacheKey);

        if (StrUtil.isNotBlank(commentsJson)) {
            List<CommentVO> cacheList = JSONUtil.toList(commentsJson, CommentVO.class);
            return Result.success(cacheList);
        }

        // 1. 查询评论列表
        List<Comment> list = this.list(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .orderByDesc(Comment::getCreateTime));
        // 2. 如果评论列表为空，返回空结果
        if (list.isEmpty()) {
            stringRedisTemplate.opsForValue().set(cacheKey, "[]", 5, TimeUnit.MINUTES);
            return Result.success(null);
        }
        // 3. 将 Comment 转换为 CommentVO
        List<CommentVO> commentVOList = list.stream().map(comment -> {
            User user = userMapper.selectById(comment.getUserId());
            return CommentVO.builder()
                    .id(comment.getId())
                    .content(comment.getContent())
                    .authorName(user.getUsername())
                    .userId(comment.getUserId())
                    .createTime(comment.getCreateTime())
                    .build();
        }).collect(Collectors.toList());

        String jsonStr = JSONUtil.toJsonStr(commentVOList);
        stringRedisTemplate.opsForValue().set(
                cacheKey,
                jsonStr,
                CACHE_POST_COMMENTS_TTL,
                TimeUnit.HOURS);

        return Result.success(commentVOList);
    }
}
