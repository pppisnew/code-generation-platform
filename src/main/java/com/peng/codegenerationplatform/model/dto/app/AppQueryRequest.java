package com.peng.codegenerationplatform.model.dto.app;

import com.peng.codegenerationplatform.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class AppQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 代码生成类型（枚举值：html / multi_file）
     */
    private String codeGenType;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 部署标识
     */
    private String deployKey;

    private static final long serialVersionUID = 1L;
}