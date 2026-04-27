package com.peng.codegenerationplatform.controller;

import com.peng.codegenerationplatform.annotation.AuthCheck;
import com.peng.codegenerationplatform.common.BaseResponse;
import com.peng.codegenerationplatform.common.CursorPageResponse;
import com.peng.codegenerationplatform.common.ResultUtils;
import com.peng.codegenerationplatform.constant.UserConstant;
import com.peng.codegenerationplatform.exception.BusinessException;
import com.peng.codegenerationplatform.exception.ErrorCode;
import com.peng.codegenerationplatform.exception.ThrowUtils;
import com.peng.codegenerationplatform.model.dto.chathistory.ChatHistoryQueryRequest;
import com.peng.codegenerationplatform.model.entity.App;
import com.peng.codegenerationplatform.model.entity.User;
import com.peng.codegenerationplatform.model.enums.UserRoleEnum;
import com.peng.codegenerationplatform.model.vo.ChatHistoryVO;
import com.peng.codegenerationplatform.service.AppService;
import com.peng.codegenerationplatform.service.ChatHistoryService;
import com.peng.codegenerationplatform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对话历史 控制层。
 *
 * @author peng
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private AppService appService;

    /**
     * 根据应用 id 分页获取对话历史（仅应用创建者和管理员可见）
     *
     * @param request   查询请求
     * @param httpRequest 请求
     * @return 游标分页响应
     */
    @PostMapping("/page/vo")
    public BaseResponse<CursorPageResponse<ChatHistoryVO>> listChatHistoryByAppId(
            @RequestBody ChatHistoryQueryRequest request, HttpServletRequest httpRequest) {
        Long appId = request.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 无效");
        User loginUser = userService.getLoginUser(httpRequest);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 仅应用创建者和管理员可查看
        if (!app.getUserId().equals(loginUser.getId())
                && !UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该应用对话历史");
        }
        int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;
        CursorPageResponse<ChatHistoryVO> result =
                chatHistoryService.getChatHistoryByAppId(appId, request.getCursor(), pageSize);
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页查询所有对话历史
     *
     * @param request 查询请求
     * @return 游标分页响应
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<CursorPageResponse<ChatHistoryVO>> listChatHistoryByPage(
            @RequestBody ChatHistoryQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        CursorPageResponse<ChatHistoryVO> result = chatHistoryService.getChatHistoryPage(request);
        return ResultUtils.success(result);
    }
}
