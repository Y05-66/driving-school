package com.drivingschool;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 驾校管理系统启动类。
 * <p>
 * 这是整个 Spring Boot 应用的入口，负责启动内嵌的 Web 服务器
 * （默认 Tomcat），加载所有配置和组件，初始化应用上下文。
 * </p>
 * <p>
 * 注解说明：
 * - @SpringBootApplication：组合注解，包含 @Configuration、@EnableAutoConfiguration、@ComponentScan
 * - @MapperScan：自动扫描指定包下的 MyBatis Mapper 接口，注册为 Spring Bean
 * </p>
 */
@SpringBootApplication
@MapperScan("com.drivingschool.*.mapper")  // 扫描所有模块下的 Mapper 接口
public class DrivingSchoolApplication {

    /**
     * 应用主入口方法。
     * <p>
     * 启动 Spring Boot 应用，初始化所有组件并启动内嵌 Web 服务器。
     * </p>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DrivingSchoolApplication.class, args);
    }
}
