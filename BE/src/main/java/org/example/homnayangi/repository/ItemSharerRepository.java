package org.example.homnayangi.repository;

import org.example.homnayangi.entity.ItemSharer;
import org.example.homnayangi.entity.ItemSharerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ItemSharerRepository extends JpaRepository<ItemSharer, ItemSharerId> {

    /**
     * Lấy danh sách tất cả sharer của một expense_item.
     */
    List<ItemSharer> findAllById_ExpenseItemId(UUID expenseItemId);

    /**
     * Đếm số người chia sẻ một món (dùng để tính giá trên đầu người).
     */
    @Query("SELECT COUNT(s) FROM ItemSharer s WHERE s.id.expenseItemId = :itemId")
    long countByExpenseItemId(@Param("itemId") UUID itemId);
}
