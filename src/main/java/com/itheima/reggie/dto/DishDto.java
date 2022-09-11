package com.itheima.reggie.dto;

import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO,全称为Data Transfer Object,即数据传输对象，一般用于展示层与服务层之间的数据传输
 */
@Data
public class DishDto extends Dish {

    //菜品对应的口味数据
    private List<DishFlavor> flavors = new ArrayList<>();


    //菜品对应的分类名称
    private String categoryName;

    //菜品的份数
    private Integer copies;
}
