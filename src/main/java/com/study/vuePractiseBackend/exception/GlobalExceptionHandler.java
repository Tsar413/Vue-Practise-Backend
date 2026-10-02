package com.study.vuePractiseBackend.exception;

import com.study.vuePractiseBackend.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 请求体为空、JSON格式错误、JSON字段类型错误。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadableBody(HttpMessageNotReadableException e) {

        return error(
                HttpStatus.BAD_REQUEST,
                "请求体不能为空，请检查JSON格式和字段类型"
        );
    }

    /**
     * 缺少必填请求参数，例如没有传status。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(MissingServletRequestParameterException e) {

        return error(
                HttpStatus.BAD_REQUEST,
                "缺少必填参数：" + e.getParameterName()
        );
    }

    /**
     * 请求参数类型错误，例如status=abc。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleParameterTypeMismatch(MethodArgumentTypeMismatchException e) {

        return error(
                HttpStatus.BAD_REQUEST,
                "参数类型错误：" + e.getName()
        );
    }

    /**
     * 主键或唯一字段重复。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKey(DuplicateKeyException e) {

        log.warn("数据库唯一键冲突", e);

        return error(
                HttpStatus.CONFLICT,
                "记录已存在，请检查编号或其他唯一字段"
        );
    }

    /**
     * 数据库约束异常，例如必填字段缺失、字段长度超限。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {

        log.error("数据库数据约束异常", e);

        return error(
                HttpStatus.CONFLICT,
                "数据不符合数据库约束，操作失败"
        );
    }

    /**
     * 处理Service中主动抛出的状态异常。
     * 例如创建用户或工作空间失败。
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Result<Void>> handleIllegalState(IllegalStateException e) {

        log.error("业务执行异常", e);

        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "操作失败，请稍后重试"
        );
    }

    /**
     * 未单独处理的异常。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {

        log.error("系统未处理异常", e);

        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "系统异常，请稍后重试"
        );
    }

    /**
     * 保持HTTP状态码与响应体code一致。
     */
    private ResponseEntity<Result<Void>> error(HttpStatus status, String message) {

        return ResponseEntity.status(status)
                .body(new Result<>(status.value(), message, null));
    }

    /**
     * Excel格式或学生数据校验失败。
     */
    @ExceptionHandler(ExcelImportException.class)
    public ResponseEntity<Result<Void>> handleExcelImport(ExcelImportException e) {

        // 文件解析的底层异常记录到后端日志
        if (e.getCause() != null) {
            log.warn("Excel文件解析失败", e);
        }

        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * 没有上传名为file的文件。
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Result<Void>> handleMissingFile(MissingServletRequestPartException e) {

        return error(
                HttpStatus.BAD_REQUEST,
                "缺少上传文件：" + e.getRequestPartName()
        );
    }

    /**
     * 文件超过上传大小限制。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleUploadTooLarge(MaxUploadSizeExceededException e) {

        return error(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "上传文件或请求超过大小限制，单个Excel文件不能超过5MB"
        );
    }
}