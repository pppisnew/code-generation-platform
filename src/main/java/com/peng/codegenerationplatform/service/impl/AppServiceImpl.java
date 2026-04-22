package com.peng.codegenerationplatform.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.peng.codegenerationplatform.constant.AppConstant;
import com.peng.codegenerationplatform.core.AiCodeGeneratorFacade;
import com.peng.codegenerationplatform.exception.BusinessException;
import com.peng.codegenerationplatform.exception.ErrorCode;
import com.peng.codegenerationplatform.exception.ThrowUtils;
import com.peng.codegenerationplatform.mapper.AppMapper;
import com.peng.codegenerationplatform.model.dto.app.AppAddRequest;
import com.peng.codegenerationplatform.model.dto.app.AppEditRequest;
import com.peng.codegenerationplatform.model.dto.app.AppQueryRequest;
import com.peng.codegenerationplatform.model.entity.App;
import com.peng.codegenerationplatform.model.entity.User;
import com.peng.codegenerationplatform.model.enums.CodeGenTypeEnum;
import com.peng.codegenerationplatform.model.vo.AppVO;
import com.peng.codegenerationplatform.model.vo.UserVO;
import com.peng.codegenerationplatform.service.AppService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author peng
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    private static final int DEPLOY_KEY_MAX_RETRY = 3;
    private static final int DEPLOY_KEY_LENGTH = 6;

    @Override
    public long addApp(AppAddRequest appAddRequest, long userId) {
        String initPrompt = appAddRequest.getInitPrompt();
        if (StrUtil.isBlank(initPrompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用初始化 prompt 不能为空");
        }
        // 自动生成应用名称：取提示词前12位
        String appName = initPrompt.length() > 12 ? initPrompt.substring(0, 12) : initPrompt;
        // 自动生成 deployKey：6位英文数字，确保唯一
        String deployKey = generateDeployKey();
        // 插入数据
        App app = new App();
        app.setAppName(appName);
        app.setInitPrompt(initPrompt);
        app.setCodeGenType(CodeGenTypeEnum.HTML.getValue());
        app.setDeployKey(deployKey);
        app.setUserId(userId);
        boolean result = this.save(app);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建失败，数据库错误");
        }
        return app.getId();
    }

    /**
     * 生成唯一的 deployKey，最多重试 3 次
     */
    private String generateDeployKey() {
        for (int i = 0; i < DEPLOY_KEY_MAX_RETRY; i++) {
            String key = RandomUtil.randomString(DEPLOY_KEY_LENGTH);
            QueryWrapper queryWrapper = QueryWrapper.create().eq("deployKey", key);
            long count = this.mapper.selectCountByQuery(queryWrapper);
            if (count == 0) {
                return key;
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成部署标识失败，请重试");
    }

    @Override
    public void editApp(AppEditRequest appEditRequest, long userId) {
        Long id = appEditRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        App oldApp = this.getById(id);
        if (oldApp == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!oldApp.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        App updateApp = new App();
        updateApp.setId(id);
        updateApp.setAppName(appEditRequest.getAppName());
        //用户编辑时间，区别于系统更新时间
        updateApp.setEditTime(LocalDateTime.now());
        boolean result = this.updateById(updateApp);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改失败");
        }
    }

    @Override
    public void deleteApp(long id, long userId) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        App oldApp = this.getById(id);
        if (oldApp == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!oldApp.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = this.removeById(id);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除失败");
        }
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        return appVO;
    }

    @Override
    public AppVO getAppVO(App app, UserVO userVO) {
        AppVO appVO = getAppVO(app);
        if (appVO != null) {
            appVO.setUserVO(userVO);
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        return appList.stream().map(this::getAppVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest, Long userId) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        String appName = appQueryRequest.getAppName();
        return QueryWrapper.create()
                .eq("userId", userId)
                .like("appName", appName)
                .orderBy("createTime", false);
    }

    @Override
    public QueryWrapper getAdminQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String codeGenType = appQueryRequest.getCodeGenType();
        Long userId = appQueryRequest.getUserId();
        Integer priority = appQueryRequest.getPriority();
        String deployKey = appQueryRequest.getDeployKey();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .eq("codeGenType", codeGenType)
                .eq("userId", userId)
                .eq("priority", priority)
                .eq("deployKey", deployKey)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public QueryWrapper getFeaturedQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        String appName = appQueryRequest.getAppName();
        return QueryWrapper.create()
                .ge("priority", AppConstant.GOOD_APP_PRIORITY)
                .like("appName", appName)
                .orderBy("priority", false)
                .orderBy("createTime", false);
    }

    @Override
    public Page<AppVO> getAppVOPage(Page<App> appPage) {
        Page<AppVO> appVOPage = new Page<>(appPage.getPageNumber(), appPage.getPageSize(), appPage.getTotalRow());
        List<AppVO> appVOList = getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return appVOPage;
    }

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限访问该应用，仅本人可以生成代码
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 获取应用的代码生成类型
        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }
        // 5. 调用 AI 生成代码
        return aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 没有则生成 6 位 deployKey（大小写字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
        // 7. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 8. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 9. 返回可访问的 URL
        return String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }

}