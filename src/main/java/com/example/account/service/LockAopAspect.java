package com.example.account.service;

import com.example.account.aop.AccountLockIdInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LockAopAspect {
    private final LockService lockService;

    // 여기서 request는 AccountLock Annotation이 걸린 Method의 Argument이름.
    @Around("@annotation(com.example.account.aop.AccountLock) && args(request)")
    public Object aroundMethod(ProceedingJoinPoint pjp, AccountLockIdInterface request) throws Throwable {
        String accountNumber = request.getAccountNumber();
        lockService.lock(accountNumber);
        try {
            return pjp.proceed();
        } finally {
            lockService.unlock(accountNumber);
        }
    }
}
