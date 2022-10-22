package com.example.account.service;

import com.example.account.dto.UseBalance;
import com.example.account.exception.AccountException;
import com.example.account.type.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class LockAopAspectTest {
    @Mock
    private LockService lockService;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    private LockAopAspect lockAopAspect;

    @Test
    void lockAndUnlock() throws Throwable {
        ArgumentCaptor<String> lockArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unlockArgumentCaptor = ArgumentCaptor.forClass(String.class);
        UseBalance.Request request = new UseBalance.Request(12L, "1000000000", 100000L);

        given(proceedingJoinPoint.proceed())
                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        assertThrows(AccountException.class,
            () -> lockAopAspect.aroundMethod(proceedingJoinPoint, request));

        verify(lockService, times(1)).lock(lockArgumentCaptor.capture());
        verify(lockService, times(1)).unlock(unlockArgumentCaptor.capture());

        assertEquals("1000000000", lockArgumentCaptor.getValue());
        assertEquals("1000000000", unlockArgumentCaptor.getValue());
    }
}
