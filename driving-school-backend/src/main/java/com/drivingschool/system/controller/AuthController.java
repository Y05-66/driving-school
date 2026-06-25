package com.drivingschool.system.controller;

import com.drivingschool.common.exception.BusinessException;
import com.drivingschool.common.result.R;
import com.drivingschool.common.utils.JwtUtils;
import com.drivingschool.common.utils.RedisUtils;
import com.drivingschool.system.dto.ChangePasswordDTO;
import com.drivingschool.system.entity.SysUser;
import com.drivingschool.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证管理控制器
 * <p>
 * 功能说明：处理用户认证相关的所有操作，包括登录、登出、Token刷新、获取当前用户信息、
 * 修改密码以及上传头像等功能。
 * </p>
 * 基础路径：/auth
 * <p>
 * 所有认证接口均无需特殊权限（登录/登出/刷新Token），但部分接口需要有效的JWT令牌。
 * </p>
 *
 * @author drivingschool
 */
@Slf4j
@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    /** Spring Security 认证管理器，用于验证用户名密码 */
    private final AuthenticationManager authenticationManager;

    /** JWT工具类，用于生成和解析JWT令牌 */
    private final JwtUtils jwtUtils;

    /** Redis工具类，用于存储Token黑名单等缓存操作 */
    private final RedisUtils redisUtils;

    /** 用户服务，用于查询和更新用户信息 */
    private final SysUserService userService;

    /** 密码编码器，用于密码加密和验证 */
    private final PasswordEncoder passwordEncoder;

    /** 头像上传目录路径，默认为 ./avatars */
    @Value("${avatar.upload-dir:./avatars}")
    private String avatarUploadDir;

    /** 头像访问基础URL，默认为 /api/avatars */
    @Value("${avatar.base-url:/api/avatars}")
    private String avatarBaseUrl;

    /**
     * 登录请求数据传输对象
     * 用于接收前端提交的用户名和密码
     */
    @Data
    public static class LoginRequest {
        /** 用户名 */
        @jakarta.validation.constraints.NotBlank(message = "用户名不能为空")
        private String username;
        /** 密码 */
        @jakarta.validation.constraints.NotBlank(message = "密码不能为空")
        private String password;
    }

    /**
     * 刷新Token请求数据传输对象
     * 用于接收前端提交的刷新令牌
     */
    @Data
    public static class RefreshRequest {
        /** 刷新令牌 */
        @jakarta.validation.constraints.NotBlank(message = "refreshToken不能为空")
        private String refreshToken;
    }

    /**
     * 用户登录接口
     * <p>
     * 验证用户名和密码，成功后生成accessToken和refreshToken返回给前端。
     * 如果用户不存在或已被禁用，返回相应的错误信息。
     * </p>
     *
     * @param request 登录请求对象，包含用户名和密码
     * @return 登录成功返回包含accessToken、refreshToken和过期时间的Map；失败返回错误信息
     * @summary 用户登录
     * @权限要求 无需权限（公开接口）
     */
    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        // 先检查用户是否存在和状态，避免时序攻击泄露用户信息
        SysUser preCheck = userService.getByUsername(request.getUsername());
        if (preCheck == null || preCheck.getStatus() == 0) {
            // 统一返回相同的错误信息，防止枚举用户
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 使用Spring Security认证管理器验证用户名密码
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 查询用户信息
        SysUser user = userService.getByUsername(request.getUsername());
        if (user == null) {
            return R.fail(401, "用户不存在");
        }
        // 检查用户状态是否被禁用（0表示禁用）
        if (user.getStatus() == 0) {
            return R.fail(403, "账号已被禁用");
        }

        // 生成访问令牌和刷新令牌
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());

        // 组装返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        data.put("refreshToken", refreshToken);
        data.put("expiresIn", jwtUtils.getAccessTokenExpiration() / 1000);

        return R.ok(data);
    }

    /**
     * 刷新Token接口
     * <p>
     * 使用有效的refreshToken换取新的accessToken和refreshToken。
     * 实现令牌无感续期，避免用户频繁重新登录。
     * </p>
     *
     * @param request 刷新请求对象，包含refreshToken
     * @return 成功返回新的accessToken、refreshToken和过期时间；失败返回错误信息
     * @summary 刷新Token
     * @权限要求 需要有效的refreshToken
     */
    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public R<Map<String, Object>> refresh(@Valid @RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        // 验证refreshToken是否有效
        if (!StringUtils.hasText(refreshToken) || !jwtUtils.validateToken(refreshToken)) {
            return R.fail(401, "refreshToken无效");
        }

        // 验证token类型必须是refresh类型
        String tokenType = jwtUtils.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            return R.fail(401, "token类型错误");
        }

        // 从token中解析用户ID并查询用户
        Long userId = jwtUtils.getUserIdFromToken(refreshToken);
        SysUser user = userService.getById(userId);
        if (user == null || user.getStatus() == 0) {
            return R.fail(401, "用户不存在或已被禁用");
        }

        // 生成新的访问令牌和刷新令牌
        String newAccessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());

        // 组装返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", newAccessToken);
        data.put("refreshToken", newRefreshToken);
        data.put("expiresIn", jwtUtils.getAccessTokenExpiration() / 1000);

        return R.ok(data);
    }

    /**
     * 用户登出接口
     * <p>
     * 将当前accessToken加入Redis黑名单，使其立即失效。
     * 黑名单有效期与token剩余有效期一致。
     * </p>
     *
     * @param request HTTP请求对象，用于从中提取JWT令牌
     * @return 操作成功返回R.ok()
     * @summary 用户登出
     * @权限要求 需要有效的JWT令牌
     */
    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        // 从请求头中提取token
        String token = jwtUtils.extractToken(request);
        if (StringUtils.hasText(token)) {
            // 将token加入黑名单，过期时间取token剩余有效期
            long remaining = jwtUtils.getRemainingTime(token);
            if (remaining > 0) {
                redisUtils.set("token:blacklist:" + token, "1", remaining, TimeUnit.MILLISECONDS);
            }
        }
        return R.ok();
    }

    /**
     * 获取当前登录用户信息接口
     * <p>
     * 根据请求中的JWT令牌解析当前用户，返回用户的基本信息。
     * </p>
     *
     * @param request HTTP请求对象，用于从中提取JWT令牌
     * @return 包含用户id、username、realName、phone、avatar、role的Map
     * @summary 获取当前用户信息
     * @权限要求 需要有效的JWT令牌
     */
    @Operation(summary = "当前用户信息")
    @GetMapping("/info")
    public R<Map<String, Object>> info(HttpServletRequest request) {
        // 从请求头提取并验证token
        String token = jwtUtils.extractToken(request);
        log.info("Auth Info - Token present: {}", token != null);

        if (token == null || !jwtUtils.validateToken(token)) {
            log.warn("Auth Info - Token invalid");
            return R.fail(401, "token无效");
        }

        // 从token中解析用户ID
        Long userId = jwtUtils.getUserIdFromToken(token);
        log.info("Auth Info - UserId: {}", userId);

        if (userId == null) {
            log.warn("Auth Info - UserId is null");
            return R.fail(401, "token无效");
        }

        // 查询用户信息
        SysUser user = userService.getById(userId);
        log.info("Auth Info - User found: {}", user != null);

        if (user == null) {
            log.warn("Auth Info - User not found for id: {}", userId);
            return R.fail(401, "用户不存在");
        }

        // 组装用户信息返回
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("phone", user.getPhone());
        data.put("avatar", user.getAvatar());
        data.put("role", user.getRole());

        return R.ok(data);
    }

    /**
     * 修改密码接口
     * <p>
     * 验证旧密码正确后，将密码更新为新密码。
     * </p>
     *
     * @param authentication Spring Security认证对象，包含当前用户身份信息
     * @param dto            修改密码DTO，包含旧密码和新密码（经过Jakarta Validation校验）
     * @return 操作成功返回R.ok()，旧密码错误抛出BusinessException
     * @summary 修改密码
     * @权限要求 需要已登录用户（任何角色）
     */
    @Operation(summary = "修改密码")
    @PutMapping("/change-password")
    public R<Void> changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordDTO dto) {
        // 从认证信息中获取用户ID
        Long userId = (Long) authentication.getPrincipal();
        SysUser user = userService.getById(userId);
        if (user == null) {
            return R.fail(401, "用户不存在");
        }
        // 验证旧密码是否正确
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }
        // 重置为新密码
        userService.resetPassword(userId, dto.getNewPassword());
        return R.ok();
    }

    /**
     * 上传头像接口
     * <p>
     * 接收图片文件，校验格式（jpg/png/gif/webp）和大小（不超过2MB），
     * 保存到服务器并更新用户头像URL。
     * </p>
     *
     * @param authentication Spring Security认证对象，包含当前用户身份信息
     * @param file           上传的图片文件
     * @return 成功返回头像的访问URL
     * @summary 上传头像
     * @权限要求 需要已登录用户（任何角色）
     * @throws BusinessException 当文件为空、格式不支持或大小超限时抛出
     */
    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public R<String> uploadAvatar(Authentication authentication, @RequestParam("file") MultipartFile file) {
        // 校验文件是否为空
        if (file.isEmpty()) {
            throw new BusinessException("请选择图片");
        }

        // 校验文件格式是否为支持的图片类型
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.matches("(?i).*\\.(jpg|jpeg|png|gif|webp)$")) {
            throw new BusinessException("仅支持 jpg/png/gif/webp 格式");
        }

        // 校验文件大小是否超过2MB
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException("图片大小不能超过2MB");
        }

        // 生成唯一文件名（UUID + 原始扩展名）
        String ext = originalName.substring(originalName.lastIndexOf("."));
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        // 确保上传目录存在
        File dir = new File(avatarUploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 保存文件到服务器
        try {
            file.transferTo(new File(dir, filename));
        } catch (IOException e) {
            throw new BusinessException("上传失败");
        }

        // 更新用户头像URL
        Long userId = (Long) authentication.getPrincipal();
        String avatarUrl = avatarBaseUrl + "/" + filename;
        SysUser update = new SysUser();
        update.setId(userId);
        update.setAvatar(avatarUrl);
        userService.update(update);

        return R.ok(avatarUrl);
    }
}
