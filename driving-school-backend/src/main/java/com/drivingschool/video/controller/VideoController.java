package com.drivingschool.video.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.result.PageResult;
import com.drivingschool.common.result.R;
import com.drivingschool.video.entity.Video;
import com.drivingschool.video.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教学视频控制器
 */
@Tag(name = "教学视频")
@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    /**
     * 视频列表（分页）
     */
    @Operation(summary = "视频列表")
    @GetMapping
    public R<PageResult<Video>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String category) {
        Page<Video> page = new Page<>(pageNum, pageSize);
        Page<Video> result = videoService.page(page, subject, category);
        return R.ok(PageResult.of(result));
    }

    /**
     * 视频详情
     */
    @Operation(summary = "视频详情")
    @GetMapping("/{id}")
    public R<Video> detail(@PathVariable Long id) {
        return R.ok(videoService.getDetail(id));
    }

    /**
     * 获取分类列表
     */
    @Operation(summary = "视频分类")
    @GetMapping("/categories")
    public R<List<String>> categories(@RequestParam(required = false) String subject) {
        return R.ok(videoService.getCategories(subject));
    }

    /**
     * 添加视频（管理员）
     */
    @Operation(summary = "添加视频")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Video> add(@RequestBody Video video) {
        video.setViewCount(0);
        video.setStatus(1);
        videoService.save(video);
        return R.ok(video);
    }

    /**
     * 删除视频（管理员）
     */
    @Operation(summary = "删除视频")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        videoService.removeById(id);
        return R.ok();
    }
}
