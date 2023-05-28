package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 购物车
 */
@Slf4j
@RestController
@RequestMapping("/shoppingCart")
@Api(tags = "购物车接口")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 添加购物车
     */
    @PostMapping("/add")
    @ApiOperation(value = "添加购物车")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart) {
        log.info("购物车数据:{}", shoppingCart);

        // 获取用户id，指定当前是哪个用户的购物车数据
        Long currentId = BaseContext.getCurrentId();   //通过session方式获得也可以
        shoppingCart.setUserId(currentId);
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, currentId);
        if (null != shoppingCart.getDishId()) {
            // 添加到购物车的是菜品
            queryWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        } else if (null != shoppingCart.getSetmealId()) {
            // 添加到购物车的是套餐
            queryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }

        // 查询当前菜品或者套餐是否在购物车中（只看userid和setmealid即可）
        // SQL:select * from shopping_cart where user_id = ? and dish_id/setmeal_id = ?
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);

        if (cartServiceOne != null) {
            // 如果已经存在，就在原来数量基础上加一
            Integer number = cartServiceOne.getNumber();
            cartServiceOne.setNumber(number + 1);
            cartServiceOne.setCreateTime(LocalDateTime.now());   //更新入库时间
            shoppingCartService.updateById(cartServiceOne);
        } else {
            // 如果不存在，则添加到购物车，数量默认就是一
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());   //手动添加入库时间
            shoppingCartService.save(shoppingCart);
            cartServiceOne = shoppingCart;
        }

        return R.success(cartServiceOne);
    }

    /**
     * 购物车数量减一
     */
    @PostMapping("/sub")
    @ApiOperation(value = "减少购物车接口")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart) {
        log.info("购物车数据:{}", shoppingCart);

        //设置用户id，指定当前是哪个用户的购物车数据
        Long currentId = BaseContext.getCurrentId();   //通过session方式获得也可以
        shoppingCart.setUserId(currentId);

        //添加条件：设置当前用户id
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, currentId);

        if (null != shoppingCart.getDishId()) {
            //购物车的是菜品
            queryWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        } else if (null != shoppingCart.getSetmealId()) {
            //购物车的是套餐
            queryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }

        //查询当前菜品或者套餐是否在购物车中（只看userid和setmealid即可）
        //SQL:select * from shopping_cart where user_id = ? and dish_id/setmeal_id = ?
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);

        if (cartServiceOne != null) {
            if (cartServiceOne.getNumber() == 1) {
                //如果数量是1，再减一就直接将该购物车删除
                shoppingCartService.remove(queryWrapper);
                return R.success(shoppingCart);
            }
            //如果已经存在，就在原来数量基础上减一
            Integer number = cartServiceOne.getNumber();
            cartServiceOne.setNumber(number - 1);
            cartServiceOne.setCreateTime(LocalDateTime.now());   //更新入库时间
            shoppingCartService.updateById(cartServiceOne);
        } else {
            //如果不存在，直接返回
            return R.error("暂未添加至购物车");
        }

        return R.success(cartServiceOne);
    }

    /**
     * 查看购物车
     */
    @GetMapping("/list")
    @ApiOperation(value = "查看购物车列表接口")
    public R<List<ShoppingCart>> list() {
        log.info("查看购物车...");

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        // 获取当前用户id
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByAsc(ShoppingCart::getCreateTime);   //时间升序

        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);

        return R.success(list);
    }

    /**
     * 清空购物车
     */
    @DeleteMapping("/clean")
    @ApiOperation(value = "清空购物车接口")
    public R<String> clean() {
        //SQL:delete from shopping_cart where user_id = ?
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());

        shoppingCartService.remove(queryWrapper);

        return R.success("清空购物车成功");
    }
}
