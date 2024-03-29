package com.itmo.eva.service.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itmo.eva.common.ErrorCode;
import com.itmo.eva.exception.BusinessException;
import com.itmo.eva.mapper.*;
import com.itmo.eva.model.dto.evaluate.EvaluateAddRequest;
import com.itmo.eva.model.dto.evaluate.EvaluateUpdateRequest;
import com.itmo.eva.model.entity.System;
import com.itmo.eva.model.entity.*;
import com.itmo.eva.model.vo.Evaluation.EvaluateNameVo;
import com.itmo.eva.model.vo.Evaluation.EvaluateVo;
import com.itmo.eva.model.vo.Evaluation.StudentCompletionVo;
import com.itmo.eva.model.vo.Evaluation.StudentEvaVo;
import com.itmo.eva.service.EvaluateService;
import com.itmo.eva.service.RedlineHistoryService;
import com.itmo.eva.service.ScoreHistoryService;
import com.itmo.eva.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author chenjiahan
* @description 针对表【e_evaluate(评测表)】的数据库操作Service实现
* @createDate 2023-01-23 13:14:03
*/
@Service
@Slf4j
public class EvaluateServiceImpl extends ServiceImpl<EvaluateMapper, Evaluate>
    implements EvaluateService {

    @Resource
    private EvaluateMapper evaluateMapper;

    @Resource
    private StudentMapper studentMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private SystemMapper systemMapper;

    @Resource
    private MarkHistoryMapper markHistoryMapper;

    @Resource
    private TeacherMapper teacherMapper;

    @Resource
    private ScoreHistoryMapper scoreHistoryMapper;

    @Resource
    private StudentClassMapper studentClassMapper;

    @Resource
    private AdminMapper adminMapper;

    @Resource
    private ScoreHistoryService scoreHistoryService;

    @Resource
    private RedlineHistoryService redlineHistoryService;

    @Resource
    private TableMapper tableMapper;

    @Resource
    private AverageScoreMapper averageScoreMapper;

    /**
     * 添加评测
     *
     * @param evaluateAddRequest 评测请求体
     * @return 添加成功
     */
    @Override
    public Boolean addEvaluate(EvaluateAddRequest evaluateAddRequest) {
        if (evaluateAddRequest == null || StringUtils.isBlank(evaluateAddRequest.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空！");
        }
        // 判断评测是否存在 【按照名称查询】
        String name = evaluateAddRequest.getName();
        Evaluate oldEvaluate = evaluateMapper.getEvaluateByName(name);
        if (oldEvaluate != null) {
            throw new BusinessException(ErrorCode.DATA_REPEAT, "已存在该测评！");
        }
        // 判断是否有正在进行的评测
        Evaluate evaluateGoing = evaluateMapper.getEvaluateByStatus();
        if (evaluateGoing != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "已有正在进行的测评！");
        }

        Evaluate evaluate = new Evaluate();
        BeanUtils.copyProperties(evaluateAddRequest, evaluate);
        Date createTime = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        evaluate.setCreate_time(dateFormat.format(createTime));

        // 校验数据
        this.validEvaluate(evaluate, true);
        boolean save = this.save(evaluate);

        // 发布评测给学生
        releaseEvaluation(evaluate.getId());

        return save;
    }

    /**
     * 删除评测
     *
     * @param id 删除id
     * @return 删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteEvaluate(Long id) {
        // 判断是否存在
        Evaluate oldEvaluate = this.getById(id);
        if (oldEvaluate == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "评测不存在");
        }

        // 删除该评测
        boolean remove = this.removeById(id);

        // 删除评测下所有的记录 mark表中
        markHistoryMapper.removeByEid(id.intValue());

        // 删除二级评价表
        String tableName = "e_second_score_" + id;
        tableMapper.removeTable(tableName);

        return remove;
    }

    /**
     * 更新课程
     *
     * @param evaluateUpdateRequest 更新请求体
     * @return 更新成功
     */
    @Override
    public Boolean updateEvaluate(EvaluateUpdateRequest evaluateUpdateRequest) {
        if (evaluateUpdateRequest == null || evaluateUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        Integer id =  evaluateUpdateRequest.getId();
        Evaluate oldEvaluate = this.getById(id);
        if (oldEvaluate == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "评测不存在");
        }
        Evaluate evaluate = new Evaluate();
        BeanUtils.copyProperties(evaluateUpdateRequest, evaluate);

        // 参数校验
        this.validEvaluate(evaluate, false);
        boolean update = this.updateById(evaluate);

        return update;
    }

    /**
     * 根据 id 获取
     *
     * @param id id
     * @return 评测信息
     */
    @Override
    public EvaluateVo getEvaluateById(Integer id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id错误");
        }
        Evaluate evaluate = this.getById(id);
        // 判空
        if (evaluate == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "评测信息不存在");
        }
        EvaluateVo evaluateInfo = new EvaluateVo();
        BeanUtils.copyProperties(evaluate, evaluateInfo);

        return evaluateInfo;
    }

    /**
     * 获取评测列表
     */
    @Override
    public List<EvaluateVo> listEvaluate() {
        List<Evaluate> evaluateList = this.list();
        if (evaluateList == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "没有数据记录");
        }

        List<EvaluateVo> evaluateVoList = evaluateList.stream().map((evaluate) -> {
            EvaluateVo evaluateVo = new EvaluateVo();
            BeanUtils.copyProperties(evaluate, evaluateVo);
            return evaluateVo;
        }).collect(Collectors.toList());
        return evaluateVoList;
    }

    /**
     * 更新评测状态
     * @param eid 评测id
     * @param token 用户token值
     * @return 更改成功
     */
    @Override
    public Boolean updateStatus(Integer eid, String token) {
        // 对传回来的token进行解析 -> 解析出token中对应用户的id
        DecodedJWT decodedJWT = JwtUtil.decodeToken(token);
        Integer id = Integer.valueOf(decodedJWT.getClaim("id").asString());
        Admin admin = adminMapper.selectById(id);

        Evaluate evaluate = this.getById(eid);
        if (ObjectUtils.isEmpty(evaluate)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "不存在该评测记录");
        }

        String operationRecord = admin.getUsername() + "正在对" + evaluate.getName() + "进行更改进行状态的操作";
        log.info(operationRecord);

        // 当前
        Integer status = evaluate.getStatus();
        // 评测为开启状态
        if (status == 1) {
            evaluate.setStatus(0);
            boolean save = this.updateById(evaluate);
            scoreHistoryService.calculateScoreAverage(evaluate.getId());
            log.info("{} 评测平均分计算完成", evaluate.getName());
            // 统计红线指标
            redlineHistoryService.recordRedline(evaluate.getId());
            Date nowTime = Calendar.getInstance().getTime();
            log.info("{} 评测已经结束，当前时间：{}", evaluate.getName(), nowTime.getTime());
            return save;
        }

        // 评测为关闭状态
        Evaluate evaluateGoing = evaluateMapper.getEvaluateByStatus();
        if (!ObjectUtils.isEmpty(evaluateGoing)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仍有正在进行中的评测");
        }
        String eTime = evaluate.getE_time();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            // 判断该评测是否超时
            Date endTime = dateFormat.parse(eTime);
            Date nowTime = Calendar.getInstance().getTime();
            if (nowTime.after(endTime)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该评测已超时");
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        // 将评测状态开启
        evaluate.setStatus(1);
        // 删除评测平均分
        this.deleteAverageScore(eid);
        // 删除最终分数
        this.deleteScoreHistory(eid);
        // 更新评测信息
        boolean update = this.updateById(evaluate);

        return update;
    }

    /**
     * 获取完成学生
     *
     * @param eid 评测id
     * @return 完成学生列表
     */
    @Override
    public StudentCompletionVo listStudentCompletion(Integer eid) {
        StudentCompletionVo studentCompletionVo = new StudentCompletionVo();
        // 查找学生aid是否还有状态（state = 0）的字段在mark表中
        // 查询所有学生信息Id
        LambdaQueryWrapper<MarkHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MarkHistory::getEid, eid);
        queryWrapper.select(MarkHistory::getAid);
        // 获取本次评测中所有的学生id
        List<MarkHistory> markHistories = markHistoryMapper.selectList(queryWrapper);
        // 将id提取出来成列表
        if (CollectionUtils.isEmpty(markHistories)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "暂无本次评测信息");
        }
        List<Integer> studentIdList = markHistories.stream().map(MarkHistory::getAid).distinct().collect(Collectors.toList());

        // 获取该评测的所有未完成学生的id
        List<Integer> studentId = markHistoryMapper.getByEidAndState(eid);

        // 判断是否存在该评测信息
        if (studentId == null || studentId.size() == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 对studentId去重 -> 未完成学生id
        List<Integer> undoneStudentId = studentId.stream().distinct().collect(Collectors.toList());
        // 取差集 ->  完成学生名单
        List<Integer> doneStudentId = studentIdList.stream().filter(item -> !undoneStudentId.contains(item)).collect(Collectors.toList());

        // 取出所有班级
        Map<Integer, String> classList = studentClassMapper.selectList(null).stream().collect(Collectors.toMap(StudentClass::getId, StudentClass::getCid));

        List<StudentEvaVo> undoneStudentList = new ArrayList<>();
        // 未完成名单
        for (Integer id : undoneStudentId) {
            StudentEvaVo undoneStudent = new StudentEvaVo();
            Student student = studentMapper.selectById(id);
            String studentName = student.getName();
            String classNumber = classList.get(student.getCid());
            String studentNumber = student.getSid();
            undoneStudent.setStudentId(studentNumber);
            undoneStudent.setName(studentName);
            undoneStudent.setStudentClass(classNumber);
            undoneStudentList.add(undoneStudent);
        }
        studentCompletionVo.setStudentUndone(undoneStudentList);

        // 记录完成同学
        List<StudentEvaVo> doneStudentList = new ArrayList<>();
        for (Integer id : doneStudentId) {
            StudentEvaVo doneStudent = new StudentEvaVo();
            Student student = studentMapper.selectById(id);
            String studentName = student.getName();
            String classNumber = classList.get(student.getCid());
            String studentNumber = student.getSid();
            doneStudent.setStudentId(studentNumber);
            doneStudent.setName(studentName);
            doneStudent.setStudentClass(classNumber);
            doneStudentList.add(doneStudent);
        }
        studentCompletionVo.setStudentDone(doneStudentList);

        return studentCompletionVo;
    }

    /**
     * 导出未完成学生名单
     * @param eid 评测id
     * @param response 响应
     * @return excel文件
     */
    @Override
    public void exportUndoneStudentExcel(Integer eid, HttpServletResponse response) {
        // 获取未完成学生名单
        List<StudentEvaVo> studentUndone = this.listStudentCompletion(eid).getStudentUndone();

        // 建立Excel对象，封装数据
        response.setCharacterEncoding("UTF-8");
        // 创建Excel对象
        XSSFWorkbook wb = new XSSFWorkbook();
        // 创建sheet对象
        XSSFSheet sheet = wb.createSheet("未完成评测学生表");
        // 创建表头
        XSSFRow xssfRow = sheet.createRow(0);
        xssfRow.createCell(0).setCellValue("班级");
        xssfRow.createCell(1).setCellValue("代评学生");
        xssfRow.createCell(2).setCellValue("学号");

        //3.遍历数据，封装Excel工作对象
        for(StudentEvaVo student : studentUndone) {
            XSSFRow dataRow = sheet.createRow(sheet.getLastRowNum() + 1);
            dataRow.createCell(0).setCellValue(student.getStudentClass());
            dataRow.createCell(1).setCellValue(student.getName());
            dataRow.createCell(2).setCellValue(student.getStudentId());
        }

        try {
            //输出Excel文件
            String filename = "未完成学生表.xlsx";
            response.reset();
            response.addHeader("Access-Control-Expose-Headers", "filetype");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; fileName=" + java.net.URLEncoder.encode(filename, "UTF-8"));
            OutputStream output = response.getOutputStream();
            wb.write(output);
            output.close();
        } catch(Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 获取全部已经完成的评测id
     * @return 已经完成的评测id
     */
    @Override
    public List<EvaluateNameVo> getEvaluateName() {
        List<Evaluate> evaluateList = this.baseMapper.getAllEndEvaluation();
        if (CollectionUtils.isEmpty(evaluateList)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        List<EvaluateNameVo> evaluateNameVoList = evaluateList.stream().map(evaluate -> {
            EvaluateNameVo evaluateNameVo = new EvaluateNameVo();
            BeanUtils.copyProperties(evaluate, evaluateNameVo);
            return evaluateNameVo;
        }).collect(Collectors.toList());

        return evaluateNameVoList;
    }

    /**
     * 校验
     *
     * @param evaluate 评测信息
     * @param add     是否为创建校验
     */
    @Override
    public void validEvaluate(Evaluate evaluate, boolean add) {
        if (evaluate == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = evaluate.getName();
        String create_time = evaluate.getCreate_time();
        String start_time = evaluate.getStart_time();
        String e_time = evaluate.getE_time();
        if (add) {
            if (StringUtils.isAnyBlank(name, create_time, start_time, e_time)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据含空");
            }
        }
    }

    /**
     * 分发学生测评
     * @param evaluateId 评测id
     */
    public void releaseEvaluation(Integer evaluateId) {
        // 取出所有学生信息
        List<Student> studentList = studentMapper.selectList(null);

        for (Student student : studentList) {
            // 取出grade和major，来查询学生的响应课程
            Integer grade = student.getGrade();
            Integer major = student.getMajor();
            Long studentId = student.getId();

            // 获取该学生的所有课程信息
            List<Course> courseList = courseMapper.getCourseByMajorAndGrade(major, grade);
            for (Course course : courseList) {
                // 取出教师id
                Long teacherId = course.getTid();
                Integer courseId = course.getId();

                // 取出教师的国籍
                Teacher teacher = teacherMapper.selectById(teacherId);
                Integer identity = teacher.getIdentity();

                // 查询教师的国籍，所对应的所有一级指标
                List<System> systemList = systemMapper.getCountByKind(identity);

                for (System system : systemList) {
                    Integer systemId = system.getId();
                    MarkHistory markHistory = new MarkHistory();
                    markHistory.setTid(teacherId.intValue());
                    markHistory.setCid(courseId);
                    markHistory.setEid(evaluateId);
                    markHistory.setScore(0);
                    markHistory.setSid(systemId);
                    markHistory.setAid(studentId.intValue());
                    markHistory.setState(0);
                    // 插入数据库
                    markHistoryMapper.insert(markHistory);
                }
            }
        }


    }

    /**
     * 删除average表中所有相关评测分数
     * @param eid
     */
    private void deleteAverageScore(Integer eid) {
        LambdaQueryWrapper<AverageScore> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AverageScore::getEid, eid);
        int delete = averageScoreMapper.delete(queryWrapper);
        log.info("删除average表中 {} 条数据", delete);
    }

    private void deleteScoreHistory(Integer eid) {
        LambdaQueryWrapper<ScoreHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ScoreHistory::getEid, eid);
        int remove = scoreHistoryMapper.delete(queryWrapper);
        log.info("删除scoreHistory表中 {} 条数据", remove);
    }


    /**
     * 在这里写一下分发逻辑 【帮助笨蛋开发者梳理逻辑】
     *
     * 学生 =》 课程         根据学生的年级（grade）和专业（major）来查找课程信息
     * 课程 =》 老师         根据课程的tid来查找教师信息
     * 老师 =》 一级指标      根据老师的国籍（identity）来确定需要的一级指标
     *
     */
}




