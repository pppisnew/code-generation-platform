package com.peng.codegenerationplatform.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.peng.codegenerationplatform.model.dto.user.UserQueryRequest;
import com.peng.codegenerationplatform.model.entity.User;
import com.peng.codegenerationplatform.model.vo.LoginUserVO;
import com.peng.codegenerationplatform.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author peng
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏后用户信息
     * @param user
     * @return
     */
    public UserVO getUserVO(User user);

    /**
     * 获取脱敏后用户信息列表
     * @param userList
     * @return
     */
    public List<UserVO> getUserVOList(List<User> userList);

    /**
     * 查询请求转换算法
     * @param userQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 密码加密
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);
}
