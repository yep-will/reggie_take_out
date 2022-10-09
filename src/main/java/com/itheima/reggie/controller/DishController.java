package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
@Api(tags = "菜品管理接口")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 新增菜品
     *
     * @param dishDto
     * @return
     */
    @PostMapping
    @ApiOperation(value = "新增菜品接口")
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info(dishDto.toString());

        dishService.saveWithFlavor(dishDto);

        //清理所有菜品的缓存数据
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        //清理某个分类下面的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId();
        redisTemplate.delete(key);

        return R.success("新增菜品成功...");
    }

    /**
     * 菜品信息分页查询
     *
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    @ApiOperation(value = "菜品信息分页查询接口")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "page", value = "页码", required = true),
        @ApiImplicitParam(name = "pageSize", value = "每页记录数", required = true),
        @ApiImplicitParam(name = "name", value = "菜品名称", required = false)
    })
    public R<Page> page(int page, int pageSize, String name) {
        //构造分页构造器对象
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>();  //用来处理categoryName属性

        //先查询pageInfo对象（不包含categoryName的查询封装）
        //构造条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(name != null, Dish::getName, name);
        //添加排序条件
        queryWrapper.orderByDesc(Dish::getUpdateTime);
        //执行分页查询
        dishService.page(pageInfo, queryWrapper);

        //对象拷贝：除了records字段，即查询条数，总页数，当前页码等数据
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

        List<Dish> records = pageInfo.getRecords();
        //对records数据进行categoryName属性处理
        List<DishDto> list = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto); //将records的数据拷贝到dishDto中
            Long categoryId = item.getCategoryId();  //获取单个菜品分类id

            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "菜品信息id查询接口")
    public R<DishDto> get(@PathVariable Long id) {
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 修改菜品
     *
     * @param dishDto
     * @return
     */
    @PutMapping
    @ApiOperation(value = "菜品信息修改接口")
    public R<String> update(@RequestBody DishDto dishDto) {
        log.info(dishDto.toString());

        dishService.updateWithFlavor(dishDto);

        //清理所有菜品的缓存数据
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        //清理某个分类下面的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId();
        redisTemplate.delete(key);

        return R.success("修改菜品成功...");
    }

    /**
     * 根据条件查询对应的菜品数据
     * @param dish
     * @return
     */
//    @GetMapping("/list")
//    public R<List<Dish>> list(Dish dish){
//        //构造查询条件
//        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
//
//        //添加条件：查询状态为1（起售状态）的菜品
//        queryWrapper.eq(Dish::getStatus, 1);
//
//        //添加排序条件
//        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
//
//        List<Dish> list = dishService.list(queryWrapper);
//
//        return R.success(list);
//    }
    @GetMapping("/list")
    @ApiOperation(value = "菜品数据查询接口")
    public R<List<DishDto>> list(Dish dish) {
        List<DishDto> dishDtoList = null;

        //动态构造key
        String key = "dish_" + dish.getCategoryId();
        //先从redis中获取缓存数据
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);

        if (dishDtoList != null){
            //如果存在，直接返回，无需查询数据库
            return R.success(dishDtoList);
        }

        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        //添加条件，查询状态为1（起售状态）的菜品
        queryWrapper.eq(Dish::getStatus, 1);

        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        dishDtoList = list.stream().map((item) -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item, dishDto);

            Long categoryId = item.getCategoryId(); //获取当前菜品的分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);

            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);   //设置当前菜品的分类名
            }

            //根据当前菜品的id获取当前菜品的口味信息
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(DishFlavor::getDishId, dishId);
            //SQL:select * from dish_flavor where dish_id = ?
            List<DishFlavor> dishFlavorList = dishFlavorService.list(lambdaQueryWrapper);
            dishDto.setFlavors(dishFlavorList);
            return dishDto;
        }).collect(Collectors.toList());

        //如果不存在，需要查询数据库，将查询到的菜品数据缓存到redis
        redisTemplate.opsForValue().set(key, dishDtoList, 60, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }

    /**
     * 菜品删除（同时删除redis中相应的套餐数据），删除前需要检查套餐中是否含有相应菜品数据
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation(value = "菜品删除接口")
    public R<String> delete(@RequestParam List<Long> ids) {
        log.info("ids: {}", ids);

        //先在套餐中查询是否有相应的菜品，如果有则提示先将套餐中的菜品删除
        //获取不符合条件菜品对应的套餐菜品关联数据
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.in(SetmealDish::getDishId, ids);
        List<SetmealDish> list = setmealDishService.list(queryWrapper);
        if (list.size() > 0){
            //获取套餐信息
            Setmeal setmeal = setmealService.getById(list.get(0).getSetmealId());
            return R.error("删除失败， " + setmeal.getName() + " 套餐中含有菜品:"+ list.get(0).getName() + "， 请先在套餐中移除该菜品");
        }

        //在redis中删除相应菜品对应分类数据
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(Dish::getId, ids);
        List<Dish> dishList = dishService.list(lambdaQueryWrapper);
        Set<Long> categorySet = new HashSet<>();
        for (Dish dish : dishList){
            categorySet.add(dish.getCategoryId());
        }
        for (Long categoryId : categorySet){
            String key = "dish_" + categoryId;
            redisTemplate.delete(key);
        }

        //菜品删除操作
        dishService.removeByIds(ids);

        return R.success("菜品数据删除成功");
    }

    /**
     * 菜品批量启售停售
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation(value = "批量启售停售")
    public R<String> updateStatusById(@PathVariable Integer status, Long[] ids){
        log.info("根据id修改菜品的启售停售：{}, id为{}", status, ids);
        int flag = 0;

        for(Long id : ids){
            Dish dish =  dishService.getById(id);
            log.info("套餐：{}", dish);
            dish.setStatus(status);
            flag = dish.getStatus();
            dishService.updateById(dish);
        }

        if (flag == 0){
            return R.success("菜品停售成功");
        }
        return R.success("菜品启售成功");
    }

}













