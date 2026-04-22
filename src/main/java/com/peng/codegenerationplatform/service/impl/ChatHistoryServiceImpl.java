package com.peng.codegenerationplatform.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.peng.codegenerationplatform.mapper.ChatHistoryMapper;
import com.peng.codegenerationplatform.model.entity.ChatHistory;
import com.peng.codegenerationplatform.service.ChatHistoryService;
import org.springframework.stereotype.Service;

/**
 * 对话历史 服务层实现。
 *
 * @author peng
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

}
