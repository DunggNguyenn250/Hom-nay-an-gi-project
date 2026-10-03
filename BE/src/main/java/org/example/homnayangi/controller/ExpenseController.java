package org.example.homnayangi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.homnayangi.dto.request.ExpenseCreateRequest;
import org.example.homnayangi.dto.response.ApiResponse;
import org.example.homnayangi.dto.response.ExpenseResponse;
import org.example.homnayangi.dto.response.ExpenseSplitResponse;
import org.example.homnayangi.service.ExpenseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    // =========================================================================
    // POST /api/v1/expenses
    // Tạo hóa đơn mới cho phòng đã CLOSED
    // =========================================================================
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExpenseResponse> createExpense(
            @Valid @RequestBody ExpenseCreateRequest request) {

        return ApiResponse.<ExpenseResponse>builder()
                .message("Tạo hóa đơn thành công")
                .result(expenseService.createExpense(request))
                .build();
    }

    // =========================================================================
    // GET /api/v1/expenses/{expenseId}
    // Lấy chi tiết một hóa đơn
    // =========================================================================
    @GetMapping("/{expenseId}")
    public ApiResponse<ExpenseResponse> getExpenseById(@PathVariable UUID expenseId) {
        return ApiResponse.<ExpenseResponse>builder()
                .result(expenseService.getExpenseById(expenseId))
                .build();
    }

    // =========================================================================
    // GET /api/v1/expenses/room/{roomId}
    // Lấy tất cả hóa đơn của một phòng
    // =========================================================================
    @GetMapping("/room/{roomId}")
    public ApiResponse<List<ExpenseResponse>> getExpensesByRoom(@PathVariable UUID roomId) {
        return ApiResponse.<List<ExpenseResponse>>builder()
                .result(expenseService.getExpensesByRoom(roomId))
                .build();
    }

    // =========================================================================
    // GET /api/v1/expenses/my-debts
    // Lấy tất cả khoản nợ chưa thanh toán của người dùng hiện tại
    // =========================================================================
    @GetMapping("/my-debts")
    public ApiResponse<List<ExpenseSplitResponse>> getMyUnpaidDebts() {
        return ApiResponse.<List<ExpenseSplitResponse>>builder()
                .message("Danh sách khoản nợ chưa thanh toán")
                .result(expenseService.getMyUnpaidSplits())
                .build();
    }

    // =========================================================================
    // PATCH /api/v1/expenses/{expenseId}/splits/{userId}/pay
    // Đánh dấu một khoản chia tiền là đã thanh toán
    // =========================================================================
    @PatchMapping("/{expenseId}/splits/{userId}/pay")
    public ApiResponse<ExpenseSplitResponse> markSplitAsPaid(
            @PathVariable UUID expenseId,
            @PathVariable UUID userId) {

        return ApiResponse.<ExpenseSplitResponse>builder()
                .message("Đã xác nhận thanh toán thành công")
                .result(expenseService.markSplitAsPaid(expenseId, userId))
                .build();
    }

    // =========================================================================
    // PATCH /api/v1/expenses/{expenseId}/pay-all
    // Đánh dấu toàn bộ hóa đơn là đã thanh toán (chỉ payer)
    // =========================================================================
    @PatchMapping("/{expenseId}/pay-all")
    public ApiResponse<ExpenseResponse> markAllSplitsAsPaid(@PathVariable UUID expenseId) {
        return ApiResponse.<ExpenseResponse>builder()
                .message("Đã xác nhận thanh toán toàn bộ hóa đơn")
                .result(expenseService.markAllSplitsAsPaid(expenseId))
                .build();
    }
}
