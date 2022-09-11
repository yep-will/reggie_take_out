package com.itheima.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 员工实体类
 */
@Data
public class Employee implements Serializable {
/*
    这个serialVersionUID是用来辅助对象的序列化与反序列化的，原则上序列化后的数据当中的serialVersionUID与
    当前类当中的serialVersionUID一致，那么该对象才能被反序列化成功。这个serialVersionUID的详细的工作机制
    是：在序列化的时候系统将serialVersionUID写入到序列化的文件中去，当反序列化的时候系统会先去检测文件中的
    serialVersionUID是否跟当前的文件的serialVersionUID是否一致，如果一直则反序列化成功，否则就说明当前类
    跟序列化后的类发生了变化，比如是成员变量的数量或者是类型发生了变化，那么在反序列化时就会发生crash，并且
    会报出错误。
 */

    private static final long serialVersionUID = 1L;  //Java的序列化机制是通过判断类的serialVersionUID来验证版本一致性的...

    private Long id;

    private String username;

    private String name;

    private String password;

    private String phone;

    private String sex;

    private String idNumber;  //身份证号码，驼峰命名法

    private Integer status;

    @TableField(fill = FieldFill.INSERT)  //插入时填充字段，结合MyMetaObjectHandler使用
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)  //插入和更新时填充字段
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)   //插入时填充字段
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)  //插入和更新时填充字段
    private Long updateUser;

}
