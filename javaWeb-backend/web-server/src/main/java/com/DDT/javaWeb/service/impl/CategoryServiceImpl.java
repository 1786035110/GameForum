package com.DDT.javaWeb.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.DDT.javaWeb.entity.Category;
import com.DDT.javaWeb.mapper.CategoryMapper;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.service.ICategoryService;
import com.DDT.javaWeb.vo.ForumCategoryVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.DDT.javaWeb.constant.RedisConstant.*;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result<List<ForumCategoryVO>> getForumCategories() {

        //先查询缓存
        String categoriesJson = stringRedisTemplate.opsForValue().get(CACHE_FORUM_CATEGORIES_KEY);

        if (StrUtil.isNotBlank(categoriesJson)) {
            List<ForumCategoryVO> cacheList = JSONUtil.toList(categoriesJson, ForumCategoryVO.class);
            return Result.success(cacheList);
        }

        // 1.查询所有的分类
        List<ForumCategoryVO> categories = this.list().stream()
                .map(category -> {
                    // 2.将分类转换为VO对象
                    ForumCategoryVO categoryVO = new ForumCategoryVO();
                    categoryVO.setId(category.getId());
                    categoryVO.setName(category.getName());
                    categoryVO.setSortOrder(category.getSortOrder());
                    return categoryVO;
                }).collect(Collectors.toList());

        String jsonStr = JSONUtil.toJsonStr(categories);
        stringRedisTemplate.opsForValue().set(
                CACHE_FORUM_CATEGORIES_KEY,
                jsonStr,
                LOGIN_USER_TTL,
                TimeUnit.MINUTES);

        return Result.success(categories);
    }
}
