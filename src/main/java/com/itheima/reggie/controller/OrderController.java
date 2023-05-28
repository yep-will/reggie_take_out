package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.OrdersDto;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单
 */
@Slf4j
@RestController
@RequestMapping("/order")
@Api(tags = "订单管理接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 用户下单
     */
    @PostMapping("/submit")
    @ApiOperation(value = "用户下单接口")
    public R<String> submit(@RequestBody Orders orders) {
        log.info("订单数据：{}", orders);
        orderService.submit(orders);
        return R.success("下单成功");
    }

    /**
     * 用户订单分页查询
     */
    @GetMapping("/userPage")
    @ApiOperation(value = "用户订单分页查询接口")
    public R<Page> userPage(int page, int pageSize) {
        log.info("page:{}, pageSize:{}", page, pageSize);

        //分页构造器对象
        Page<Orders> ordersPage = new Page<>(page, pageSize);

        //添加查询条件:1.用户id；2.订单更新时间降序
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Orders::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByDesc(Orders::getOrderTime);
        orderService.page(ordersPage, queryWrapper);

        //Dto订单分页构造器对象
        Page<OrdersDto> ordersDtoPage = new Page<>();
        BeanUtils.copyProperties(ordersPage, ordersDtoPage, "records");

        List<Orders> records = ordersPage.getRecords();    //获取数据进行加工

        //对数据加工并存入list中
        List<OrdersDto> list = records.stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            //对象拷贝
            BeanUtils.copyProperties(item, ordersDto);

            //统计订单内件数
            int sumNum = 0;
            LambdaQueryWrapper<OrderDetail> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.in(OrderDetail::getOrderId, item.getId());
            sumNum = orderDetailService.count(lambdaQueryWrapper);
            ordersDto.setSumNum(sumNum);
            //装填订单明细
            List<OrderDetail> orderDetailList = orderDetailService.list(lambdaQueryWrapper);
            ordersDto.setOrderDetails(orderDetailList);

            return ordersDto;
        }).collect(Collectors.toList());

        ordersDtoPage.setRecords(list);

        return R.success(ordersDtoPage);
    }


    /**
     * 后台管理订单信息分页查询
     */
    @GetMapping("/page")
    @ApiOperation(value = "后台管理订单信息分页查询接口")
    public R<Page> page(int page, int pageSize, String number, String beginTime, String endTime) {  //也可以写成Date类型
        //构造分页构造器
        Page<Orders> pageInfo = new Page(page, pageSize);

        //构造条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(number != null, Orders::getId, number);
        queryWrapper.ge(beginTime != null, Orders::getOrderTime, beginTime);
        queryWrapper.le(endTime != null, Orders::getOrderTime, endTime);
        //添加排序条件
        queryWrapper.orderByDesc(Orders::getCheckoutTime);

        //执行查询
        orderService.page(pageInfo, queryWrapper);

        List<Orders> records = pageInfo.getRecords();
        records = records.stream().map((item) -> {
            item.setUserName(item.getUserId().toString());
            return item;
        }).collect(Collectors.toList());

        return R.success(pageInfo);
    }

    /**
     * 派送订单状态更改
     */
    @PutMapping
    @ApiOperation(value = "派送订单状态更改接口")
    public R<String> updateOrder(@RequestBody Orders orders) {
        //构造条件构造器
        LambdaUpdateWrapper<Orders> updateWrapper = new LambdaUpdateWrapper<>();
        //添加过滤条件
        updateWrapper.eq(Orders::getId, orders.getId());
        updateWrapper.set(Orders::getStatus, orders.getStatus());
        orderService.update(updateWrapper);

        return R.success("订单派送成功");
    }

    /**
     * 再来一单
     */
    @PostMapping("/again")
    @ApiOperation(value = "再来一单接口")
    public R<String> again(@RequestBody Orders orders) {
        log.info(orders.toString());

        // 根据订单id获取订单
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getId, orders.getId());
        Orders one = orderService.getOne(queryWrapper);

        // 根据订单id获取订单明细集合
        LambdaQueryWrapper<OrderDetail> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(OrderDetail::getOrderId, one.getNumber());
        List<OrderDetail> orderDetails = orderDetailService.list(lambdaQueryWrapper);

        // 通过用户id把原来的购物车给清空
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        shoppingCartService.remove(shoppingCartLambdaQueryWrapper);

        //新建购物车集合
        List<ShoppingCart> shoppingCarts = new ArrayList<>();
        //遍历订单明细集合,将订单明细重新加入购物车集合
        for (OrderDetail orderDetail : orderDetails) {
            //得到菜品id或套餐id
            Long dishId = orderDetail.getDishId();
            Long setmealId = orderDetail.getSetmealId();

            //添加购物车部分属性
            ShoppingCart shoppingCart = new ShoppingCart();
            //设置用户id 指定当前是哪个用户的购物车数据
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setDishFlavor(orderDetail.getDishFlavor());
            shoppingCart.setNumber(orderDetail.getNumber());
            shoppingCart.setAmount(orderDetail.getAmount());
            shoppingCart.setCreateTime(LocalDateTime.now());
            if (dishId != null) {
                //订单明细元素中是菜品
                LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
                dishLambdaQueryWrapper.eq(Dish::getId, dishId);
                //根据订单明细集合中的单个元素获得单个菜品元素
                Dish dishone = dishService.getOne(dishLambdaQueryWrapper);
                //根据菜品信息添加购物车信息
                shoppingCart.setDishId(dishId);
                shoppingCart.setName(dishone.getName());
                shoppingCart.setImage(dishone.getImage());
                //调用保存购物车方法
                shoppingCarts.add(shoppingCart);
            } else if (setmealId != null) {
                //订单明细元素中是套餐
                LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
                setmealLambdaQueryWrapper.eq(Setmeal::getId, setmealId);
                //根据订单明细集合中的单个元素获得单个套餐元素
                Setmeal setmealone = setmealService.getOne(setmealLambdaQueryWrapper);
                //根据套餐信息添加购物车信息
                shoppingCart.setSetmealId(setmealId);
                shoppingCart.setName(setmealone.getName());
                shoppingCart.setImage(setmealone.getImage());
                //调用保存购物车方法
                shoppingCarts.add(shoppingCart);
            }
        }
        shoppingCartService.saveBatch(shoppingCarts);
        return R.success("操作成功");
    }

}