package com.erp.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.QueryPageParam;
import com.erp.common.Result;
import com.erp.common.JwtUtil;
import com.erp.entity.Menu;
import com.erp.entity.User;
import com.erp.service.MenuService;
import com.erp.service.UserService;
import freemarker.template.utility.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 
 * @since 2026-03-01
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private MenuService menuService;
    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/list")
    public List<User> list(){
        return userService.list();
    }

    @GetMapping("/findByNo")
    public Result findByNo(@RequestParam String no){
       List list = userService.lambdaQuery().eq(User::getNo,no).list();
       return list.size()>0?Result.suc(list):Result.fail();
    }
//新增
    @PostMapping("/save")
    public Result save(@RequestBody User user){
        String authErr = assertSuperAdmin(user == null ? null : user.getOperatorId());
        if (authErr != null) {
            return Result.fail(authErr);
        }
        String roleErr = validateRole(user);
        if (roleErr != null) {
            return Result.fail(roleErr);
        }
        return userService.save(user)?Result.suc():Result.fail();
    }
//更新
    @PostMapping("/update")
    public Result update(@RequestBody User user){
        String authErr = assertSuperAdmin(user == null ? null : user.getOperatorId());
        if (authErr != null) {
            return Result.fail(authErr);
        }
        String roleErr = validateRole(user);
        if (roleErr != null) {
            return Result.fail(roleErr);
        }
        return userService.updateById(user)?Result.suc():Result.fail();
    }
//删除
    @DeleteMapping ("/del")
    public Result del(@RequestParam String id, @RequestParam(required = false) Integer operatorId){
        String authErr = assertSuperAdmin(operatorId);
        if (authErr != null) {
            return Result.fail(authErr);
        }
        return userService.removeById(id)?Result.suc():Result.fail();
    }

    @PostMapping("/login")
    public Result login(@RequestBody User user){
        List list = userService.lambdaQuery()
                .eq(User::getNo,user.getNo())
                .eq(User::getPassword,user.getPassword()).list();


        if (list.size()>0){
            User user1 = (User)list.get(0);
            if ("N".equalsIgnoreCase(user1.getIsValid())) {
                return Result.fail("账号已被停用");
            }
            List menuList = menuService.lambdaQuery().like(Menu::getMenuright,user1.getRoleId()).list();
            HashMap res =new HashMap<>();
            String token = jwtUtil.generateToken(user1);
            user1.setPassword(null);
            res.put("user",user1);
            res.put("menu",menuList);
            res.put("token", token);
            return Result.suc(res);
        }
        return Result.fail("用户名或密码错误");
    }

    @PostMapping("/mod")
    public boolean mod(@RequestBody User user){
        return userService.updateById(user);
    }
    @PostMapping("/saveOrMod")
    public boolean saveOrMod(@RequestBody User user){
        return userService.saveOrUpdate(user);
    }

    @PostMapping("/listP")
    public Result listP(@RequestBody User user){
        LambdaQueryWrapper<User> lambdaQueryWrapper=new LambdaQueryWrapper();
        if (StringUtils.isNotBlank(user.getName())){
            lambdaQueryWrapper.like(User::getName,user.getName());
        }

        return Result.suc(userService.list(lambdaQueryWrapper));
    }
    @PostMapping("/listPage")
   // public List<User> listpage(@RequestBody HashMap map){
    public List<User> listPage(@RequestBody QueryPageParam query){
        System.out.println(query);
        System.out.println("num="+query.getPageNum());
        System.out.println("size="+query.getPageSize());

        HashMap param =query.getParam();
        String name =(String)param.get("name");
        System.out.println("name="+(String)param.get("name"));
        //System.out.println("no="+(String)param.get("no"));

        Page<User> page =new Page(1,2);
        page.setCurrent(query.getPageNum());
        page.setSize(query.getPageSize());

        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper();
        lambdaQueryWrapper.like(User::getName,name);

        IPage result=userService.page(page,lambdaQueryWrapper);
        System.out.println("total="+result.getTotal());
        return result.getRecords();
    }

    @PostMapping("/listPageC")
    public List<User> listPageC(@RequestBody QueryPageParam query){
        HashMap param=query.getParam();
        String name=(String)param.get("name");
        System.out.println("name="+(String)param.get("name"));
        Page<User> page=new Page();
        page.setCurrent(query.getPageNum());
        page.setSize(query.getPageSize());

        LambdaQueryWrapper<User> lambdaQueryWrapper=new LambdaQueryWrapper();
        lambdaQueryWrapper.like(User::getName,name);

        IPage result=userService.pageCC(page,lambdaQueryWrapper);
        System.out.println("total="+result.getTotal());
        return result.getRecords();
    }

    @PostMapping("/listPageC1")
    public Result listPageC1(@RequestBody QueryPageParam query){
        HashMap param=query.getParam();
        String name=(String)param.get("name");
        String roleId=(String)param.get("roleId");
        Integer operatorId = parseIntFromMap(param, "operatorId");

        String authErr = assertSuperAdmin(operatorId);
        if (authErr != null) {
            return Result.fail(authErr);
        }


        Page<User> page=new Page();
        page.setCurrent(query.getPageNum());
        page.setSize(query.getPageSize());

        LambdaQueryWrapper<User> lambdaQueryWrapper=new LambdaQueryWrapper();
        if (StringUtils.isNotBlank(name) && !"null".equals(name)){
            lambdaQueryWrapper.like(User::getName,name);
        }
        if (StringUtils.isNotBlank(roleId)) {
            lambdaQueryWrapper.eq(User::getRoleId, roleId);
        }

        IPage result=userService.pageCC(page,lambdaQueryWrapper);
        System.out.println("total="+result.getTotal());
        return Result.suc(result.getRecords(), result.getTotal());
    }

    private String validateRole(User user) {
        if (user == null || user.getRoleId() == null) {
            return "角色不能为空";
        }
        Integer roleId = user.getRoleId();
        if (roleId < 0 || roleId > 2) {
            return "角色不合法";
        }
        return null;
    }

    private String assertSuperAdmin(Integer operatorId) {
        if (operatorId == null) {
            return "缺少操作人信息";
        }
        User op = userService.getById(operatorId);
        if (op == null || op.getRoleId() == null || op.getRoleId() != 0) {
            return "仅超级管理员可操作用户管理";
        }
        return null;
    }

    private static Integer parseIntFromMap(Map map, String key) {
        if (map == null || key == null) {
            return null;
        }
        Object val = map.get(key);
        if (val == null) {
            return null;
        }
        try {
            return Integer.valueOf(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

}
