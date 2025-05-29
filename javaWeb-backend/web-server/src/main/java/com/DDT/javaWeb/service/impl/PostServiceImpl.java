package com.DDT.javaWeb.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.DDT.javaWeb.dto.PostDTO;
import com.DDT.javaWeb.entity.Follow;
import com.DDT.javaWeb.entity.Post;
import com.DDT.javaWeb.entity.User;
import com.DDT.javaWeb.mapper.CategoryMapper;
import com.DDT.javaWeb.mapper.FollowMapper;
import com.DDT.javaWeb.mapper.PostMapper;

import com.DDT.javaWeb.mapper.UserMapper;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.service.IPostService;
import com.DDT.javaWeb.utils.UserHolder;
import com.DDT.javaWeb.vo.LikeVO;
import com.DDT.javaWeb.vo.PostVO;
import com.DDT.javaWeb.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.DDT.javaWeb.constant.RedisConstant.*;

@Service
@Slf4j
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements IPostService {

    @Resource
    private PostMapper postMapper;

    @Resource
    private FollowMapper followMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public IPage<PostVO> getForumPosts(IPage<PostVO> pageDTO) {
        return postMapper.getForumPosts(pageDTO);
    }

    @Override
    public IPage<PostVO> getPostsByCategory(Long categoryId, IPage<PostVO> pageDTO) {
        return postMapper.getPostsByCategory(categoryId, pageDTO);
    }

    @Override
    public IPage<PostVO> getPostsByFriends(IPage<PostVO> pageDTO) {
        Long userId = UserHolder.getUser().getUserID();
        LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Follow::getUserId, userId);
        List<Long> followIdList = followMapper.selectList(queryWrapper).stream()
                .map(Follow::getFollowUserId)
                .collect(Collectors.toList());

        return postMapper.getPostsByFriends(pageDTO, followIdList);
    }

    @Override
    public PostVO getPostById(Long postId) {
        // 1. 查询缓存
        String cacheKey = CACHE_POST_KEY + postId;
        String postJson = stringRedisTemplate.opsForValue().get(cacheKey);

        // 2. 在Redis中处理浏览计数器
        String viewCountKey = POST_VIEW_COUNT_KEY + postId;

        // 检查计数器是否存在，不存在则初始化
        Boolean hasKey = stringRedisTemplate.hasKey(viewCountKey);
        if (Boolean.FALSE.equals(hasKey)) {
            // 从数据库获取当前浏览量作为初始值
            Post post = postMapper.selectById(postId);
            if (post != null) {
                stringRedisTemplate.opsForValue().set(viewCountKey, String.valueOf(post.getViewCount()));
            } else {
                stringRedisTemplate.opsForValue().set(viewCountKey, "0");
            }
        }
        stringRedisTemplate.opsForValue().increment(viewCountKey);

        // 3. 缓存命中，直接返回
        if (StrUtil.isNotBlank(postJson)) {
            PostVO postVO = JSONUtil.toBean(postJson, PostVO.class);
            // 从Redis获取最新浏览次数
            String viewCountStr = stringRedisTemplate.opsForValue().get(viewCountKey);
            int viewCount = viewCountStr == null ? 0 : Integer.parseInt(viewCountStr);
            postVO.setViewCount(viewCount);
            return postVO;
        }

        // 4. 缓存未命中，查询数据库
        Post post = postMapper.selectById(postId);
        if (post == null) {
            stringRedisTemplate.opsForValue().set(cacheKey, "", 5, TimeUnit.MINUTES);
            return null;
        }

        // 5. 更新数据库浏览量（
        update().setSql("view_count = view_count + 1").eq("id", postId).update();

        // 6. 构建PostVO对象
        Long userId = UserHolder.getUser().getUserID();
        String key = BLOG_LIKED_KEY + postId;

        PostVO postVO = new PostVO();
        BeanUtil.copyProperties(post, postVO);
        postVO.setAuthorName(userMapper.selectById(post.getAuthorId()).getUsername());
        postVO.setCategoryName(categoryMapper.selectById(post.getCategoryId()).getName());
        postVO.setIsLiked(!BooleanUtil.isFalse(stringRedisTemplate.opsForSet().isMember(key, userId.toString())));

        // 从Redis获取最新浏览次数
        String viewCountStr = stringRedisTemplate.opsForValue().get(viewCountKey);
        int viewCount = viewCountStr == null ? 0 : Integer.parseInt(viewCountStr);
        postVO.setViewCount(viewCount);

        // 7. 存入缓存
        stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(postVO), CACHE_POST_TTL, TimeUnit.MINUTES);

        return postVO;
    }

    @Override
    public Long createPost(PostDTO postDTO) {
        UserVO user =  UserHolder.getUser();
        log.info("Creating post by user: {}", user);
        Long userId = user.getUserID();
        Post post = Post.builder()
                .authorId(userId)
                .content(postDTO.getContent())
                .summary(postDTO.getSummary())
                .title(postDTO.getTitle())
                .categoryId(postDTO.getCategoryId())
                .createTime(LocalDateTime.now())
                .build();
        postMapper.insert(post);

//        stringRedisTemplate.delete(CACHE_POST_LIST_KEY);
//        stringRedisTemplate.delete(CACHE_POST_CATEGORY_KEY + post.getCategoryId());

        return post.getId();
    }

    @Override
    public Result<LikeVO> likePost(Long postId) {
        // 1.获取登录用户
        Long userId = UserHolder.getUser().getUserID();
        // 2.判断当前登录用户是否已经点赞
        String key = BLOG_LIKED_KEY + postId;
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        if(BooleanUtil.isFalse(isMember)){
            //3.如果未点赞，可以点赞
            //3.1 数据库点赞数+1
            boolean isSuccess = update().setSql("like_count = like_count + 1").eq("id", postId).update();
            //3.2 保存用户到Redis的set集合
            if(isSuccess){
                stringRedisTemplate.opsForSet().add(key,userId.toString());
            }
        }else {
            //4.如果已点赞，取消点赞
            //4.1 数据库点赞数-1
            boolean isSuccess = update().setSql("like_count = like_count - 1").eq("id", postId).update();
            //4.2 把用户从Redis的set集合移除
            if (isSuccess) {
                stringRedisTemplate.opsForSet().remove(key, userId.toString());
            }
        }

        // 删除帖子缓存，确保下次获取时能获取最新点赞状态
        stringRedisTemplate.delete(CACHE_POST_KEY + postId);

        // 查询最新点赞数量
        Post post = postMapper.selectById(postId);

        //5.返回点赞结果
        LikeVO likeVO = new LikeVO();
        likeVO.setIsLiked(BooleanUtil.isFalse(isMember));
        likeVO.setLikeCount(post.getLikeCount());
        return Result.success(likeVO);
    }
}
