package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.repository.AccountRepository;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccountSuccess() {
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("Pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(
                        Account.builder()
                                .accountUser(accountUser)
                                .accountNumber("1000000012")
                                .build()));

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1000000013")
                        .build());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        AccountDto accountDto = accountService.createAccount(1L, 100L);

        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000013", accountDto.getAccountNumber());
    }

    @Test
    void createFirstAccount() {
        AccountUser accountUser = AccountUser.builder()
                .id(15L)
                .name("Pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1000000015")
                        .build());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        AccountDto accountDto = accountService.createAccount(1L, 100L);

        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(15L, accountDto.getUserId());
        assertEquals("1000000000", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 생성 실패")
    void createAccount_userNotFound() {
        AccountUser accountUser = AccountUser.builder()
                .id(15L)
                .name("Pobi")
                .build();

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        AccountException accountException = assertThrows(AccountException.class,
                                                () -> accountService.createAccount(1L, 100L));

        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("유저당 계좌는 10개")
    void createAccount_maxAccountIs10() {
        AccountUser accountUser = AccountUser.builder()
                .id(15L)
                .name("Pobi")
                .build();

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(accountUser));
        given(accountRepository.countByAccountUser(any())).willReturn(10);

        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, accountException.getErrorCode());
    }

    @Test
    void deleteAccountSuccess() {
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("Pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(
                        Account.builder()
                                .accountUser(accountUser)
                                .accountNumber("1000000012")
                                .balance(0L)
                                .build()));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        AccountDto accountDto = accountService.deleteAccount(1L, "1000000012");

        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000012", accountDto.getAccountNumber());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 해지 실패")
    void deleteAccount_userNotFound() {
        given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());

        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 계좌 해지 실패")
    void deleteAccount_accountNotFound() {
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("Pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름")
    void deleteAccount_userUnMatch() {
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("Pobi")
                .build();
        AccountUser accountOtherUser = AccountUser.builder()
                .id(13L)
                .name("Harry")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(
                        Account.builder()
                                .accountUser(accountOtherUser)
                                .accountNumber("1000000012")
                                .balance(0L)
                                .build()));

        AccountException accountException =  assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1000000012"));
        assertEquals(ErrorCode.USER_ACCOUNT_UNMATCH, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액이 남은 경우")
    void deleteAccount_balanceNotEmpty() {
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("Pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(
                        Account.builder()
                                .accountUser(accountUser)
                                .accountNumber("1000000012")
                                .balance(100L)
                                .build()));

        AccountException accountException =  assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1000000012"));
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, accountException.getErrorCode());
    }

    @Test
    @DisplayName("이미 해지된 경우")
    void deleteAccount_alreadyUnregistered() {
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("Pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(
                        Account.builder()
                                .accountUser(accountUser)
                                .accountNumber("1000000012")
                                .balance(0L)
                                .accountStatus(AccountStatus.UNREGISTERED)
                                .build()));

        AccountException accountException =  assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1000000012"));
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, accountException.getErrorCode());
    }

    @Test
    void successGetAccountsByUserId() {
        AccountUser accountUser = AccountUser.builder()
                .id(12L)
                .name("Pobi")
                .build();
        List<Account> accountsList =Arrays.asList(
                Account.builder().accountUser(accountUser).accountNumber("1234567890").balance(1000L).build(),
                Account.builder().accountUser(accountUser).accountNumber("1234567891").balance(2000L).build(),
                Account.builder().accountUser(accountUser).accountNumber("1234567892").balance(3000L).build()
        );

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountUser(any()))
                .willReturn(accountsList);

        List<AccountDto> result = accountService.getAccountByUserId(1L);

        assertEquals(3, result.size());
        assertEquals("1234567890", result.get(0).getAccountNumber());
        assertEquals(1000L, result.get(0).getBalance());
        assertEquals("1234567891", result.get(1).getAccountNumber());
        assertEquals(2000L, result.get(1).getBalance());
        assertEquals("1234567892", result.get(2).getAccountNumber());
        assertEquals(3000L, result.get(2).getBalance());
    }

    @Test
    void failedGetAccountsByUserId() {
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        AccountException accountException =
                assertThrows(AccountException.class, () -> accountService.getAccountByUserId(1L));

        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }
}

