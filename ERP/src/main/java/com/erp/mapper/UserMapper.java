package com.erp.mapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
//import jdk.vm.ci.meta.Constant;
//import org.apache.coyote.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 
 * @since 2026-03-01
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    IPage pageC(IPage<User> page);

    IPage pageCC(IPage<User> page, @Param(Constants.WRAPPER) Wrapper wrapper);
}
