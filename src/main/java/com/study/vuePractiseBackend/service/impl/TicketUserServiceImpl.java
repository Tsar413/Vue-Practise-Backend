package com.study.vuePractiseBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.vuePractiseBackend.entity.TicketUser;
import com.study.vuePractiseBackend.mapper.TickerUserMapper;
import com.study.vuePractiseBackend.service.TicketUserService;
import org.springframework.stereotype.Service;

import static com.study.vuePractiseBackend.exception.BusinessExceptions.forbidden;
import static com.study.vuePractiseBackend.exception.BusinessExceptions.notFound;

@Service
public class TicketUserServiceImpl extends ServiceImpl<TickerUserMapper, TicketUser> implements TicketUserService {
    @Override
    public TicketUser switchUser(Long workspaceId, String userNo) {
        if (userNo == null || userNo.isBlank()) {
            throw new IllegalArgumentException("模拟用户编号不能为空");
        }
        userNo = userNo.trim();
        if (userNo.length() > 50) {
            throw new IllegalArgumentException("模拟用户编号不能超过50个字符");
        }
        LambdaQueryWrapper<TicketUser> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(TicketUser::getWorkspaceId, workspaceId)
                .eq(TicketUser::getUserNo, userNo);
        TicketUser user = baseMapper.selectOne(wrapper);
        if (user == null) {
            throw notFound("模拟用户不存在");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw forbidden("模拟用户已停用，无法切换");
        }
        if (!"USER".equals(user.getRole())
                && !"ADMIN".equals(user.getRole())) {
            throw forbidden("模拟用户角色异常");
        }
        return user;
    }
}
