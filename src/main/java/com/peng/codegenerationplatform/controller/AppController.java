package com.peng.codegenerationplatform.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.peng.codegenerationplatform.annotation.AuthCheck;
import com.peng.codegenerationplatform.common.BaseResponse;
import com.peng.codegenerationplatform.common.DeleteRequest;
import com.peng.codegenerationplatform.common.ResultUtils;
import com.peng.codegenerationplatform.constant.UserConstant;
import com.peng.codegenerationplatform.exception.BusinessException;
import com.peng.codegenerationplatform.exception.ErrorCode;
import com.peng.codegenerationplatform.exception.ThrowUtils;
import com.peng.codegenerationplatform.model.dto.app.*;
import com.peng.codegenerationplatform.model.entity.App;
import com.peng.codegenerationplatform.model.entity.User;
import com.peng.codegenerationplatform.model.vo.AppVO;
import com.peng.codegenerationplatform.model.vo.UserVO;
import com.peng.codegenerationplatform.service.AppService;
import com.peng.codegenerationplatform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 控制层。
 *
 * @author peng
 */
@Slf4j
@RestController
@RequestMapping("/app")
public class AppController {

    @Autowired
    private AppService appService;

    @Autowired
    private UserService userService;

    // ==================== 用户端接口 ====================

    /**
     * 用户创建应用（须填写 initPrompt）
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long id = appService.addApp(appAddRequest, loginUser.getId());
        return ResultUtils.success(id);
    }

    /**
     * 用户根据 id 修改自己的应用（目前仅支持修改应用名称）
     */
    @PostMapping("/my/edit")
    public BaseResponse<Boolean> editMyApp(@RequestBody AppEditRequest appEditRequest, HttpServletRequest request) {
        if (appEditRequest == null || appEditRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        appService.editApp(appEditRequest, loginUser.getId());
        return ResultUtils.success(true);
    }

    /**
     * 用户根据 id 删除自己的应用
     */
    @PostMapping("/my/delete")
    public BaseResponse<Boolean> deleteMyApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        appService.deleteApp(deleteRequest.getId(), loginUser.getId());
        return ResultUtils.success(true);
    }

    /**
     * 用户根据 id 查看应用详情（仅可查看自己的）
     */
    @GetMapping("/my/get/{id}")
    public BaseResponse<AppVO> getMyAppById(@PathVariable Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR);
        UserVO userVO = userService.getUserVO(loginUser);
        return ResultUtils.success(appService.getAppVO(app, userVO));
    }

    /**
     * 用户分页查询自己的应用列表（支持根据名称查询，每页最多 20 个）
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<AppVO>> listMyAppByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageSize = appQueryRequest.getPageSize();
        if (pageSize > 20) {
            appQueryRequest.setPageSize(20);
        }
        User loginUser = userService.getLoginUser(request);
        Page<App> appPage = appService.page(
                Page.of(appQueryRequest.getPageNum(), appQueryRequest.getPageSize()),
                appService.getQueryWrapper(appQueryRequest, loginUser.getId())
        );
        return ResultUtils.success(fillAppVOPage(appPage));
    }

    /**
     * 用户分页查询精选应用列表（支持根据名称查询，每页最多 20 个）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<AppVO>> listFeaturedAppByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageSize = appQueryRequest.getPageSize();
        if (pageSize > 20) {
            appQueryRequest.setPageSize(20);
        }
        Page<App> appPage = appService.page(
                Page.of(appQueryRequest.getPageNum(), appQueryRequest.getPageSize()),
                appService.getFeaturedQueryWrapper(appQueryRequest)
        );
        return ResultUtils.success(fillAppVOPage(appPage));
    }

    // ==================== 管理员端接口 ====================

    /**
     * 管理员根据 id 删除任意应用
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = appService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 管理员根据 id 更新任意应用（支持更新应用名称、封面、优先级）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        App app = new App();
        BeanUtil.copyProperties(appUpdateRequest, app);
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员分页查询应用列表（支持根据除时间外的任何字段查询，每页数量不限）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        Page<App> appPage = appService.page(
                Page.of(pageNum, pageSize),
                appService.getAdminQueryWrapper(appQueryRequest)
        );
        return ResultUtils.success(fillAppVOPage(appPage));
    }

    /**
     * 管理员根据 id 查看应用详情
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        User user = userService.getById(app.getUserId());
        UserVO userVO = user != null ? userService.getUserVO(user) : null;
        return ResultUtils.success(appService.getAppVO(app, userVO));
    }

    // ==================== 私有方法 ====================

    /**
     * 填充分页结果中的用户信息
     */
    private Page<AppVO> fillAppVOPage(Page<App> appPage) {
        List<App> records = appPage.getRecords();
        if (CollUtil.isEmpty(records)) {
            return appService.getAppVOPage(appPage);
        }
        Set<Long> userIds = records.stream()
                .map(App::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .map(user -> userService.getUserVO(user))
                .collect(Collectors.toMap(UserVO::getId, u -> u));
        Page<AppVO> appVOPage = new Page<>(appPage.getPageNumber(), appPage.getPageSize(), appPage.getTotalRow());
        List<AppVO> appVOList = records.stream()
                .map(app -> {
                    UserVO userVO = userVOMap.get(app.getUserId());
                    return appService.getAppVO(app, userVO);
                })
                .collect(Collectors.toList());
        appVOPage.setRecords(appVOList);
        return appVOPage;
    }

    /**
     * 应用聊天生成代码（流式 SSE）
     *
     * @param appId   应用 ID
     * @param message 用户消息
     * @param request 请求对象
     * @return 生成结果流
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务生成代码（流式）
        Flux<String> contentFlux = appService.chatToGenCode(appId, message, loginUser);
        // 转换为 ServerSentEvent 格式
        return contentFlux
                .map(chunk -> {
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ))
                .onErrorResume(e -> {
                    log.error("流式生成中断: {}", e.getMessage(), e);
                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event("error")
                                    .data("{\"code\":50000,\"msg\":\"服务端流式生成中断\"}")
                                    .build()
                    );
                });
    }

    /**
     * 应用部署
     *
     * @param appDeployRequest 部署请求
     * @param request          请求
     * @return 部署 URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务部署应用
        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }


}