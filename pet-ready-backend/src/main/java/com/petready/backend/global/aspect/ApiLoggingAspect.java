package com.petready.backend.global.aspect;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class ApiLoggingAspect {

    // com.petready.backend.domain 패키지 하위의 모든 Controller 빈들을 타겟팅합니다.
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *) || within(@org.springframework.web.bind.annotation.Controller *)")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        
        // Method 정보 획득
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // Swagger @Operation 어노테이션 정보 추출
        String featureName = "Unknown API";
        if (method.isAnnotationPresent(Operation.class)) {
            Operation operation = method.getAnnotation(Operation.class);
            featureName = operation.summary();
        } else {
            // 어노테이션이 없을 경우 메서드 명을 기능명으로 사용
            featureName = method.getName();
        }

        String methodType = request.getMethod();
        String requestUri = request.getRequestURI();
        
        Object result = null;
        int status = 200; // 기본값
        
        try {
            result = joinPoint.proceed();
            
            // Response 객체 획득하여 상태 코드 추출
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletResponse response = attributes.getResponse();
                if (response != null) {
                    status = response.getStatus();
                }
            }
            
            log.info("[{}] {} {} - Status: {} 완료", featureName, methodType, requestUri, status);
        } catch (Throwable throwable) {
            // 예외가 일어난 경우 응답 코드 500 등 매핑
            status = 500;
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletResponse response = attributes.getResponse();
                if (response != null) {
                    status = response.getStatus();
                    if (status == 200) {
                        status = 500;
                    }
                }
            }
            log.warn("[{}] {} {} - Status: {} 에러 발생: {}", featureName, methodType, requestUri, status, throwable.getMessage());
            throw throwable;
        }
        
        return result;
    }
}
