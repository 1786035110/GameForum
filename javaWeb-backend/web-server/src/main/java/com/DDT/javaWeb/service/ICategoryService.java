package com.DDT.javaWeb.service;

import com.DDT.javaWeb.entity.Category;
import com.DDT.javaWeb.result.Result;
import com.DDT.javaWeb.vo.ForumCategoryVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ICategoryService extends IService<Category> {
    Result<List<ForumCategoryVO>> getForumCategories();
}
