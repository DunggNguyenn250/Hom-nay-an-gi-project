package org.example.homnayangi.repository;

import org.example.homnayangi.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    /**
     * Lấy tất cả hóa đơn của một phòng, kèm theo splits và payer (tránh N+1).
     */
    @Query("""
            SELECT DISTINCT e FROM Expense e
            LEFT JOIN FETCH e.splits s
            LEFT JOIN FETCH s.user
            LEFT JOIN FETCH e.payer
            WHERE e.room.id = :roomId
            ORDER BY e.createdAt DESC
            """)
    List<Expense> findAllByRoomIdWithDetails(@Param("roomId") UUID roomId);

    /**
     * Lấy một hóa đơn theo id, kèm đầy đủ items, splits, sharers.
     */
    @Query("""
            SELECT e FROM Expense e
            LEFT JOIN FETCH e.payer
            LEFT JOIN FETCH e.splits s
            LEFT JOIN FETCH s.user
            WHERE e.id = :expenseId
            """)
    Optional<Expense> findByIdWithDetails(@Param("expenseId") UUID expenseId);

    /**
     * Kiểm tra phòng đã có hóa đơn chưa.
     */
    boolean existsByRoomId(UUID roomId);
}
