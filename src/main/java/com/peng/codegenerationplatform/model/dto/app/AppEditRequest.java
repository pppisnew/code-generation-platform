package com.peng.codegenerationplatform.model.dto.app;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppEditRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    private static final long serialVersionUID = 1L;
}