package com.peng.codegenerationplatform.model.dto.chathistory;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatHistoryQueryRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 消息类型：user/ai/error
     */
    private String messageType;

    /**
     * 游标（上一页最后一条的 createTime），首次查询传 null
     */
    private LocalDateTime cursor;

    /**
     * 每页大小，默认 10
     */
    private int pageSize = 10;

    private static final long serialVersionUID = 1L;
}
