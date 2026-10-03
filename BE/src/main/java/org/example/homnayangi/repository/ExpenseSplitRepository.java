package org.example.homnayangi.repository;

import org.example.homnayangi.entity.ExpenseSplit;
import org.example.homnayangi.entity.ExpenseSplitId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, ExpenseSplitId> {

    /**
     * Lấy tất cả các phần chia của một hóa đơn.
     */
    List<ExpenseSplit> findAllById_ExpenseId(UUID expenseId);

    /**
     * Lấy tất cả các khoản nợ của một user trên mọi hóa đơn (chưa thanh toán).
     */
    @Query("""
            SELECT s FROM ExpenseSplit s
            JOIN FETCH s.expense e
            JOIN FETCH e.room
            JOIN FETCH e.payer
            WHERE s.user.id = :userId
            AND s.paid = false
            ORDER BY e.createdAt DESC
            """)
    List<ExpenseSplit> findUnpaidSplitsByUserId(@Param("userId") UUID userId);

    /**
     * Đánh dấu thanh toán cho một split cụ thể.
     */
    @Modifying
    @Query("""
            UPDATE ExpenseSplit s
            SET s.paid = true
            WHERE s.id.expenseId = :expenseId
            AND s.id.userId = :userId
            """)
    int markAsPaid(@Param("expenseId") UUID expenseId, @Param("userId") UUID userId);

    /**
     * Kiểm tra tất cả các split trong một hóa đơn đã thanh toán hết chưa.
     */
    @Query("""
            SELECT COUNT(s) = 0 FROM ExpenseSplit s
            WHERE s.expense.id = :expenseId AND s.paid = false
            """)
    boolean allSplitsPaid(@Param("expenseId") UUID expenseId);
}
