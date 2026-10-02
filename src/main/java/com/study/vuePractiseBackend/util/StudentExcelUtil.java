package com.study.vuePractiseBackend.util;

import com.study.vuePractiseBackend.exception.ExcelImportException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class StudentExcelUtil {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_STUDENTS = 1000;

    private static final String[] HEADERS = {
            "学号", "姓名", "班级编号"
    };

    private StudentExcelUtil() {
    }

    /**
     * 保留Excel行号，便于业务失败时定位。
     */
    public record StudentRow(int rowNumber, String id, String realName, String classId, String errorMessage) {
    }

    public static List<StudentRow> readStudents(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ExcelImportException("请选择非空Excel文件");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ExcelImportException("Excel文件不能超过5MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new ExcelImportException("文件名不能为空");
        }

        String lowerName = filename.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".xls")
                && !lowerName.endsWith(".xlsx")) {
            throw new ExcelImportException("仅支持.xls或.xlsx文件");
        }

        try (InputStream input = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(input)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new ExcelImportException("Excel中没有工作表");
            }

            Sheet sheet = workbook.getSheetAt(0);
            checkHeader(sheet.getRow(0));

            List<StudentRow> students = new ArrayList<>();

            for (Row row : sheet) {
                if (row.getRowNum() == 0 || isEmptyRow(row)) {
                    continue;
                }

                if (students.size() >= MAX_STUDENTS) {
                    throw new ExcelImportException(
                            "每次最多处理1000行学生数据"
                    );
                }

                int rowNumber = row.getRowNum() + 1;
                String id = "";
                String realName = "";
                String classId = "";

                try {
                    id = readText(row.getCell(0), rowNumber, "学号");
                    realName = readText(row.getCell(1), rowNumber, "姓名");
                    classId = readText(row.getCell(2), rowNumber, "班级编号");

                    checkRequired(id, rowNumber, "学号");
                    checkRequired(realName, rowNumber, "姓名");
                    checkRequired(classId, rowNumber, "班级编号");

                    checkLength(id, rowNumber, "学号");
                    checkLength(realName, rowNumber, "姓名");
                    checkLength(classId, rowNumber, "班级编号");

                    students.add(new StudentRow(
                            rowNumber, id, realName, classId, null
                    ));

                } catch (ExcelImportException e) {
                    // 单行错误只记录，继续读取后面的行
                    students.add(new StudentRow(
                            rowNumber,
                            id,
                            realName,
                            classId,
                            e.getMessage()
                    ));
                }
            }

            if (students.isEmpty()) {
                throw new ExcelImportException("Excel中没有学生数据");
            }

            return students;

        } catch (ExcelImportException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new ExcelImportException(
                    "Excel读取失败，请检查文件是否损坏、加密或格式不正确",
                    e
            );
        }
    }

    private static void checkHeader(Row header) {
        if (header == null) {
            throw new ExcelImportException(
                    "第一行必须为表头：学号、姓名、班级编号"
            );
        }

        for (int i = 0; i < HEADERS.length; i++) {
            String value = readText(header.getCell(i), 1, HEADERS[i]);

            if (!HEADERS[i].equals(value)) {
                throw new ExcelImportException(
                        "第1行第" + (i + 1)
                                + "列的表头必须为：" + HEADERS[i]
                );
            }
        }
    }

    /**
     * 只检查需要导入的前三列。
     */
    private static boolean isEmptyRow(Row row) {
        for (int i = 0; i < 3; i++) {
            Cell cell = row.getCell(i);

            if (cell == null || cell.getCellType() == CellType.BLANK) {
                continue;
            }

            if (cell.getCellType() == CellType.STRING
                    && cell.getStringCellValue().isBlank()) {
                continue;
            }

            return false;
        }

        return true;
    }

    /**
     * 固定使用文本，避免编号前导零丢失或长学号精度丢失。
     * 不接受公式、数字、日期等其他单元格类型。
     */
    private static String readText(
            Cell cell, int rowNumber, String fieldName) {

        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }

        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                throw new ExcelImportException(
                        "第" + rowNumber + "行：" + fieldName + "不能是日期"
                );
            }

            double value = cell.getNumericCellValue();

            if (!Double.isFinite(value) || value < 0
                    || value != Math.rint(value)) {
                throw new ExcelImportException(
                        "第" + rowNumber + "行：" + fieldName
                                + "必须是文本或非负整数"
                );
            }

            // Excel长数字可能已丢失精度，不能直接当作学号使用
            if (value >= 1_000_000_000_000_000d) {
                throw new ExcelImportException(
                        "第" + rowNumber + "行：" + fieldName
                                + "超过15位，请设为文本后重新填写原始编号"
                );
            }

            return java.math.BigDecimal.valueOf(value)
                    .toBigIntegerExact()
                    .toString();
        }

        throw new ExcelImportException(
                "第" + rowNumber + "行：" + fieldName
                        + "不支持公式、布尔值或错误单元格"
        );
    }

    private static void checkRequired(
            String value, int rowNumber, String fieldName) {

        if (value.isBlank()) {
            throw new ExcelImportException(
                    "第" + rowNumber + "行：" + fieldName + "不能为空"
            );
        }
    }

    private static void checkLength(
            String value, int rowNumber, String fieldName) {

        if (value.length() > 50) {
            throw new ExcelImportException(
                    "第" + rowNumber + "行：" + fieldName
                            + "不能超过50个字符"
            );
        }
    }
}