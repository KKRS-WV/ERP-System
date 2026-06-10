package com.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单（与前端 menuname、menuclick、menucomponent 等字段名保持一致）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Menu对象", description = "")
@TableName("menu")
public class Menu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "菜单编码")
    @TableField("menu_code")
    private String menucode;

    @ApiModelProperty(value = "菜单名字")
    @TableField("menu_name")
    private String menuname;

    @ApiModelProperty(value = "菜单级别")
    @TableField("menu_level")
    private String menulevel;

    @ApiModelProperty(value = "菜单的父code")
    @TableField("menu_parent_code")
    private String menuparentcode;

    @ApiModelProperty(value = "点击触发的路由名")
    @TableField("menu_click")
    private String menuclick;

    @ApiModelProperty(value = "权限 0超级管理员，1管理员，2普通用户，逗号组合")
    @TableField("menu_right")
    private String menuright;

    @TableField("menu_component")
    private String menucomponent;

    @TableField("menu_icon")
    private String menuicon;

    @TableField("sort_order")
    private Integer sortorder;
}
