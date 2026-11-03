package org.example.homnayangi.service;

import org.example.homnayangi.dto.response.ExpenseResponse;
import org.example.homnayangi.dto.response.ExpenseSplitResponse;
import org.example.homnayangi.entity.Expense;
import org.example.homnayangi.entity.ExpenseSplit;
import org.example.homnayangi.entity.ExpenseSplitId;
import org.example.homnayangi.entity.Room;
import org.example.homnayangi.entity.User;
import org.example.homnayangi.enums.ExpenseStatus;
import org.example.homnayangi.helper.RedisLockHelper;
import org.example.homnayangi.helper.VietQRHelper;
import org.example.homnayangi.mapper.ExpenseMapper;
import org.example.homnayangi.mapper.UserMapper;
import org.example.homnayangi.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {
    @Mock ExpenseRepository expenseRepository;
    @Mock ExpenseItemRepository expenseItemRepository;
    @Mock ExpenseSplitRepository expenseSplitRepository;
    @Mock ItemSharerRepository itemSharerRepository;
    @Mock RoomRepository roomRepository;
    @Mock RoomMemberRepository roomMemberRepository;
    @Mock UserRepository userRepository;
    @Spy ExpenseMapper expenseMapper = Mappers.getMapper(ExpenseMapper.class);
    @Spy UserMapper userMapper = Mappers.getMapper(UserMapper.class);
    @Mock RedisLockHelper redisLockHelper;
    @Mock VietQRHelper vietQRHelper;
    @Mock PlatformTransactionManager transactionManager;
    @Mock SocketService socketService;
    @InjectMocks ExpenseService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("payer", null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void singlePaymentBroadcastsFullExpenseWithUpdatedSplitsAndStatus(boolean lastPayment) {
        User payer = User.builder().id(UUID.randomUUID()).username("payer").build();
        User debtor = User.builder().id(UUID.randomUUID()).build();
        User otherDebtor = User.builder().id(UUID.randomUUID()).build();
        Room room = Room.builder().id(UUID.randomUUID()).roomCode("DINNER").build();
        Expense expense = Expense.builder().id(UUID.randomUUID()).room(room).payer(payer)
                .totalAmount(new BigDecimal("300000")).build();
        ExpenseSplit payerSplit = split(expense, payer, true);
        ExpenseSplit debtorSplit = split(expense, debtor, false);
        ExpenseSplit otherSplit = split(expense, otherDebtor, lastPayment);
        expense.setSplits(List.of(payerSplit, debtorSplit, otherSplit));

        when(userRepository.findByUsername("payer")).thenReturn(Optional.of(payer));
        when(userRepository.findById(debtor.getId())).thenReturn(Optional.of(debtor));
        when(expenseRepository.findByIdWithDetails(expense.getId())).thenReturn(Optional.of(expense));
        when(expenseSplitRepository.findById(debtorSplit.getId())).thenReturn(Optional.of(debtorSplit));
        when(expenseSplitRepository.markAsPaid(expense.getId(), debtor.getId())).thenReturn(1);
        when(expenseSplitRepository.findAllById_ExpenseId(expense.getId())).thenReturn(expense.getSplits());
        if (!lastPayment) {
            when(vietQRHelper.generateQrUrl(any(), any(), any(), any(), any())).thenReturn("remaining-payment-qr");
        }

        ExpenseSplitResponse result = service.markSplitAsPaid(expense.getId(), debtor.getId());

        assertTrue(result.isPaid());
        assertEquals(debtor.getId(), result.getUser().getId());
        ArgumentCaptor<ExpenseResponse> eventData = ArgumentCaptor.forClass(ExpenseResponse.class);
        verify(socketService).sendExpenseUpdated(eq(room.getId()), eventData.capture());
        ExpenseResponse broadcast = eventData.getValue();
        assertEquals(expense.getId(), broadcast.getId());
        assertEquals(lastPayment ? ExpenseStatus.FULLY_PAID : ExpenseStatus.PARTIALLY_PAID, broadcast.getStatus());
        assertEquals(lastPayment, broadcast.isFullyPaid());
        assertEquals(3, broadcast.getSplits().size());
        ExpenseSplitResponse paidSplit = broadcast.getSplits().stream()
                .filter(s -> s.getUser().getId().equals(debtor.getId())).findFirst().orElseThrow();
        assertTrue(paidSplit.isPaid());
        assertNull(paidSplit.getQrCodeUrl());
        ExpenseSplitResponse remainingSplit = broadcast.getSplits().stream()
                .filter(s -> s.getUser().getId().equals(otherDebtor.getId())).findFirst().orElseThrow();
        assertEquals(lastPayment, remainingSplit.isPaid());
        assertEquals(lastPayment ? null : "remaining-payment-qr", remainingSplit.getQrCodeUrl());
    }

    private ExpenseSplit split(Expense expense, User user, boolean paid) {
        return ExpenseSplit.builder().id(new ExpenseSplitId(expense.getId(), user.getId()))
                .expense(expense).user(user).amountOwed(new BigDecimal("100000")).paid(paid).build();
    }
}
