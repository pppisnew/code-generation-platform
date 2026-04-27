package com.peng.codegenerationplatform.service;

import com.mybatisflex.core.service.IService;
import com.peng.codegenerationplatform.common.CursorPageResponse;
import com.peng.codegenerationplatform.model.dto.chathistory.ChatHistoryQueryRequest;
import com.peng.codegenerationplatform.model.entity.ChatHistory;
import com.peng.codegenerationplatform.model.vo.ChatHistoryVO;
import com.peng.codegenerationplatform.model.vo.UserVO;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 对话历史 服务层。
 *
 * @author peng
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 根据应用 id 游标分页查询对话历史
     *
     * @param appId    应用 id
     * @param cursor   游标（上一页最后一条的 createTime），首次查询传 null
     * @param pageSize 每页大小
     * @return 游标分页响应
     */
    CursorPageResponse<ChatHistoryVO> getChatHistoryByAppId(Long appId, LocalDateTime cursor, int pageSize);

    /**
     * 管理员游标分页查询对话历史
     *
     * @param request 查询请求
     * @return 游标分页响应
     */
    CursorPageResponse<ChatHistoryVO> getChatHistoryPage(ChatHistoryQueryRequest request);

    /**
     * 根据应用 id 删除所有对话历史
     *
     * @param appId 应用 id
     * @return {@code true} 删除成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 获取脱敏的对话历史视图
     *
     * @param chatHistory 对话历史
     * @param userVO      用户视图
     * @return ChatHistoryVO
     */
    ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory, UserVO userVO);

    /**
     * 批量获取脱敏的对话历史视图
     *
     * @param list     对话历史列表
     * @param userVOMap 用户 id -> UserVO 映射
     * @return ChatHistoryVO 列表
     */
    List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> list, Map<Long, UserVO> userVOMap);

    /**
     * 加载对话历史到内存
     *
     * @param appId       应用 id
     * @param chatMemory  聊天内存
     * @param maxCount    最大数量
     * @return 加载数量
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
