package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */
@RestController
@RequestMapping("/setmeal")
@Slf4j
@Api(tags = "套餐相关接口")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishService dishService;

    /**
     * 新增套餐
     * @param setmealDto
     * @return
     */
    @PostMapping
    //@CacheEvict(value = "setmealCache", allEntries = true)   //删除所有setmealCache分类下的所有数据
    @ApiOperation(value = "新增套餐接口")
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        log.info("套餐信息：{}", setmealDto);

        setmealService.saveWithDish(setmealDto);

        return R.success("新增套餐成功...");
    }

    /**
     * 套餐分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    @ApiOperation(value = "套餐分页查询接口")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "page", value = "页码", required = true),
        @ApiImplicitParam(name = "pageSize", value = "每页记录数", required = true),
        @ApiImplicitParam(name = "name", value = "套餐名称", required = false)
    })
    public R<Page> page(int page, int pageSize, String name) {
        //分页构造器对象
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);
        Page<SetmealDto> dtoPage = new Page<>();

        //添加查询条件，根据name进行like模糊查询
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null, Setmeal::getName, name);
        //添加排序条件，根据更新时间降序排序
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        setmealService.page(pageInfo, queryWrapper);

        //对象拷贝:records数据不拷贝，而且泛型不一致也无法拷贝数据
        BeanUtils.copyProperties(pageInfo, dtoPage, "records");
        List<Setmeal> records = pageInfo.getRecords();    //获取数据进行加工

        //对数据加工并存入list中
        List<SetmealDto> list = records.stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            //对象拷贝
            BeanUtils.copyProperties(item, setmealDto);
            //分类id
            Long categoryId = item.getCategoryId();
            //根据分类id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                //分类名称
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());

        dtoPage.setRecords(list);

        return R.success(dtoPage);
    }

    /**
     * 删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    //@CacheEvict(value = "setmealCache", allEntries = true)   //删除所有setmealCache分类下的所有数据
    @ApiOperation(value = "套餐删除接口")
    public R<String> delete(@RequestParam List<Long> ids) {
        log.info("ids: {}", ids);

        setmealService.removeWithDish(ids);

        return R.success("套餐数据删除成功...");
    }

    /**
     * 套餐批量启售停售
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    //停售请求路径为：http://localhost:8080/setmeal/status/0?ids=156716415,141558011
    //启售请求路径为：http://localhost:8080/setmeal/status/1?ids=156716415,141558011
    @ApiOperation(value = "套餐批量启售停售")
    public R<String> updateStatusById(@PathVariable Integer status, Long[] ids){
        log.info("根据id修改套餐的启售停售卖：{}, id为{}", status, ids);
        int flag = 0;

        for(Long id : ids){
            Setmeal setmeal =  setmealService.getById(id);
            setmeal.setStatus(status);
            flag = setmeal.getStatus();
            setmealService.updateById(setmeal);
        }

        if (flag == 0){
            return R.success("套餐停售成功");
        }
        return R.success("套餐启售成功");
    }

    /**
     * 查询相应套餐列表数据
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    //@Cacheable(value = "setmealCache", key = "#setmeal.categoryId + '_' + #setmeal.status")
    @ApiOperation(value = "套餐列表查询接口")
    public R<List<Setmeal>> list(Setmeal setmeal){
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> list = setmealService.list(queryWrapper);

        return R.success(list);
    }

    /**
     * 套餐修改接口
     * @param setmealDto
     * @return
     */
    @PutMapping
    @ApiOperation(value = "套餐修改接口")
    public R<String> update(@RequestBody SetmealDto setmealDto){
        log.info("套餐信息：{}", setmealDto);

       setmealService.updateWithDish(setmealDto);

        return R.success("套餐修改成功");
    }

    /**
     * 套餐查询接口
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "套餐查询接口")
    public R<SetmealDto> getById(@PathVariable Long id){
        log.info("根据id查询套餐信息...{}", id);

        Setmeal setmeal = setmealService.getById(id);

        SetmealDto setmealDto = new SetmealDto();

        //对象拷贝:records数据不拷贝，而且泛型不一致也无法拷贝数据
        BeanUtils.copyProperties(setmeal, setmealDto);

        //构造查询条件：查找套餐id下对应的菜品
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> setmealDishes = setmealDishService.list(queryWrapper);

        //菜品装填
        setmealDto.setSetmealDishes(setmealDishes);

        return R.success(setmealDto);
    }

    /**
     * 套餐查询全部菜品接口
     * @param id
     * @return
     */
    @GetMapping("/dish/{id}")
    @ApiOperation(value = "套餐查询全部菜品")
    public R<List<Dish>> findAllDish(@PathVariable Long id){
        log.info("根据id查询套餐下菜品信息...{}", id);

        //获取套餐关联菜品信息
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> setmealDishes = setmealDishService.list(queryWrapper);

        //提取关联菜品id
        List<Long> dishIds = new ArrayList<>();
        for(SetmealDish setmealDish : setmealDishes){
            dishIds.add(setmealDish.getDishId());
        }

        //获取菜品信息
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(Dish::getId, dishIds);
        List<Dish> dishes = dishService.list(lambdaQueryWrapper);

        return R.success(dishes);
    }

}
