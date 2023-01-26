package com.itmo.eva.service.rank;

import com.itmo.eva.model.entity.ScoreHistory;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itmo.eva.model.vo.ScoreHistoryVo;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
* @author chenjiahan
* @description 针对表【e_score_history(总分表)】的数据库操作Service
* @createDate 2023-01-25 13:28:07
*/
public interface ScoreHistoryService extends IService<ScoreHistory> {

    /**
     * 获取所有中方教师分数
     * @return 中方分数
     */
    List<ScoreHistoryVo> getChineseScore(Integer eid);

    /**
     * 获取所有俄方教师分数
     * @return 俄方分数
     */
    List<ScoreHistoryVo> getRussianScore(Integer eid);

    /**
     * 导出中方教师排名
     *
     * @param response 响应
     * @param eid 评测id
     */
    void exportChineseExcel(HttpServletResponse response, Integer eid);

    /**
     * 导出俄方教师排名
     *
     * @param response 响应
     * @param eid 评测id
     */
    void exportRussianExcel(HttpServletResponse response, Integer eid);
}
