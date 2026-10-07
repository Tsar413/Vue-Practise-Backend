package com.study.vuePractiseBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.vuePractiseBackend.dto.SysLoginDTO;
import com.study.vuePractiseBackend.dto.SysLoginReturnDTO;
import com.study.vuePractiseBackend.entity.SysClass;
import com.study.vuePractiseBackend.entity.SysLoginToken;
import com.study.vuePractiseBackend.entity.SysUser;
import com.study.vuePractiseBackend.mapper.SysClassMapper;
import com.study.vuePractiseBackend.mapper.SysLoginMapper;
import com.study.vuePractiseBackend.mapper.SysUserMapper;
import com.study.vuePractiseBackend.service.SysLoginService;
import com.study.vuePractiseBackend.util.PasswordUtil;
import com.study.vuePractiseBackend.util.TokenUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Service
public class SysLoginServiceImpl extends ServiceImpl<SysLoginMapper, SysLoginToken> implements SysLoginService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysClassMapper sysClassMapper;

    @Override
    @Transactional
    public SysLoginReturnDTO login(SysLoginDTO sysLoginDTO) {
        // 1. 检查登录参数
        if (sysLoginDTO == null || sysLoginDTO.getId() == null || sysLoginDTO.getPassword() == null) {
            return failure(-2);
        }
        String id = sysLoginDTO.getId().trim();
        // 密码保留原始内容，不去除首尾空格
        String password = sysLoginDTO.getPassword();
        if (id.isBlank() || password.isBlank()) {
            return failure(-2);
        }
        if (id.length() > 50 || password.length() > 50) {
            return failure(-3);
        }
        // 2. 查询用户并校验密码
        SysUser user = sysUserMapper.selectById(id);
        // 账号不存在和密码错误统一处理
        if (user == null || !matchesPassword(password, user)) {
            return failure(-4);
        }
        // 3. 检查账号状态
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            return failure(-5);
        }
        // 4. 学生检查班级，教师不检查班级
        if ("STUDENT".equals(user.getRole())) {
            String classId = user.getClassId();
            if (classId == null || classId.isBlank()) {
                return failure(-6);
            }
            SysClass sysClass = sysClassMapper.selectById(classId);
            if (sysClass == null || !Integer.valueOf(1).equals(sysClass.getStatus())) {
                return failure(-6);
            }
        } else if (!"TEACHER".equals(user.getRole())) {
            return failure(-7);
        }
        // 5. 查询已有登录记录
        QueryWrapper<SysLoginToken> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", id);
        SysLoginToken loginToken = baseMapper.selectOne(wrapper);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusDays(1);
        // 原始Token返回前端，数据库只保存哈希
        String rawToken = TokenUtil.generateToken();
        String tokenHash = TokenUtil.hashToken(rawToken);
        // 6. 保留已有API访问码，仅学生缺失访问码时生成
        String apiAccessCode = loginToken == null ? null : loginToken.getApiAccessCode();
        if ("STUDENT".equals(user.getRole()) && (apiAccessCode == null || apiAccessCode.isBlank())) {
            apiAccessCode = TokenUtil.generateToken();
        }
        // 7. 创建或更新登录记录
        if (loginToken == null) {
            createLoginToken(id, tokenHash, apiAccessCode, now, expireTime);
        } else {
            updateLoginToken(loginToken.getId(), tokenHash, apiAccessCode, now, expireTime);
        }

        // 8. 更新最后登录时间
        UpdateWrapper<SysUser> userUpdate = new UpdateWrapper<>();
        userUpdate.eq("id", id);
        userUpdate.set("last_login_time", now);
        if (sysUserMapper.update(null, userUpdate) != 1) {
            throw new IllegalStateException("更新最后登录时间失败");
        }
        // 9. 返回登录结果，不返回过期时间
        SysLoginReturnDTO result = new SysLoginReturnDTO();
        result.setStatus(1);
        result.setUserId(id);
        result.setToken(rawToken);
        result.setRealName(user.getRealName());
        result.setRole(user.getRole());
        if ("STUDENT".equals(user.getRole())) {
            result.setApiAccessCode(apiAccessCode);
        }
        return result;
    }

    @Override
    @Transactional
    public Integer logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return -2;
        }
        rawToken = rawToken.trim();
        if (!rawToken.matches("[0-9a-f]{64}")) {
            return -2;
        }
        UpdateWrapper<SysLoginToken> wrapper = new UpdateWrapper<>();
        wrapper.eq("token_hash", TokenUtil.hashToken(rawToken));
        wrapper.isNull("revoke_time");
        wrapper.set("revoke_time", LocalDateTime.now());
        baseMapper.update(null, wrapper);
        return 1;
    }

    @Override
    @Transactional
    public void revokeByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("用户编号不能为空");
        }
        UpdateWrapper<SysLoginToken> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId.trim());
        wrapper.isNull("revoke_time");
        wrapper.set("revoke_time", LocalDateTime.now());
        // 用户可能从未登录，更新0条记录是正常情况。
        baseMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public void deleteByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("用户编号不能为空");
        }
        QueryWrapper<SysLoginToken> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId.trim());
        baseMapper.delete(wrapper);
    }

    /**
     * 使用用户保存的盐校验密码。
     */
    private boolean matchesPassword(String password, SysUser user) {
        if (user.getPasswordSalt() == null || user.getPasswordHash() == null) {
            return false;
        }
        if (!"SHA-256".equals(user.getPasswordAlgorithm())) {
            return false;
        }
        String calculatedHash = PasswordUtil.hashPassword(password, user.getPasswordSalt());
        return MessageDigest.isEqual(
                calculatedHash.getBytes(StandardCharsets.UTF_8),
                user.getPasswordHash().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 首次登录创建记录。
     */
    private void createLoginToken(String userId, String tokenHash, String apiAccessCode, LocalDateTime now, LocalDateTime expireTime) {
        SysLoginToken token = new SysLoginToken();
        token.setUserId(userId);
        token.setTokenHash(tokenHash);
        token.setApiAccessCode(apiAccessCode);
        token.setCreateTime(now);
        token.setExpireTime(expireTime);
        token.setRevokeTime(null);
        if (baseMapper.insert(token) != 1) {
            throw new IllegalStateException("创建登录Token失败");
        }
    }

    /**
     * 再次登录更换网页登录Token，保留API访问码。
     */
    private void updateLoginToken(Long tokenId, String tokenHash, String apiAccessCode, LocalDateTime now, LocalDateTime expireTime) {
        UpdateWrapper<SysLoginToken> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", tokenId);
        wrapper.set("token_hash", tokenHash);
        wrapper.set("api_access_code", apiAccessCode);
        wrapper.set("create_time", now);
        wrapper.set("expire_time", expireTime);
        // 显式清空此前的撤销时间
        wrapper.set("revoke_time", null);
        if (baseMapper.update(null, wrapper) != 1) {
            throw new IllegalStateException("更新登录Token失败");
        }
    }

    /**
     * 构造登录失败结果。
     */
    private SysLoginReturnDTO failure(int status) {
        SysLoginReturnDTO result = new SysLoginReturnDTO();
        result.setStatus(status);
        return result;
    }
}