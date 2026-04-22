package com.peng.codegenerationplatform.exception;

import com.peng.codegenerationplatform.common.BaseResponse;
import com.peng.codegenerationplatform.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e, HttpServletResponse response) {
        log.error("BusinessException", e);
        if (response.isCommitted()) {
            writeSseErrorEvent(response, e.getCode(), e.getMessage());
            return null;
        }
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e, HttpServletResponse response) {
        log.error("RuntimeException", e);
        if (response.isCommitted()) {
            writeSseErrorEvent(response, ErrorCode.SYSTEM_ERROR.getCode(), "服务端流式生成中断");
            return null;
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    private void writeSseErrorEvent(HttpServletResponse response, int code, String msg) {
        try {
            String sseEvent = "event: error\ndata: {\"code\":" + code + ",\"msg\":\"" + msg + "\"}\n\n";
            response.getOutputStream().write(sseEvent.getBytes(StandardCharsets.UTF_8));
            response.getOutputStream().flush();
        } catch (IOException ioEx) {
            log.warn("SSE 写入错误事件失败", ioEx);
        }
    }
}