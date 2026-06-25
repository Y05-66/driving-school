package com.drivingschool.video.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drivingschool.video.entity.Video;
import com.drivingschool.video.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 教学视频服务类
 * <p>
 * 提供教学视频的查询和管理功能，包括：
 * - 按科目和分类分页查询上架视频
 * - 获取视频详情并原子递增播放次数
 * - 获取视频分类列表
 * </p>
 *
 * @author drivingschool
 */
@Service
@RequiredArgsConstructor
public class VideoService extends ServiceImpl<VideoMapper, Video> {

    /**
     * 分页查询上架视频
     * <p>
     * 只查询status=1（上架）的视频，支持按科目和分类筛选，
     * 结果按sortOrder升序排列。
     * </p>
     *
     * @param page     分页参数
     * @param subject  科目筛选，如"科目一"、"科目二"，不传则查全部
     * @param category 分类筛选，如"倒车入库"、"侧方停车"，不传则查全部
     * @return 分页后的视频列表
     */
    public Page<Video> page(Page<Video> page, String subject, String category) {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getStatus, 1);
        if (subject != null) {
            wrapper.eq(Video::getSubject, subject);
        }
        if (category != null) {
            wrapper.eq(Video::getCategory, category);
        }
        wrapper.orderByAsc(Video::getSortOrder);
        return page(page, wrapper);
    }

    /**
     * 获取视频详情（原子递增播放次数）
     * <p>
     * 使用SQL层面的原子递增（view_count = view_count + 1）更新播放次数，
     * 避免并发场景下的竞态条件。递增后再查询返回完整的视频信息。
     * </p>
     *
     * @param id 视频ID
     * @return 视频详情，如果不存在则返回null
     */
    public Video getDetail(Long id) {
        // 原子递增播放次数，避免并发竞争
        update(new LambdaUpdateWrapper<Video>()
                .eq(Video::getId, id)
                .setSql("view_count = view_count + 1"));
        return getById(id);
    }

    /**
     * 获取所有视频分类
     * <p>
     * 查询所有上架视频的去重分类列表，可按科目筛选。
     * 用于前端展示分类筛选标签。
     * </p>
     *
     * @param subject 科目筛选，不传则返回所有科目的分类
     * @return 去重后的分类名称列表
     */
    public List<String> getCategories(String subject) {
        return listObjs(new LambdaQueryWrapper<Video>()
                .eq(Video::getStatus, 1)
                .eq(subject != null, Video::getSubject, subject)
                .select(Video::getCategory)
                .groupBy(Video::getCategory), obj -> (String) obj);
    }
}
