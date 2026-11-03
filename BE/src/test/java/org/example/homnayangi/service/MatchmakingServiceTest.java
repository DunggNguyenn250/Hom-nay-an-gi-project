package org.example.homnayangi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.homnayangi.dto.MatchTicket;
import org.example.homnayangi.dto.request.MatchmakingRequest;
import org.example.homnayangi.dto.response.MatchmakingStatusResponse;
import org.example.homnayangi.entity.Room;
import org.example.homnayangi.entity.User;
import org.example.homnayangi.enums.MatchStatus;
import org.example.homnayangi.enums.RoomStatus;
import org.example.homnayangi.enums.RoomType;
import org.example.homnayangi.mapper.UserMapper;
import org.example.homnayangi.repository.RoomMemberRepository;
import org.example.homnayangi.repository.RoomRepository;
import org.example.homnayangi.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchmakingServiceTest {
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;
    @Mock SetOperations<String, String> setOperations;
    @Mock RoomRepository roomRepository;
    @Mock RoomMemberRepository roomMemberRepository;
    @Mock UserRepository userRepository;
    @Mock RoomService roomService;
    @Mock UserMapper userMapper;
    @Mock PlatformTransactionManager transactionManager;
    @Mock SocketService socketService;
    @Spy ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks MatchmakingService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service.init();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("radar-user", null));
        when(userRepository.findByUsername("radar-user"))
                .thenReturn(Optional.of(User.builder().id(userId).username("radar-user").build()));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void newSearchClearsOldResultBeforeFindingOrQueuing() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        MatchmakingStatusResponse response = service.findOrQueueMatch(MatchmakingRequest.builder().build());

        assertEquals(MatchStatus.WAITING, response.getStatus());
        assertNull(response.getRoomId());
        var order = inOrder(redisTemplate, roomRepository, valueOperations);
        order.verify(redisTemplate).delete(MatchmakingService.RESULT_PREFIX + userId);
        order.verify(roomRepository).findByStatusAndRoomType(RoomStatus.OPEN, RoomType.PUBLIC);
        order.verify(valueOperations).set(eq(MatchmakingService.TICKET_PREFIX + userId),
                anyString(), eq(MatchmakingService.TICKET_TTL));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void staleRoomResultFallsBackToActiveQueue(boolean closed) throws Exception {
        cacheMatchedResult();
        stubStaleRoom(closed);
        MatchTicket ticket = MatchTicket.builder().userId(userId).radius(5.0).targetGroupSize(2).build();
        String ticketJson = objectMapper.writeValueAsString(ticket);
        when(valueOperations.get(MatchmakingService.TICKET_PREFIX + userId))
                .thenReturn(ticketJson);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size(MatchmakingService.QUEUE_KEY)).thenReturn(1L);

        MatchmakingStatusResponse response = service.getMatchStatus();

        assertEquals(MatchStatus.WAITING, response.getStatus());
        assertNull(response.getRoomId());
        assertEquals(2, response.getTargetGroupSize());
        assertEquals(1, response.getCurrentQueueSize());
        verify(redisTemplate).delete(MatchmakingService.RESULT_PREFIX + userId);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void staleRoomResultWithoutTicketReturnsTimeout(boolean closed) throws Exception {
        cacheMatchedResult();
        stubStaleRoom(closed);

        MatchmakingStatusResponse response = service.getMatchStatus();

        assertEquals(MatchStatus.TIMEOUT, response.getStatus());
        assertNull(response.getRoomId());
        verify(redisTemplate).delete(MatchmakingService.RESULT_PREFIX + userId);
        verify(valueOperations).get(MatchmakingService.TICKET_PREFIX + userId);
    }

    @ParameterizedTest
    @EnumSource(value = RoomStatus.class, names = {"OPEN", "SWIPING"})
    void activeRoomKeepsCachedMatch(RoomStatus status) throws Exception {
        cacheMatchedResult();
        when(roomRepository.findById(roomId))
                .thenReturn(Optional.of(Room.builder().id(roomId).status(status).build()));

        MatchmakingStatusResponse response = service.getMatchStatus();

        assertEquals(MatchStatus.MATCHED, response.getStatus());
        assertEquals(roomId, response.getRoomId());
        verify(redisTemplate, never()).delete(anyString());
        verify(valueOperations, never()).get(MatchmakingService.TICKET_PREFIX + userId);
    }

    private void cacheMatchedResult() throws Exception {
        MatchmakingStatusResponse cachedResult = MatchmakingStatusResponse.builder()
                .status(MatchStatus.MATCHED).roomId(roomId).build();
        String resultJson = objectMapper.writeValueAsString(cachedResult);
        when(valueOperations.get(MatchmakingService.RESULT_PREFIX + userId))
                .thenReturn(resultJson);
    }

    private void stubStaleRoom(boolean closed) {
        when(roomRepository.findById(roomId)).thenReturn(closed
                ? Optional.of(Room.builder().id(roomId).status(RoomStatus.CLOSED).build())
                : Optional.empty());
    }
}
