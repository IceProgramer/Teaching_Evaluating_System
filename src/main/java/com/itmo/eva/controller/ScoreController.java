package com.itmo.eva.controller;

import com.itmo.eva.common.BaseResponse;
import com.itmo.eva.common.ErrorCode;
import com.itmo.eva.common.ResultUtils;
import com.itmo.eva.exception.BusinessException;
import com.itmo.eva.model.dto.score.ScoreFilterRequest;
import com.itmo.eva.model.vo.score.TeacherAllScoreVo;
import com.itmo.eva.model.vo.score.TeacherSecondScoreVo;
import com.itmo.eva.model.vo.score.TeacherSystemScoreVo;
import com.itmo.eva.service.ScoreHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 分数排名接口
 */
@RestController
@Slf4j
@RequestMapping("/score")
public class ScoreController {

    @Resource
    private ScoreHistoryService scoreHistoryService;

    /**
     * 获取教师所有排名
     *
     * @param scoreFilterRequest 
     * @return
     */
    @PostMapping("/get/rank/teacher/total")
    public BaseResponse<List<TeacherAllScoreVo>> getTeacherTotalRank(@RequestBody ScoreFilterRequest scoreFilterRequest) {
        List<TeacherAllScoreVo> teacherTotalRank = scoreHistoryService.getTeacherTotalRank(scoreFilterRequest);
        if (teacherTotalRank == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(teacherTotalRank);
    }

    @PostMapping("/get/rank/teacher/first")
    public BaseResponse<List<TeacherSystemScoreVo>> getTeacherFirstRank(@RequestBody ScoreFilterRequest scoreFilterRequest) {
        List<TeacherSystemScoreVo> teacherFirstRank = scoreHistoryService.getTeacherFirstRank(scoreFilterRequest);
        if (teacherFirstRank == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(teacherFirstRank);
    }

    @PostMapping("/get/rank/teacher/second")
    public BaseResponse<List<TeacherSecondScoreVo>> getTeacherSecondRank(@RequestBody ScoreFilterRequest scoreFilterRequest) {
        List<TeacherSecondScoreVo> teacherSecondScore = scoreHistoryService.getTeacherSecondScore(scoreFilterRequest);
        if (teacherSecondScore == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(teacherSecondScore);
    }

    @PostMapping("/excel/export")
    public void getExcel(HttpServletResponse response, @RequestBody ScoreFilterRequest scoreFilterRequest) {
        scoreHistoryService.exportExcel(response, scoreFilterRequest);
    }

    @GetMapping("/test")
    public void excel(HttpServletResponse response) {
        ScoreFilterRequest scoreFilterRequest = new ScoreFilterRequest();
        scoreFilterRequest.setIdentity(0);
        scoreHistoryService.exportExcel(response, scoreFilterRequest);
    }
}

