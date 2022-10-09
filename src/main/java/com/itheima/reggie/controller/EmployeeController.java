package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.service.EmployeeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/employee")
@Api(tags = "员工管理接口")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录
     * @param request
     * @param employee  传递的参数是json形式，同时注意转递的数据“主键”名和实体类中的属性名应该一致，否则会封装失败
     * @return
     */
    @PostMapping("/login")
    @ApiOperation(value = "员工登录接口")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee){
        //1.将页面提交的密码password进行MD5加密处理
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2.根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> queryWrapper= new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper); //emp是实际查询到的真实用户数据

        //3.如果没有查询到则返回登录失败结果
        if (emp == null){
            return R.error("此用户不存在...");
        }

        //4.密码比对，如果不一致则返回登录失败结果
        if ((!emp.getPassword().equals(password))){
            return R.error("密码错误...");
        }

        //5.查看员工状态，如果是已经禁用状态，则返回员工共已禁用结果
        if(emp.getStatus() == 0){
            return R.error("账号已禁用");
        }

        //6.登录成功，将员工id存入Session并返回登录成功结果 --> 存的是员工id，不是全部信息
        request.getSession().setAttribute("employee", emp.getId());
        return R.success(emp);
    }

    /**
     * 员工退出
     * @param request
     * @return
     */
    @PostMapping("/logout")
    @ApiOperation(value = "员工登出接口")
    public R<String> logout(HttpServletRequest request){
        //清理Session中保存的当前登录员工的id
        request.getSession().removeAttribute("employee");
        return R.success("退出成功");
    }

    /**
     * 新增员工
     * @param employee
     * @return
     */
    @PostMapping
    @ApiOperation(value = "新增员工接口")
    public R<String> save(HttpServletRequest request, @RequestBody Employee employee){
        log.info("新增员工，员工信息：{}", employee.toString());

        //设置初始密码123456，进行MD5加密
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

//        //获得当前登录用户的id
//        Long empId = (Long) request.getSession().getAttribute("employee");
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());
//        employee.setCreateUser(empId);
//        employee.setUpdateUser(empId);

        employeeService.save(employee);
        return R.success("新增员工成功");
    }

    /**
     * 员工信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    @ApiOperation(value = "员工信息分页查询接口")
    public R<Page> page(int page, int pageSize, String name){
        log.info("page = {}, pageSize = {}, name = {}", page, pageSize, name);

        //构造分页构造器
        Page pageInfo = new Page(page, pageSize);

        //构造条件构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();

        //添加过滤条件(考虑name为null的条件)
        queryWrapper.like(StringUtils.isNotEmpty(name), Employee::getName, name);

        //添加排序条件
        queryWrapper.orderByDesc(Employee::getUpdateTime);

        //执行查询
        employeeService.page(pageInfo, queryWrapper);

        return R.success(pageInfo);
    }

    /**
     * 根据id修改员工信息  --> 这里的修改方法是通用的
     * @param employee
     * @return
     */
    @PutMapping
    @ApiOperation(value = "修改员工信息接口")
    public R<String> update(@RequestBody Employee employee){
        log.info(employee.toString());

//        Long empId = (Long) request.getSession().getAttribute("employee");
//        employee.setUpdateTime(LocalDateTime.now());
//        employee.setUpdateUser(empId);
        employeeService.updateById(employee);

        return R.success("员工信息修改成功...");
    }

    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "id查询员工信息接口")
    public R<Employee> getById(@PathVariable Long id){
        log.info("根据id查询员工信息...{}", id);
        Employee employee = employeeService.getById(id);
        if (employee != null){
            return R.success(employee);
        }
        return R.error("未查询到对应的员工信息...");
    }


    /**
     * （批量）删除员工-----------------------------------------（未测试，俺也不知道哪里用到了）
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation(value = "删除员工")
    public R<String> delete(@RequestParam List<Long> ids){
        log.info("ids: {}", ids);

        employeeService.removeByIds(ids);

        return R.success("员工删除成功...");
    }

    /**
     * 查询员工列表数据-----------------------------------------（未测试，俺也不知道哪里用到了）
     * @param employee
     * @return
     */
    @GetMapping("/list")
    @ApiOperation(value = "员工列表查询接口")
    public R<List<Employee>> list(Employee employee){
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(employee.getId() != null, Employee::getUpdateTime, employee.getId());
        queryWrapper.eq(employee.getStatus() != null, Employee::getStatus, employee.getStatus());
        queryWrapper.orderByDesc(Employee::getUpdateTime);

        List<Employee> list = employeeService.list(queryWrapper);

        return R.success(list);
    }

    /**
     * 员工修改密码-----------------------------------------（未测试，俺也不知道哪里用到了）
     * @param employee
     * @return
     */
    @PutMapping("/updatePassword")
    @ApiOperation(value = "员工修改密码")
    public R<String> updatePassword(@RequestBody Employee employee){
        log.info("修改密码，员工信息：{}", employee.toString());

        employeeService.updateById(employee);

        return R.success("修改密码成功");
    }
}




























