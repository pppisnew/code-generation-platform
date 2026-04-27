package com.peng.codegenerationplatform.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.peng.codegenerationplatform.common.CursorPageResponse;
import com.peng.codegenerationplatform.mapper.ChatHistoryMapper;
import com.peng.codegenerationplatform.model.dto.chathistory.ChatHistoryQueryRequest;
import com.peng.codegenerationplatform.model.entity.ChatHistory;
import com.peng.codegenerationplatform.model.enums.MessageTypeEnum;
import com.peng.codegenerationplatform.model.vo.ChatHistoryVO;
import com.peng.codegenerationplatform.model.vo.UserVO;
import com.peng.codegenerationplatform.service.ChatHistoryService;
import com.peng.codegenerationplatform.service.UserService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 对话历史 服务层实现。
 *
 * @author peng
 */
@Slf4j
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Autowired
    private UserService userService;

    @Override
    public CursorPageResponse<ChatHistoryVO> getChatHistoryByAppId(Long appId, LocalDateTime cursor, int pageSize) {
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("appId", appId)
                .lt("createTime", cursor)
                .orderBy("createTime", false)
                .limit(pageSize + 1);
        return doCursorQuery(wrapper, pageSize);
    }

    @Override
    public CursorPageResponse<ChatHistoryVO> getChatHistoryPage(ChatHistoryQueryRequest request) {
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("appId", request.getAppId())
                .eq("userId", request.getUserId())
                .eq("messageType", request.getMessageType())
                .lt("createTime", request.getCursor())
                .orderBy("createTime", false)
                .limit(request.getPageSize() + 1);
        return doCursorQuery(wrapper, request.getPageSize());
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        return this.mapper.deleteByQuery(QueryWrapper.create().eq("appId", appId)) >= 0;
    }

    @Override
    public ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory, UserVO userVO) {
        if (chatHistory == null) {
            return null;
        }
        ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
        BeanUtil.copyProperties(chatHistory, chatHistoryVO);
        chatHistoryVO.setUserVO(userVO);
        return chatHistoryVO;
    }

    @Override
    public List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> list, Map<Long, UserVO> userVOMap) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        return list.stream()
                .map(chatHistory -> {
                    UserVO userVO = userVOMap.get(chatHistory.getUserId());
                    return getChatHistoryVO(chatHistory, userVO);
                })
                .collect(Collectors.toList());
    }

    /**
     * 游标分页通用查询
     *
     * @param wrapper  查询条件（已包含 limit(pageSize+1)）
     * @param pageSize 每页大小
     * @return 游标分页响应
     */
    private CursorPageResponse<ChatHistoryVO> doCursorQuery(QueryWrapper wrapper, int pageSize) {
        List<ChatHistory> list = this.mapper.selectListByQuery(wrapper);
        boolean hasMore = list.size() > pageSize;
        if (hasMore) {
            list = list.subList(0, pageSize);
        }
        LocalDateTime nextCursor = list.isEmpty() ? null : list.get(list.size() - 1).getCreateTime();
        // 批量填充用户信息
        Map<Long, UserVO> userVOMap = fillUserVOMap(list);
        // 转换 VO
        List<ChatHistoryVO> voList = getChatHistoryVOList(list, userVOMap);
        // 封装响应
        CursorPageResponse<ChatHistoryVO> response = new CursorPageResponse<>();
        response.setRecords(voList);
        response.setHasMore(hasMore);
        response.setNextCursor(nextCursor);
        return response;
    }

    /**
     * 批量查询并映射 userId -> UserVO
     */
    private Map<Long, UserVO> fillUserVOMap(List<ChatHistory> list) {
        Set<Long> userIds = list.stream()
                .map(ChatHistory::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(userIds)) {
            return Map.of();
        }
        return userService.listByIds(userIds).stream()
                .map(user -> userService.getUserVO(user))
                .collect(Collectors.toMap(UserVO::getId, u -> u));
    }

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 直接构造查询条件，起始点为 1 而不是 0，用于排除最新的用户消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 反转列表，确保按时间正序（老的在前，新的在后）
            historyList = historyList.reversed();
            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if (MessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                } else if (MessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }

}
