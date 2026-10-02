package com.study.vuePractiseBackend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StudentImportResultDTO {

    // 非空数据行总数
    private int total;

    private int successCount;

    // 学号已存在
    private int skippedCount;

    // 格式、班级、数据库写入等失败
    private int failedCount;

    // 只记录跳过和失败的行
    private List<RowResult> details = new ArrayList<>();

    public record RowResult(int rowNumber, String studentId, String status, String message) {
    }
}