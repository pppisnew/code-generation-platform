package com.peng.codegenerationplatform.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.peng.codegenerationplatform.model.dto.app.AppAddRequest;
import com.peng.codegenerationplatform.model.dto.app.AppEditRequest;
import com.peng.codegenerationplatform.model.dto.app.AppQueryRequest;
import com.peng.codegenerationplatform.model.entity.App;
import com.peng.codegenerationplatform.model.entity.User;
import com.peng.codegenerationplatform.model.vo.AppVO;
import com.peng.codegenerationplatform.model.vo.UserVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author peng
 */
public interface AppService extends IService<App> {

    /**
     * 创建应用
     *
     * @param appAddRequest 创建请求
     * @param userId        当前登录用户id
     * @return 新应用 id
     */
    long addApp(AppAddRequest appAddRequest, long userId);

    /**
     * 用户修改自己的应用（目前仅支持修改名称）
     *
     * @param appEditRequest 修改请求
     * @param userId         当前登录用户id
     */
    void editApp(AppEditRequest appEditRequest, long userId);

    /**
     * 用户删除自己的应用
     *
     * @param id     应用id
     * @param userId 当前登录用户id
     */
    void deleteApp(long id, long userId);

    /**
     * 获取应用脱敏视图
     *
     * @param app 应用
     * @return AppVO
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用脱敏视图（带创建用户信息）
     *
     * @param app    应用
     * @param userVO 创建用户脱敏信息
     * @return AppVO
     */
    AppVO getAppVO(App app, UserVO userVO);

    /**
     * 批量获取应用脱敏视图
     *
     * @param appList 应用列表
     * @return AppVO 列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 获取查询条件（用户分页查询）
     *
     * @param appQueryRequest 查询请求
     * @param userId          当前登录用户id（仅查自己的）
     * @return QueryWrapper
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest, Long userId);

    /**
     * 获取查询条件（管理员分页查询）
     *
     * @param appQueryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper getAdminQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取精选应用查询条件
     *
     * @param appQueryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper getFeaturedQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 分页获取应用 VO 列表
     *
     * @param appPage 应用分页
     * @return AppVO 分页
     */
    Page<AppVO> getAppVOPage(Page<App> appPage);

    /**
     * 业务接口
     *
     * @param appId
     * @param message
     * @param loginUser
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);
}