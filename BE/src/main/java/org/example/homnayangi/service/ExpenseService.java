package org.example.homnayangi.service;

import lombok.RequiredArgsConstructor;
import org.example.homnayangi.dto.request.ExpenseCreateRequest;
import org.example.homnayangi.dto.request.ExpenseItemRequest;
import org.example.homnayangi.dto.response.ExpenseResponse;
import org.example.homnayangi.dto.response.ExpenseSplitResponse;
import org.example.homnayangi.entity.*;
import org.example.homnayangi.enums.RoomStatus;
import org.example.homnayangi.enums.SplitType;
import org.example.homnayangi.exception.AppException;
import org.example.homnayangi.exception.ErrorCode;
import org.example.homnayangi.mapper.ExpenseMapper;
import org.example.homnayangi.mapper.UserMapper;
import org.example.homnayangi.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseItemRepository expenseItemRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final ItemSharerRepository itemSharerRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final ExpenseMapper expenseMapper;
    private final UserMapper userMapper;

    // =========================================================================
    // 1. TẠO HÓA ĐƠN
    // =========================================================================

    /**
     * Tạo hóa đơn mới cho phòng đã CLOSED.
     * Tự động tính toán và lưu ExpenseSplit theo kiểu chia được chọn.
     *
     * @param request Thông tin hóa đơn cần tạo
     * @return ExpenseResponse đầy đủ
     */
    @Transactional
    public ExpenseResponse createExpense(ExpenseCreateRequest request) {
        // --- Xác thực người dùng hiện tại ---
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // --- Kiểm tra phòng ---
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getStatus() != RoomStatus.CLOSED) {
            throw new AppException(ErrorCode.ROOM_NOT_CLOSED);
        }

        if (expenseRepository.existsByRoomId(room.getId())) {
            throw new AppException(ErrorCode.EXPENSE_ALREADY_EXISTS);
        }

        // --- Xác định người trả tiền (payer) ---
        User payer = userRepository.findById(request.getPayerId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // --- Lấy danh sách thành viên phòng ---
        List<RoomMember> roomMembers = roomMemberRepository.findAllById_RoomId(room.getId());
        Set<UUID> memberIds = roomMembers.stream()
                .map(m -> m.getId().getUserId())
                .collect(Collectors.toSet());

        // --- Kiểm tra payer phải là thành viên phòng ---
        if (!memberIds.contains(payer.getId())) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        // --- Xác minh tổng tiền ---
        BigDecimal computedTotal = request.getItems().stream()
                .map(ExpenseItemRequest::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (computedTotal.compareTo(request.getTotalAmount()) != 0) {
            throw new AppException(ErrorCode.BILL_AMOUNT_MISMATCH);
        }

        // --- Xác định kiểu chia trước khi tạo Expense ---
        SplitType splitType = request.getSplitType() != null ? request.getSplitType() : SplitType.EVENLY;

        // --- Tạo Expense ---
        Expense expense = Expense.builder()
                .room(room)
                .payer(payer)
                .totalAmount(request.getTotalAmount())
                .splitType(splitType)
                .build();
        expense = expenseRepository.save(expense);

        // --- Tạo ExpenseItems và ItemSharers ---
        List<ExpenseItem> savedItems = saveExpenseItems(expense, request.getItems(), memberIds);

        // --- Tính toán và tạo ExpenseSplits ---
        Map<UUID, BigDecimal> splitAmounts;

        if (splitType == SplitType.EVENLY) {
            splitAmounts = calculateEvenSplit(request.getTotalAmount(), memberIds);
        } else {
            splitAmounts = calculateByItemSplit(savedItems, memberIds);
        }

        saveSplits(expense, splitAmounts, memberIds);

        // --- Trả về kết quả ---
        Expense fullExpense = expenseRepository.findByIdWithDetails(expense.getId())
                .orElseThrow(() -> new AppException(ErrorCode.EXPENSE_NOT_FOUND));

        // Load items với sharers
        List<ExpenseItem> itemsWithSharers = expenseItemRepository.findAllByExpenseIdWithSharers(expense.getId());
        fullExpense.setItems(itemsWithSharers);

        return expenseMapper.toExpenseResponse(fullExpense);
    }

    // =========================================================================
    // 2. XEM HÓA ĐƠN
    // =========================================================================

    /**
     * Lấy chi tiết một hóa đơn theo ID.
     */
    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(UUID expenseId) {
        Expense expense = expenseRepository.findByIdWithDetails(expenseId)
                .orElseThrow(() -> new AppException(ErrorCode.EXPENSE_NOT_FOUND));

        List<ExpenseItem> itemsWithSharers = expenseItemRepository.findAllByExpenseIdWithSharers(expenseId);
        expense.setItems(itemsWithSharers);

        return expenseMapper.toExpenseResponse(expense);
    }

    /**
     * Lấy tất cả hóa đơn của một phòng.
     */
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByRoom(UUID roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }

        return expenseRepository.findAllByRoomIdWithDetails(roomId).stream()
                .map(expense -> {
                    List<ExpenseItem> items = expenseItemRepository.findAllByExpenseIdWithSharers(expense.getId());
                    expense.setItems(items);
                    return expenseMapper.toExpenseResponse(expense);
                })
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả các khoản nợ chưa thanh toán của người dùng hiện tại.
     */
    @Transactional(readOnly = true)
    public List<ExpenseSplitResponse> getMyUnpaidSplits() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return expenseSplitRepository.findUnpaidSplitsByUserId(currentUser.getId()).stream()
                .map(split -> ExpenseSplitResponse.builder()
                        .expenseId(split.getId().getExpenseId())
                        .user(userMapper.toUserResponse(split.getUser()))
                        .amountOwed(split.getAmountOwed())
                        .paid(split.isPaid())
                        .build())
                .collect(Collectors.toList());
    }

    // =========================================================================
    // 3. CẬP NHẬT TRẠNG THÁI THANH TOÁN
    // =========================================================================

    /**
     * Đánh dấu một khoản chia tiền là đã thanh toán.
     * Chỉ chính người nợ hoặc người trả tiền (payer) mới được phép.
     *
     * @param expenseId UUID của hóa đơn
     * @param userId    UUID của người đã thanh toán
     */
    @Transactional
    public ExpenseSplitResponse markSplitAsPaid(UUID expenseId, UUID userId) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Kiểm tra expense tồn tại
        Expense expense = expenseRepository.findByIdWithDetails(expenseId)
                .orElseThrow(() -> new AppException(ErrorCode.EXPENSE_NOT_FOUND));

        // Chỉ payer hoặc chính người nợ mới được mark paid
        boolean isPayer = expense.getPayer().getId().equals(currentUser.getId());
        boolean isSelf = currentUser.getId().equals(userId);
        if (!isPayer && !isSelf) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Kiểm tra split tồn tại
        ExpenseSplitId splitId = new ExpenseSplitId(expenseId, userId);
        ExpenseSplit split = expenseSplitRepository.findById(splitId)
                .orElseThrow(() -> new AppException(ErrorCode.SPLIT_NOT_FOUND));

        if (split.isPaid()) {
            throw new AppException(ErrorCode.ALREADY_PAID);
        }

        // Đánh dấu thanh toán
        int updated = expenseSplitRepository.markAsPaid(expenseId, userId);
        if (updated == 0) {
            throw new AppException(ErrorCode.SPLIT_NOT_FOUND);
        }

        // Lấy lại split đã cập nhật
        split.setPaid(true);

        User debtor = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return ExpenseSplitResponse.builder()
                .expenseId(expenseId)
                .user(userMapper.toUserResponse(debtor))
                .amountOwed(split.getAmountOwed())
                .paid(true)
                .build();
    }

    /**
     * Đánh dấu toàn bộ hóa đơn là đã thanh toán (tất cả các split).
     * Chỉ payer mới có quyền.
     */
    @Transactional
    public ExpenseResponse markAllSplitsAsPaid(UUID expenseId) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Expense expense = expenseRepository.findByIdWithDetails(expenseId)
                .orElseThrow(() -> new AppException(ErrorCode.EXPENSE_NOT_FOUND));

        if (!expense.getPayer().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.NOT_EXPENSE_PAYER);
        }

        // Đánh dấu tất cả splits là đã thanh toán
        List<ExpenseSplit> splits = expenseSplitRepository.findAllById_ExpenseId(expenseId);
        splits.forEach(s -> s.setPaid(true));
        expenseSplitRepository.saveAll(splits);

        List<ExpenseItem> itemsWithSharers = expenseItemRepository.findAllByExpenseIdWithSharers(expenseId);
        expense.setItems(itemsWithSharers);
        expense.setSplits(splits);

        return expenseMapper.toExpenseResponse(expense);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Lưu các ExpenseItem và ItemSharer vào database.
     */
    private List<ExpenseItem> saveExpenseItems(
            Expense expense,
            List<ExpenseItemRequest> itemRequests,
            Set<UUID> memberIds) {

        List<ExpenseItem> savedItems = new ArrayList<>();

        for (ExpenseItemRequest itemReq : itemRequests) {
            // Xác định danh sách sharers
            List<UUID> sharerIds = (itemReq.getSharerIds() == null || itemReq.getSharerIds().isEmpty())
                    ? new ArrayList<>(memberIds)  // Mặc định: tất cả thành viên
                    : itemReq.getSharerIds();

            // Kiểm tra tất cả sharer phải là thành viên phòng
            for (UUID sharerId : sharerIds) {
                if (!memberIds.contains(sharerId)) {
                    throw new AppException(ErrorCode.SHARER_NOT_IN_ROOM);
                }
            }

            // Tạo ExpenseItem
            ExpenseItem item = ExpenseItem.builder()
                    .expense(expense)
                    .itemName(itemReq.getItemName())
                    .price(itemReq.getPrice())
                    .build();
            item = expenseItemRepository.save(item);

            // Tạo ItemSharers
            List<ItemSharer> sharers = new ArrayList<>();
            for (UUID sharerId : sharerIds) {
                User sharerUser = userRepository.getReferenceById(sharerId);
                ItemSharer sharer = ItemSharer.builder()
                        .id(new ItemSharerId(item.getId(), sharerId))
                        .expenseItem(item)
                        .user(sharerUser)
                        .build();
                sharers.add(sharer);
            }
            itemSharerRepository.saveAll(sharers);
            item.setSharers(sharers);
            savedItems.add(item);
        }

        return savedItems;
    }

    /**
     * Tính toán chia đều: tổng hóa đơn / số thành viên.
     * Phần dư (do làm tròn) được cộng vào người đầu tiên trong danh sách.
     */
    private Map<UUID, BigDecimal> calculateEvenSplit(BigDecimal totalAmount, Set<UUID> memberIds) {
        int memberCount = memberIds.size();
        BigDecimal baseAmount = totalAmount.divide(
                BigDecimal.valueOf(memberCount), 2, RoundingMode.DOWN);

        BigDecimal distributed = baseAmount.multiply(BigDecimal.valueOf(memberCount));
        BigDecimal remainder = totalAmount.subtract(distributed);

        Map<UUID, BigDecimal> splitMap = new LinkedHashMap<>();
        boolean firstAdded = false;

        for (UUID memberId : memberIds) {
            if (!firstAdded) {
                // Người đầu tiên chịu phần dư
                splitMap.put(memberId, baseAmount.add(remainder));
                firstAdded = true;
            } else {
                splitMap.put(memberId, baseAmount);
            }
        }

        return splitMap;
    }

    /**
     * Tính toán chia theo món: cộng dồn phần tiền từ mỗi món mà người đó chia sẻ.
     * Các thành viên không tham gia bất kỳ món nào vẫn có split = 0 (ghi lại để theo dõi).
     */
    private Map<UUID, BigDecimal> calculateByItemSplit(
            List<ExpenseItem> items,
            Set<UUID> allMemberIds) {

        Map<UUID, BigDecimal> splitMap = new HashMap<>();
        // Khởi tạo 0 cho tất cả thành viên
        allMemberIds.forEach(id -> splitMap.put(id, BigDecimal.ZERO));

        for (ExpenseItem item : items) {
            List<ItemSharer> sharers = item.getSharers();
            if (sharers.isEmpty()) continue;

            BigDecimal pricePerPerson = item.getPrice().divide(
                    BigDecimal.valueOf(sharers.size()), 2, RoundingMode.HALF_UP);

            for (ItemSharer sharer : sharers) {
                UUID userId = sharer.getId().getUserId();
                splitMap.merge(userId, pricePerPerson, BigDecimal::add);
            }
        }

        return splitMap;
    }

    /**
     * Lưu danh sách ExpenseSplit vào database.
     */
    private void saveSplits(Expense expense, Map<UUID, BigDecimal> splitAmounts, Set<UUID> memberIds) {
        List<ExpenseSplit> splits = new ArrayList<>();

        for (Map.Entry<UUID, BigDecimal> entry : splitAmounts.entrySet()) {
            UUID userId = entry.getKey();
            BigDecimal amount = entry.getValue();

            User member = userRepository.getReferenceById(userId);

            ExpenseSplit split = ExpenseSplit.builder()
                    .id(new ExpenseSplitId(expense.getId(), userId))
                    .expense(expense)
                    .user(member)
                    .amountOwed(amount)
                    .paid(false)
                    .build();
            splits.add(split);
        }

        expenseSplitRepository.saveAll(splits);
    }
}
