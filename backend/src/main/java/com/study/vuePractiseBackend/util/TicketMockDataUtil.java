package com.study.vuePractiseBackend.util;

import com.study.vuePractiseBackend.entity.TicketActivity;
import com.study.vuePractiseBackend.entity.TicketRecord;
import com.study.vuePractiseBackend.entity.TicketUser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class TicketMockDataUtil {

    private TicketMockDataUtil() {
    }

    /**
     * 模拟业务用户，不是系统中的学生、教师账号。
     */
    public static List<TicketUser> createUsers(Long workspaceId) {
        List<TicketUser> list = new ArrayList<>();

        list.add(createUser(workspaceId,
                "N0001", "张明", "13800000001",
                "电子信息工程系", "新吴校区", "USER", 1));

        list.add(createUser(workspaceId,
                "N0002", "李华", "13800000002",
                "电子信息工程系", "新吴校区", "USER", 1));

        list.add(createUser(workspaceId,
                "N0003", "王芳", "13800000003",
                "机电工程系", "新吴校区", "USER", 1));

        list.add(createUser(workspaceId,
                "N0004", "陈晨", "13800000004",
                "汽车工程学院", "藕塘校区", "USER", 0));

        list.add(createUser(workspaceId,
                "N0005", "赵敏", "13800000005",
                "城轨工程系", "藕塘校区", "ADMIN", 1));

        return list;
    }

    /**
     * 活动时间相对于生成时间设置，避免固定日期过期。
     * 所有活动初始报名人数为0。
     */
    public static List<TicketActivity> createActivities(
            Long workspaceId, LocalDateTime now) {

        List<TicketActivity> list = new ArrayList<>();

        // 已发布，正在报名。
        list.add(createActivity(
                workspaceId,
                "教职工电影观影活动",
                "工会组织教职工集体观影，每人限领取一张票。",
                "新吴校区",
                "荟聚金逸影城",
                1, 30,
                now.minusDays(1),
                now.plusDays(2),
                now.plusDays(3),
                now.plusDays(3).plusHours(2)
        ));

        // 已发布，尚未开始报名。
        list.add(createActivity(
                workspaceId,
                "周末城市文化参观",
                "参观城市博物馆，集合后统一乘车前往。",
                "新吴校区",
                "新吴校区图书馆门口集合",
                1, 20,
                now.plusDays(1),
                now.plusDays(3),
                now.plusDays(4),
                now.plusDays(4).plusHours(4)
        ));

        // 草稿，尚未发布。
        list.add(createActivity(
                workspaceId,
                "羽毛球活动",
                "提供场地和基础器材，欢迎师生参加。",
                "新吴校区",
                "新吴校区体艺馆一楼",
                0, 16,
                now.plusDays(2),
                now.plusDays(4),
                now.plusDays(5),
                now.plusDays(5).plusHours(2)
        ));

        // 已关闭，活动已结束。
        list.add(createActivity(
                workspaceId,
                "工会读书交流会",
                "分享阅读心得，交流教学与生活经验。",
                "新吴校区",
                "新吴校区行政楼二楼报告厅",
                2, 25,
                now.minusDays(7),
                now.minusDays(5),
                now.minusDays(4),
                now.minusDays(4).plusHours(2)
        ));

        return list;
    }

    /**
     * 初始抢票记录为空，后续通过业务操作生成。
     */
    public static List<TicketRecord> createRecords() {
        return new ArrayList<>();
    }

    private static TicketUser createUser(Long workspaceId, String userNo, String realName,
            String phone, String department, String campus, String role, Integer status) {

        TicketUser user = new TicketUser();
        user.setWorkspaceId(workspaceId);
        user.setUserNo(userNo);
        user.setRealName(realName);
        user.setPhone(phone);
        user.setDepartment(department);
        user.setCampus(campus);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    private static TicketActivity createActivity(Long workspaceId, String activityName, String description,
            String campus, String location, Integer status, Integer quota, LocalDateTime bookingStartTime,
            LocalDateTime bookingEndTime, LocalDateTime activityStartTime, LocalDateTime activityEndTime) {

        TicketActivity activity = new TicketActivity();
        activity.setWorkspaceId(workspaceId);
        activity.setActivityName(activityName);
        activity.setDescription(description);
        activity.setCoverUrl(null);
        activity.setCampus(campus);
        activity.setLocation(location);
        activity.setStatus(status);
        activity.setQuota(quota);
        activity.setBookedCount(0);
        activity.setBookingStartTime(bookingStartTime);
        activity.setBookingEndTime(bookingEndTime);
        activity.setActivityStartTime(activityStartTime);
        activity.setActivityEndTime(activityEndTime);
        return activity;
    }
}