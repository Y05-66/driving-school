package com.drivingschool.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类。
 * <p>
 * 实现 WebMvcConfigurer 接口，自定义 Spring MVC 的行为。
 * 当前主要配置静态资源映射，将服务器本地的头像文件目录
 * 映射为 HTTP 可访问的静态资源路径。
 * </p>
 * <p>
 * 配置后，客户端可以通过 /avatars/xxx.png 这样的 URL
 * 直接访问服务器上存储的头像文件，无需编写额外的 Controller。
 * </p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 头像文件的上传存储目录路径。
     * <p>
     * 从配置文件中读取 avatar.upload-dir 属性，
     * 如果未配置则默认为当前目录下的 ./avatars 文件夹。
     * </p>
     */
    @Value("${avatar.upload-dir:./avatars}")
    private String avatarUploadDir;

    /**
     * 添加静态资源处理器。
     * <p>
     * 将 URL 路径 /avatars/** 映射到服务器本地的头像存储目录，
     * 使得头像文件可以通过 HTTP 直接访问。
     * 例如：/avatars/1.jpg 会映射到 {avatarUploadDir}/1.jpg
     * </p>
     *
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /avatars/** URL 路径映射到本地文件系统目录
        // "file:" 前缀表示从本地文件系统加载资源
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:" + avatarUploadDir + "/");
    }
}
