package com.itheima.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.reggie.entity.Employee;
import org.apache.ibatis.annotations.Mapper;

@Mapper  //这里相当于mybatis-plus刚学时的dao接口
public interface EmployeeMapper extends BaseMapper<Employee> {
}
