package com.drivingschool.practice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drivingschool.practice.entity.PracticeFavorite;
import com.drivingschool.practice.entity.QuestionBank;
import com.drivingschool.practice.mapper.PracticeFavoriteMapper;
import com.drivingschool.practice.mapper.QuestionBankMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 题目收藏服务类
 * <p>
 * 处理学员的题目收藏功能，包括收藏、取消收藏、查询收藏列表和检查收藏状态。
 * 防止重复收藏（幂等性保证）。
 * </p>
 *
 * @author drivingschool
 */
@Service
@RequiredArgsConstructor
public class PracticeFavoriteService extends ServiceImpl<PracticeFavoriteMapper, PracticeFavorite> {

    /** 题库Mapper，用于根据题目ID批量查询题目详情 */
    private final QuestionBankMapper questionBankMapper;

    /**
     * 收藏题目
     */
    public void addFavorite(Long userId, Long questionId) {
        Long count = count(new LambdaQueryWrapper<PracticeFavorite>()
                .eq(PracticeFavorite::getUserId, userId)
                .eq(PracticeFavorite::getQuestionId, questionId));
        if (count == 0) {
            PracticeFavorite fav = new PracticeFavorite();
            fav.setUserId(userId);
            fav.setQuestionId(questionId);
            save(fav);
        }
    }

    /**
     * 取消收藏
     */
    public void removeFavorite(Long userId, Long questionId) {
        remove(new LambdaQueryWrapper<PracticeFavorite>()
                .eq(PracticeFavorite::getUserId, userId)
                .eq(PracticeFavorite::getQuestionId, questionId));
    }

    /**
     * 获取收藏的题目列表
     */
    public List<QuestionBank> getFavoriteQuestions(Long userId) {
        List<PracticeFavorite> favs = list(new LambdaQueryWrapper<PracticeFavorite>()
                .eq(PracticeFavorite::getUserId, userId)
                .orderByDesc(PracticeFavorite::getCreateTime));
        if (favs.isEmpty()) {
            return List.of();
        }
        List<Long> questionIds = favs.stream()
                .map(PracticeFavorite::getQuestionId)
                .toList();
        return questionBankMapper.selectBatchIds(questionIds);
    }

    /**
     * 检查是否已收藏
     */
    public boolean isFavorite(Long userId, Long questionId) {
        return count(new LambdaQueryWrapper<PracticeFavorite>()
                .eq(PracticeFavorite::getUserId, userId)
                .eq(PracticeFavorite::getQuestionId, questionId)) > 0;
    }
}
