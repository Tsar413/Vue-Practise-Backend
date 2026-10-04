package com.study.vuePractiseBackend.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.study.vuePractiseBackend.entity.SysClass;
import com.study.vuePractiseBackend.entity.SysLoginToken;
import com.study.vuePractiseBackend.entity.SysUser;
import com.study.vuePractiseBackend.entity.SysWorkspace;
import com.study.vuePractiseBackend.mapper.SysClassMapper;
import com.study.vuePractiseBackend.mapper.SysLoginMapper;
import com.study.vuePractiseBackend.mapper.SysUserMapper;
import com.study.vuePractiseBackend.mapper.SysWorkspaceMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.Map;

@Component
public class ApiAccessInterceptor implements HandlerInterceptor {

    public static final String WORKSPACE_ID = "practiceWorkspaceId";
    public static final String STUDENT_ID = "practiceStudentId";

    @Resource
    private SysLoginMapper sysLoginMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysClassMapper sysClassMapper;

    @Resource
    private SysWorkspaceMapper sysWorkspaceMapper;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        // 跨域预检交给CORS配置处理。
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }

        // 从Spring匹配的路径变量中读取访问码。
        Object variablesValue = request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        if (!(variablesValue instanceof Map<?, ?> variables)) {
            return reject(response, 401, "缺少API访问码");
        }

        Object codeValue = variables.get("accessCode");
        if (!(codeValue instanceof String accessCode)
                || !accessCode.matches("[0-9a-f]{64}")) {
            return reject(response, 401, "API访问码格式错误");
        }

        // 访问码在数据库中直接保存，按原值查询。
        SysLoginToken loginToken = sysLoginMapper.selectOne(
                new QueryWrapper<SysLoginToken>()
                        .eq("api_access_code", accessCode));

        if (loginToken == null) {
            return reject(response, 401, "API访问码无效");
        }

        /*
         * 不检查expireTime、revokeTime。
         * 这两个字段控制网页登录Token，
         * 网页退出或登录过期不影响长期API访问码。
         */

        SysUser user = sysUserMapper.selectById(loginToken.getUserId());

        if (user == null) {
            return reject(response, 403, "访问码所属账号不存在");
        }

        if (!"STUDENT".equals(user.getRole())) {
            return reject(response, 403, "访问码所属账号不是学生");
        }

        if (!Integer.valueOf(1).equals(user.getStatus())) {
            return reject(response, 403, "学生账号已停用");
        }

        String classId = user.getClassId();
        if (classId == null || classId.isBlank()) {
            return reject(response, 403, "学生未关联有效班级");
        }

        SysClass sysClass = sysClassMapper.selectById(classId);
        if (sysClass == null
                || !Integer.valueOf(1).equals(sysClass.getStatus())) {
            return reject(response, 403, "所属班级不存在或已停用");
        }

        SysWorkspace workspace = sysWorkspaceMapper.selectOne(
                new QueryWrapper<SysWorkspace>()
                        .eq("student_id", user.getId()));

        if (workspace == null) {
            return reject(response, 403, "工作空间不存在");
        }

        if (!Integer.valueOf(1).equals(workspace.getStatus())) {
            return reject(response, 403, "工作空间已暂停");
        }

        // 只在当前请求中保存，不使用全局变量或ThreadLocal。
        request.setAttribute(WORKSPACE_ID, workspace.getId());
        request.setAttribute(STUDENT_ID, user.getId());

        return true;
    }

    /**
     * message仅使用本类固定提示。
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
                        + "\",\"data\":null}");

        return false;
    }
}