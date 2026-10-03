package com.study.vuePractiseBackend.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.study.vuePractiseBackend.entity.SysClass;
import com.study.vuePractiseBackend.entity.SysLoginToken;
import com.study.vuePractiseBackend.entity.SysUser;
import com.study.vuePractiseBackend.mapper.SysClassMapper;
import com.study.vuePractiseBackend.mapper.SysLoginMapper;
import com.study.vuePractiseBackend.mapper.SysUserMapper;
import com.study.vuePractiseBackend.util.TokenUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class SysLoginInterceptor implements HandlerInterceptor {

    public static final String LOGIN_USER_ID = "loginUserId";
    public static final String LOGIN_USER_ROLE = "loginUserRole";

    @Resource
    private SysLoginMapper sysLoginMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysClassMapper sysClassMapper;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        // 跨域预检由CORS配置判断，不在这里要求登录。
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }

        // 1. 获取原始Token
        String authorization = request.getHeader("Authorization");

        if (authorization == null
                || !authorization.regionMatches(
                true, 0, "Bearer ", 0, 7)) {
            return reject(response, 401, "请先登录");
        }

        String rawToken = authorization.substring(7).trim();

        // 对应TokenUtil生成的64位小写十六进制字符串
        if (!rawToken.matches("[0-9a-f]{64}")) {
            return reject(response, 401, "登录凭证无效，请重新登录");
        }

        // 2. 使用哈希查询登录记录
        QueryWrapper<SysLoginToken> wrapper = new QueryWrapper<>();
        wrapper.eq("token_hash", TokenUtil.hashToken(rawToken));

        SysLoginToken loginToken = sysLoginMapper.selectOne(wrapper);

        if (loginToken == null) {
            return reject(response, 401, "登录已失效，请重新登录");
        }

        // 3. 检查是否已退出或被撤销
        if (loginToken.getRevokeTime() != null) {
            return reject(response, 401, "登录已撤销，请重新登录");
        }

        // 4. 检查过期时间，全部以后端时间为准
        LocalDateTime now = LocalDateTime.now();

        if (loginToken.getExpireTime() == null
                || !loginToken.getExpireTime().isAfter(now)) {
            return reject(response, 401, "登录已过期，请重新登录");
        }

        // 5. 检查用户
        SysUser user = sysUserMapper.selectById(loginToken.getUserId());

        if (user == null) {
            return reject(response, 401, "账号不存在，请重新登录");
        }

        if (!Integer.valueOf(1).equals(user.getStatus())) {
            return reject(response, 403, "账号已停用");
        }

        // 6. 检查角色和学生班级
        if ("STUDENT".equals(user.getRole())) {
            String classId = user.getClassId();

            if (classId == null || classId.isBlank()) {
                return reject(response, 403, "账号未关联有效班级");
            }

            SysClass sysClass = sysClassMapper.selectById(classId);

            if (sysClass == null
                    || !Integer.valueOf(1).equals(sysClass.getStatus())) {
                return reject(response, 403, "所属班级不存在或已停用");
            }

        } else if (!"TEACHER".equals(user.getRole())) {
            return reject(response, 403, "账号角色异常");
        }

        // 7. 校验接口权限和数据归属
        if (!hasPermission(request, user)) {
            return reject(response, 403, "无权执行此操作");
        }

        // 8. 保存本次请求的登录身份
        request.setAttribute(LOGIN_USER_ID, user.getId());
        request.setAttribute(LOGIN_USER_ROLE, user.getRole());

        return true;
    }

    private boolean hasPermission(
            HttpServletRequest request, SysUser user) {

        if ("TEACHER".equals(user.getRole())) {
            return true;
        }

        // 使用Spring实际匹配的接口模板
        Object patternValue = request.getAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE
        );

        if (patternValue == null) {
            return false;
        }

        String pattern = patternValue.toString();
        String method = request.getMethod();

        boolean allowedEndpoint =
                ("GET".equals(method)
                        && ("/api/sys-user/one/{id}".equals(pattern)
                        || "/api/sys-workspace/one/{id}".equals(pattern)))
                        || ("POST".equals(method)
                        && "/api/sys-workspace/one/{id}/reset".equals(pattern));

        if (!allowedEndpoint) {
            return false;
        }

        Object variablesValue = request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
        );

        if (!(variablesValue instanceof Map<?, ?> variables)) {
            return false;
        }

        // 学生只能操作自己的学号
        Object requestedId = variables.get("id");
        return user.getId().equals(requestedId);
    }

    /**
     * 返回与Result一致的JSON。
     * message仅传入本类固定提示，不拼接外部输入。
     */
    private boolean reject(
            HttpServletResponse response,
            int status,
            String message) throws IOException {

        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");

        response.getWriter().write(
                "{\"code\":" + status
                        + ",\"message\":\"" + message
                        + "\",\"data\":null}"
        );

        return false;
    }
}