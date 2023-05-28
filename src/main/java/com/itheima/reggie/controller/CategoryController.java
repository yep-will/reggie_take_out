package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理
 */
@RestController
@RequestMapping("/category")
@Slf4j
@Api(tags = "分类管理接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增分类
     * @param category
     * @return
     */
    @PostMapping
    @ApiOperation(value = "新增分类")
    public R<String> save(@RequestBody Category category){
        log.info("category:{}", category);
        categoryService.save(category);
        return R.success("新增分类成功");
    }

    /**
     * 分类的分页查询
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    @ApiOperation(value = "分类分页查询")
    public R<Page> page(int page, int pageSize){
        //分页构造器
        Page<Category> pageInfo = new Page<>(page, pageSize);

        //条件构造器
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();

        //根据sort添加排序条件
        queryWrapper.orderByAsc(Category::getSort);

        //分页查询
        categoryService.page(pageInfo, queryWrapper);

        return R.success(pageInfo);
    }

    /**
     * 根据id删除分类
     */
    @DeleteMapping
    @ApiOperation(value = "删除分类")
    public R<String> delete(Long id){
        log.info("删除分类：id为：{}", id);
        // 使用自定义业务逻辑进行删除
        categoryService.remove(id);
        //categoryService.removeById(ids);
        return R.success("分类信息删除成功...");
    }

    /**
     * 根据id修改分类信息
     * @param category
     * @return
     */
    @PutMapping
    @ApiOperation(value = "修改分类")
    public R<String> update(@RequestBody Category category){
        log.info("修改分类信息：{}", category);

        categoryService.updateById(category);

        return R.success("修改分类信息成功");
    }

    /**
     * 根据条件查询分类数据
     * @param category
     * @return
     */
    @GetMapping("/list")
    @ApiOperation(value = "查询分类列表")
    public R<List<Category>> list(Category category){
        //条件构造器
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();

        //添加条件:按type条件查询
        queryWrapper.eq(category.getType() != null, Category::getType, category.getType());

        //添加排序条件：如果sort相同，则按更新时间降序排列
        queryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);

        List<Category> list = categoryService.list(queryWrapper);

        return R.success(list);
    }

    /**
     * 根据id查询分类数据------------------------------------------（未测试，俺也不知道哪里用到了）
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "根据id查询分类数据")
    public R<Category> getById(@PathVariable Long id){
        log.info(Long.toString(id));
        Category category = categoryService.getById(id);
        return R.success(category);
    }

}











