package com.drivingschool.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置类。
 * <p>
 * 配置 MyBatis-Plus 的分页插件和自动填充功能：
 * - 分页插件：支持 MySQL 数据库的物理分页查询
 * - 自动填充：在插入和更新记录时自动填充 createTime 和 updateTime 字段
 * </p>
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 配置 MyBatis-Plus 分页拦截器。
     * <p>
     * 注册分页插件后，可以使用 MyBatis-Plus 提供的 IPage 进行分页查询，
     * 插件会自动将逻辑分页转换为 MySQL 的 LIMIT 物理分页语句。
     * </p>
     *
     * @return MybatisPlusInterceptor 包含分页插件的拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加 MySQL 分页拦截器，指定数据库类型为 MySQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 配置元数据对象自动填充处理器。
     * <p>
     * 实现插入时自动填充 createTime 和 updateTime，
     * 更新时自动填充 updateTime，无需在业务代码中手动赋值。
     * 实体类字段需添加 @TableField(fill = FieldFill.INSERT) 等注解才能生效。
     * </p>
     *
     * @return MetaObjectHandler 自动填充处理器实例
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            /**
             * 插入记录时自动填充 createTime 和 updateTime。
             *
             * @param metaObject 元数据对象，包含实体类的字段信息
             */
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }

            /**
             * 更新记录时自动填充 updateTime。
             *
             * @param metaObject 元数据对象，包含实体类的字段信息
             */
            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
