package com.itheima.reggie.dto;

import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Orders;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrdersDto extends Orders {
    //订单对应的订单明细
    private List<OrderDetail> orderDetailList = new ArrayList<>();

    //订单内件数
    private Integer sumNum;
}
