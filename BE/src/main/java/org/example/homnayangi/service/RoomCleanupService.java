package org.example.homnayangi.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.homnayangi.enums.RoomStatus;
import org.example.homnayangi.repository.RoomPlaceRepository;
import org.example.homnayangi.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomCleanupService {

    final RoomRepository roomRepository;
    final RoomPlaceRepository roomPlaceRepository;

    @Value("${app.cleanup.room-retention-days:7}")
    int retentionDays;
    @Scheduled(cron = "${app.cleanup.cron:0 0 2 * * ?}")
    @Transactional
    public void cleanupOldRoomsAndDecks() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
        log.info("Bắt đầu tiến trình dọn dẹp định kỳ các phòng và bộ bài cũ trước ngày: {}", cutoffTime);

        // 1. Đóng các phòng bỏ dở quá hạn
        int closedCount = roomRepository.closeAbandonedRooms(RoomStatus.CLOSED, cutoffTime);
        if (closedCount > 0) {
            log.info("Đã tự động đóng {} phòng ăn bị bỏ quên quá {} ngày", closedCount, retentionDays);
        }

        // 2. Dọn dẹp danh sách bài (room_places) của các phòng quá hạn
        int deletedDecks = roomPlaceRepository.deleteOldRoomPlaces(cutoffTime);
        if (deletedDecks > 0) {
            log.info("Đã xóa {} bản ghi thẻ bài (room_places) của các phòng quá {} ngày", deletedDecks, retentionDays);
        }

        log.info("Hoàn tất tiến trình dọn dẹp định kỳ.");
    }
}
