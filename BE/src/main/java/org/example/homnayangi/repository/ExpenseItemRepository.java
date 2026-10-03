package org.example.homnayangi.repository;

import org.example.homnayangi.entity.ExpenseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExpenseItemRepository extends JpaRepository<ExpenseItem, UUID> {

    /**
     * Lấy tất cả items của một expense, kèm sharers (tránh N+1).
     */
    @Query("""
            SELECT DISTINCT i FROM ExpenseItem i
            LEFT JOIN FETCH i.sharers sh
            LEFT JOIN FETCH sh.user
            WHERE i.expense.id = :expenseId
            """)
    List<ExpenseItem> findAllByExpenseIdWithSharers(@Param("expenseId") UUID expenseId);
}
