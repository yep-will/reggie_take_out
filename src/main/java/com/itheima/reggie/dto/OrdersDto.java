package com.itheima.reggie.dto;

import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Orders;
import lombok.Data;
import java.util.List;

@Data
public class OrdersDto extends Orders {
    //用户名
    private String userName;

    //用户电话
    private String phone;

    //收获地址
    private String address;

    //收货人
    private String consignee;

    //订单对应的订单明细(前端使用的变量名就是orderDetails！所以更改其它变量名前端无法显示)
    private List<OrderDetail> orderDetails;

    //订单内件数
    private Integer sumNum;
}
