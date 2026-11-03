package org.example.homnayangi.service;

import org.example.homnayangi.dto.response.ExpenseResponse;
import org.example.homnayangi.dto.response.SocketEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocketServiceTest {
    @Mock SimpMessagingTemplate messagingTemplate;
    @Captor ArgumentCaptor<SocketEvent<?>> eventCaptor;

    private SocketService service;
    private TransactionTemplate transactionTemplate;
    private final UUID roomId = UUID.randomUUID();
    private final UUID expenseId = UUID.randomUUID();
    private final ExpenseResponse expense = ExpenseResponse.builder().id(expenseId).build();

    @BeforeEach
    void setUp() {
        service = new SocketService(messagingTemplate);
        transactionTemplate = new TransactionTemplate(new TestTransactionManager());
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXPENSE_CREATED", "EXPENSE_UPDATED", "EXPENSE_DELETED"})
    void expenseEventUsesRoomTopicAndExpectedPayload(String type) {
        sendExpenseEvent(type);
        assertExpenseEvent(type);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXPENSE_CREATED", "EXPENSE_UPDATED", "EXPENSE_DELETED"})
    void expenseEventIsSentOnlyAfterCommit(String type) {
        transactionTemplate.executeWithoutResult(status -> {
            sendExpenseEvent(type);
            verifyNoInteractions(messagingTemplate);
        });

        assertExpenseEvent(type);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXPENSE_CREATED", "EXPENSE_UPDATED", "EXPENSE_DELETED"})
    void rolledBackExpenseDoesNotSendEvent(String type) {
        transactionTemplate.executeWithoutResult(status -> {
            sendExpenseEvent(type);
            status.setRollbackOnly();
        });

        verifyNoInteractions(messagingTemplate);
    }

    private void sendExpenseEvent(String type) {
        switch (type) {
            case "EXPENSE_CREATED" -> service.sendExpenseCreated(roomId, expense);
            case "EXPENSE_UPDATED" -> service.sendExpenseUpdated(roomId, expense);
            case "EXPENSE_DELETED" -> service.sendExpenseDeleted(roomId, expenseId);
            default -> throw new IllegalArgumentException(type);
        }
    }

    private void assertExpenseEvent(String type) {
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/" + roomId), eventCaptor.capture());
        SocketEvent<?> event = eventCaptor.getValue();
        assertEquals(type, event.getType());
        assertEquals(roomId, event.getRoomId());
        assertNotNull(event.getTimestamp());
        if (type.equals("EXPENSE_DELETED")) {
            assertEquals(Map.of("expenseId", expenseId.toString()), event.getData());
        } else {
            assertSame(expense, event.getData());
        }
    }

    // Exercise Spring's commit/rollback callbacks without an external database.
    private static class TestTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() { return new Object(); }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) { }

        @Override
        protected void doCommit(DefaultTransactionStatus status) { }

        @Override
        protected void doRollback(DefaultTransactionStatus status) { }
    }
}
