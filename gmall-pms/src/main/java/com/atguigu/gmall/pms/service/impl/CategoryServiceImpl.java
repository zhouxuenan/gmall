package com.atguigu.gmall.pms.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<CategoryEntity> queryCategory(Long parentId) {
        //构造查询条件
        QueryWrapper<CategoryEntity> wrapper = new QueryWrapper<>();
        //如果parentId为-1，说明用户没有传该字段，查询所有
        if(parentId != -1){
            wrapper.eq("parent_id", parentId);
        }
        return this.categoryMapper.selectList(wrapper);

    }

    @Override
    public List<CategoryEntity> queryCategoriesWithSub(Long pid) {
        return this.categoryMapper.queryCategoriesByPid(pid);
    }

    @Override
    public List<CategoryEntity> queryAllLvlCategoriesByCid3(Long cid) {
        //1.根据三级分类id查询三级分类
        CategoryEntity categoryEntity3 = this.getById(cid);
        if(categoryEntity3 == null){
            return null;
        }
        //2.根据二级分类id查询二级分类
        CategoryEntity categoryEntity2 = this.getById(categoryEntity3.getParentId());
        if(categoryEntity2 == null){
            return null;
        }
        //3.根据一级分类id查询一级分类
        CategoryEntity categoryEntity1 = this.getById(categoryEntity2.getParentId());
        return Arrays.asList(categoryEntity1,categoryEntity2,categoryEntity3);
    }

}