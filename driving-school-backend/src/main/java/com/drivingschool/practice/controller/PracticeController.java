package com.drivingschool.practice.controller;

import com.drivingschool.common.result.R;
import com.drivingschool.practice.entity.QuestionBank;
import com.drivingschool.practice.service.PracticeFavoriteService;
import com.drivingschool.practice.service.PracticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 在线刷题控制器
 * <p>
 * 提供在线刷题相关的RESTful API接口，包括：
 * - 获取随机题目（按科目和数量）
 * - 提交答案并自动判题
 * - 查看刷题统计（正确率、答题数等）
 * - 错题本（答错的题目列表）
 * - 题目收藏（收藏/取消收藏/收藏列表）
 * </p>
 *
 * @author drivingschool
 * @since 1.0.0
 */
@Tag(name = "在线刷题")
@RestController
@RequestMapping("/practice")
@RequiredArgsConstructor
public class PracticeController {

    /** 刷题服务，处理答题、统计和错题的核心业务逻辑 */
    private final PracticeService practiceService;
    /** 收藏服务，处理题目的收藏和取消收藏 */
    private final PracticeFavoriteService favoriteService;

    /**
     * 获取随机题目
     * <p>
     * 根据科目类型和数量随机抽取题目，用于学员在线刷题练习。
     * </p>
     *
     * @param subject 科目类型（1-科目一，4-科目四）
     * @param count   题目数量，默认10道
     * @return 随机题目列表
     */
    @Operation(summary = "获取随机题目")
    @GetMapping("/questions")
    @PreAuthorize("hasRole('STUDENT')")
    public R<List<QuestionBank>> getRandomQuestions(@RequestParam Integer subject,
                                                     @RequestParam(defaultValue = "10") Integer count) {
        return R.ok(practiceService.getRandomQuestions(subject, count));
    }

    /**
     * 提交答案
     * <p>
     * 学员提交答案后自动判题，记录答题结果。
     * 如果答错，自动加入错题本。
     * </p>
     *
     * @param authentication Spring Security认证对象
     * @param params         请求参数，包含questionId（题目ID）、studentAnswer（学员答案）、practiceTime（答题耗时，可选）
     * @return 答题结果，包含是否正确、正确答案、解析等
     */
    @Operation(summary = "提交答案")
    @PostMapping("/answer")
    @PreAuthorize("hasRole('STUDENT')")
    public R<Map<String, Object>> submitAnswer(Authentication authentication,
                                                @RequestBody Map<String, Object> params) {
        Long userId = (Long) authentication.getPrincipal();
        if (params.get("questionId") == null) {
            return R.fail("题目ID不能为空");
        }
        if (params.get("studentAnswer") == null) {
            return R.fail("答案不能为空");
        }
        Long questionId;
        Integer practiceTime;
        try {
            questionId = Long.valueOf(params.get("questionId").toString());
        } catch (NumberFormatException e) {
            return R.fail("题目ID格式无效");
        }
        String studentAnswer = params.get("studentAnswer").toString();
        try {
            practiceTime = params.get("practiceTime") != null
                    ? Integer.valueOf(params.get("practiceTime").toString()) : null;
        } catch (NumberFormatException e) {
            practiceTime = null;
        }
        return R.ok(practiceService.submitAnswer(userId, questionId, studentAnswer, practiceTime));
    }

    /**
     * 获取我的刷题统计
     * <p>
     * 返回当前学员的刷题统计数据，包括总答题数、正确数、正确率等。
     * </p>
     *
     * @param authentication Spring Security认证对象
     * @return 统计数据Map
     */
    @Operation(summary = "我的刷题统计")
    @GetMapping("/stats")
    @PreAuthorize("hasRole('STUDENT')")
    public R<Map<String, Object>> getMyStats(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return R.ok(practiceService.getMyStats(userId));
    }

    /**
     * 获取错题本
     * <p>
     * 返回当前学员答错的题目列表，用于针对性复习。
     * </p>
     *
     * @param authentication Spring Security认证对象
     * @return 错题列表
     */
    @Operation(summary = "错题本")
    @GetMapping("/wrong-questions")
    @PreAuthorize("hasRole('STUDENT')")
    public R<List<Map<String, Object>>> getWrongQuestions(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return R.ok(practiceService.getWrongQuestions(userId));
    }

    /**
     * 收藏题目
     * <p>
     * 将指定题目加入学员的收藏夹，便于后续重点复习。
     * 重复收藏不会产生重复记录（幂等操作）。
     * </p>
     *
     * @param authentication Spring Security认证对象
     * @param questionId     题目ID
     * @return 操作成功返回R.ok()
     */
    @Operation(summary = "收藏题目")
    @PostMapping("/favorites/{questionId}")
    @PreAuthorize("hasRole('STUDENT')")
    public R<Void> addFavorite(Authentication authentication, @PathVariable Long questionId) {
        Long userId = (Long) authentication.getPrincipal();
        favoriteService.addFavorite(userId, questionId);
        return R.ok();
    }

    /**
     * 取消收藏
     * <p>
     * 将指定题目从学员的收藏夹中移除。
     * </p>
     *
     * @param authentication Spring Security认证对象
     * @param questionId     题目ID
     * @return 操作成功返回R.ok()
     */
    @Operation(summary = "取消收藏")
    @DeleteMapping("/favorites/{questionId}")
    @PreAuthorize("hasRole('STUDENT')")
    public R<Void> removeFavorite(Authentication authentication, @PathVariable Long questionId) {
        Long userId = (Long) authentication.getPrincipal();
        favoriteService.removeFavorite(userId, questionId);
        return R.ok();
    }

    /**
     * 获取收藏列表
     * <p>
     * 返回当前学员收藏的所有题目列表，按收藏时间倒序排列。
     * </p>
     *
     * @param authentication Spring Security认证对象
     * @return 收藏的题目列表
     */
    @Operation(summary = "收藏列表")
    @GetMapping("/favorites")
    @PreAuthorize("hasRole('STUDENT')")
    public R<List<QuestionBank>> getFavorites(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return R.ok(favoriteService.getFavoriteQuestions(userId));
    }
}
