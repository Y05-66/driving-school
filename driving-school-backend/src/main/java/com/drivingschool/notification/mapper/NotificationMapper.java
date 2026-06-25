package com.drivingschool.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 通知消息Mapper接口
 * 对应实体类：{@link Notification}（notification表）
 * 提供通知消息的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 * 同时包含未读通知统计相关的自定义查询方法
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 统计指定用户的未读通知数量
     * 查询满足以下条件之一的未读通知总数：
     * 1. 直接指定给该用户的通知（target_user_id = userId）
     * 2. 广播给所有用户的通知（target_role = 'ALL'）
     * 3. 广播给该用户所属角色的通知（target_role = 用户角色）
     *
     * @param userId 用户ID
     * @param role   用户角色（如：STUDENT、COACH、ADMIN等）
     * @return 该用户的未读通知数量
     */
    @Select("SELECT COUNT(*) FROM notification WHERE is_read = 0 AND deleted = 0 " +
            "AND (target_user_id = #{userId} OR target_role = 'ALL' OR target_role = #{role})")
    long countUnread(@Param("userId") Long userId, @Param("role") String role);
}
