package com.drivingschool.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson 序列化配置类。
 * <p>
 * 自定义 JSON 序列化行为。主要解决前端 JavaScript 处理 Long 类型数据
 * 精度丢失的问题：JavaScript 的 Number 类型最大安全整数为 2^53，
 * 超过该范围的 Long 值会丢失精度。通过将 Long 类型序列化为字符串，
 * 可以避免前端接收数据时的精度问题。
 * </p>
 * <p>
 * 影响范围：Long 和 long 类型的字段在 JSON 响应中都会被序列化为字符串。
 * 例如：数据库 ID 1234567890123456789 会以 "1234567890123456789" 形式返回。
 * </p>
 */
@Configuration
public class JacksonConfig {

    /**
     * 自定义 ObjectMapper，注册 Long 类型的字符串序列化器。
     * <p>
     * 将 Long.class（包装类型）和 Long.TYPE（基本类型 long）都序列化为字符串，
     * 防止前端 JavaScript 处理大整数时出现精度丢失。
     * </p>
     *
     * @param builder Jackson2 对象映射构建器，由 Spring 自动注入
     * @return 自定义配置后的 ObjectMapper 实例
     */
    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.build();
        SimpleModule module = new SimpleModule();
        // 将 Long 包装类型序列化为字符串
        module.addSerializer(Long.class, ToStringSerializer.instance);
        // 将 long 基本类型序列化为字符串
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        mapper.registerModule(module);
        return mapper;
    }
}
