package com.itmo.eva.controller;

import com.itmo.eva.common.*;
import com.itmo.eva.exception.BusinessException;
import com.itmo.eva.model.dto.teacher.TeacherAddRequest;
import com.itmo.eva.model.dto.teacher.TeacherUpdateRequest;
import com.itmo.eva.model.vo.TeacherVo;
import com.itmo.eva.service.TeacherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

/**
 * 教师接口
 */
@RestController
@Slf4j
@RequestMapping("/teacher")
public class TeacherController {

    @Resource
    private TeacherService teacherService;

    /**
     * 添加教师
     *
     * @param teacherAddRequest 添加请求体
     * @return 添加成功
     */
    @PostMapping("/add")
    public BaseResponse<Boolean> addTeacher(@RequestBody TeacherAddRequest teacherAddRequest) {
        if (teacherAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "添加参数为空");
        }
        Boolean save = teacherService.addTeacher(teacherAddRequest);

        if (!save) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存数据失败！");
        }

        return ResultUtils.success(true);
    }

    /**
     * 删除教师信息
     *
     * @param deleteRequest 删除请求体
     * @return 删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeacher(@RequestBody DeleteRequest deleteRequest) {
        Long id = deleteRequest.getId();
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id为空");
        }
        Boolean delete = teacherService.deleteTeacher(id);

        if (!delete) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除信息失败！");
        }

        return ResultUtils.success(true);
    }

    /**
     * 更新教师信息
     *
     * @param teacherUpdateRequest 更新请求体
     * @return 更新成功
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeacher(@RequestBody TeacherUpdateRequest teacherUpdateRequest) {
        if (teacherUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Boolean update = teacherService.updateTeacher(teacherUpdateRequest);

        if (!update) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新数据失败！");
        }

        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取
     *
     * @param idRequest id请求体
     * @return 教师信息
     */
    @PostMapping("/get")
    public BaseResponse<TeacherVo> getTeacherById(@RequestBody IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = idRequest.getId();
        TeacherVo teacherInfo = teacherService.getTeacherById(id);

        if (teacherInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(teacherInfo);
    }

    /**
     * 获取列表
     *
     * @return 所有教师信息
     */
    @GetMapping("/list")
    public BaseResponse<List<TeacherVo>> listTeacher() {
        List<TeacherVo> teacherVoList = teacherService.listTeacher();
        if (teacherVoList == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(teacherVoList);
    }

    /**
     * Excel文件批量上传教师信息
     *
     * @param file excel
     * @return 保存成功
     */
    @PostMapping("/excel/import")
    public BaseResponse<Boolean> saveExcel(@RequestParam("file") MultipartFile file) {

        Boolean isImport = teacherService.excelImport(file);

        if (!isImport) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存文件失败！");
        }

        return ResultUtils.success(true);
    }

    /**
     * 下载示例Excel
     *
     * @param response 请求
     * @return 示例
     */
    @GetMapping("/excel/export")
    public BaseResponse<Boolean> exportExcel(HttpServletResponse response) {

        //2.建立Excel对象，封装数据
        response.setCharacterEncoding("UTF-8");
        //2.1创建Excel对象
        XSSFWorkbook wb = new XSSFWorkbook();
        //2.3创建sheet对象
        XSSFSheet sheet = wb.createSheet("教师信息表");
        //2.3创建表头
        XSSFRow xssfRow = sheet.createRow(0);
        xssfRow.createCell(0).setCellValue("教师名称");
        xssfRow.createCell(1).setCellValue("性别");
        xssfRow.createCell(2).setCellValue("年龄");
        xssfRow.createCell(3).setCellValue("职位");
        xssfRow.createCell(4).setCellValue("职称");
        xssfRow.createCell(5).setCellValue("专业");
        xssfRow.createCell(6).setCellValue("邮箱");
        xssfRow.createCell(7).setCellValue("国籍");
        // 建立输出流，输出浏览器文件
        OutputStream os = null;

        try {
            String folderPath = "C:\\excel";
            //创建上传文件目录
            File folder = new File(folderPath);
            //如果文件夹不存在创建对应的文件夹
            if (!folder.exists()) {
                folder.mkdirs();
            }
            //设置文件名
            String fileName = "教师信息表" + ".xlsx";
            String savePath = folderPath + File.separator + fileName;
            OutputStream fileOut = new FileOutputStream(savePath);
            wb.write(fileOut);
            fileOut.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null)
                    os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return ResultUtils.success(true);
    }

    /**
     * 获取中方教师信息
     *
     * @return
     */
    @GetMapping("/get/china")
    public BaseResponse<List<TeacherVo>> getChineseTeacher() {
        List<TeacherVo> chineseTeacher = teacherService.getChineseTeacher();
        if (chineseTeacher == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chineseTeacher);
    }

    /**
     * 获取俄方教师信息
     */
    @GetMapping("/get/russia")
    public BaseResponse<List<TeacherVo>> getRussianTeacher() {
        List<TeacherVo> russianTeacher = teacherService.getRussianTeacher();
        if (russianTeacher == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(russianTeacher);
    }


}
