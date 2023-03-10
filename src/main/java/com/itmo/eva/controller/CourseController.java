package com.itmo.eva.controller;

import com.itmo.eva.common.*;
import com.itmo.eva.exception.BusinessException;
import com.itmo.eva.model.dto.course.CourseAddRequest;
import com.itmo.eva.model.dto.course.CourseUpdateRequest;
import com.itmo.eva.model.vo.course.CourseListVo;
import com.itmo.eva.model.vo.course.CourseVo;
import com.itmo.eva.service.CourseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.List;

/**
 * 课程接口
 */
@RestController
@Slf4j
@RequestMapping("/course")
public class CourseController {

    @Resource
    private CourseService courseService;

    /**
     * 添加课程
     *
     * @param courseAddRequest 添加请求体
     * @return 添加成功
     */
    @PostMapping("/add")
    public BaseResponse<Boolean> addCourse(@RequestBody CourseAddRequest courseAddRequest) {
        if (courseAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "添加参数为空");
        }
        Boolean save = courseService.addCourse(courseAddRequest);

        if (!save) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存数据失败！");
        }

        return ResultUtils.success(true);
    }

    /**
     * 删除课程信息
     *
     * @param deleteRequest 删除请求体
     * @return 删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteCourse(@RequestBody DeleteRequest deleteRequest) {
        Long id = deleteRequest.getId();
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id为空");
        }
        Boolean delete = courseService.deleteCourse(id);

        if (!delete) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除信息失败！");
        }

        return ResultUtils.success(true);
    }

    /**
     * 更新课程信息
     *
     * @param courseUpdateRequest 更新请求体
     * @return 更新成功
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateCourse(@RequestBody CourseUpdateRequest courseUpdateRequest) {
        if (courseUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Boolean update = courseService.updateCourse(courseUpdateRequest);

        if (!update) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新数据失败！");
        }

        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取
     *
     * @param idRequest id请求体
     * @return 课程信息
     */
    @PostMapping("/get")
    public BaseResponse<CourseVo> getCourseById(@RequestBody IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = idRequest.getId();
        CourseVo courseInfo = courseService.getCourseById(id);

        if (courseInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(courseInfo);
    }

    /**
     * 获取列表
     *
     * @return 所有课程信息
     */
    @GetMapping("/list")
    public BaseResponse<List<CourseVo>> listCourse() {
        List<CourseVo> courseVoList = courseService.listCourse();
        if (courseVoList == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(courseVoList);
    }

    /**
     * Excel文件批量课程教师信息
     * @param file excel
     * @return 保存成功
     */
    @PostMapping("/excel/import")
    public BaseResponse<Boolean> saveExcel(@RequestParam("file") MultipartFile file) {

        Boolean isImport = courseService.excelImport(file);

        if (!isImport) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存文件失败！");
        }

        return ResultUtils.success(true);
    }

    /**
     * 下载示例Excel
     * @param response 请求
     * @return 示例
     */
    @GetMapping("/excel/export")
    public void exportExcel(HttpServletResponse response) {
        // 建立Excel对象，封装数据
        response.setCharacterEncoding("UTF-8");
        // 创建Excel对象
        XSSFWorkbook wb = new XSSFWorkbook();
        // 创建sheet对象
        XSSFSheet sheet = wb.createSheet("课程信息模版");
        // 创建表头
        XSSFRow xssfRow = sheet.createRow(0);
        xssfRow.createCell(0).setCellValue("课程名称");
        xssfRow.createCell(1).setCellValue("课程英文名");
        xssfRow.createCell(2).setCellValue("授课专业");
        xssfRow.createCell(3).setCellValue("授课教师");
        xssfRow.createCell(4).setCellValue("年级");

        try {
            //输出Excel文件
            String filename = "课程信息模版.xlsx";
            response.reset();
            response.addHeader("Access-Control-Expose-Headers", "filetype");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; fileName=" + java.net.URLEncoder.encode(filename, "UTF-8"));
            OutputStream output = response.getOutputStream();
            wb.write(output);
            output.close();
        } catch(Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "模版下载失败");
        }

    }


}
