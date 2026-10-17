package org.example.homnayangi.service;

import lombok.RequiredArgsConstructor;
import org.example.homnayangi.dto.request.ExpenseCreateRequest;
import org.example.homnayangi.dto.request.ExpenseItemRequest;
import org.example.homnayangi.dto.response.ExpenseResponse;
import org.example.homnayangi.dto.response.ExpenseSplitResponse;
import org.example.homnayangi.entity.*;
import org.example.homnayangi.enums.ExpenseStatus;
import org.example.homnayangi.enums.RoomStatus;
import org.example.homnayangi.enums.SplitType;
import org.example.homnayangi.exception.AppException;
import org.example.homnayangi.exception.ErrorCode;
import org.example.homnayangi.helper.RedisLockHelper;
import org.example.homnayangi.mapper.ExpenseMapper;
import org.example.homnayangi.mapper.UserMapper;
import org.example.homnayangi.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final RedisLockHelper redisLockHelper;
    private final PlatformTransactionManager transactionManager;

    // =========================================================================
    // 1. TẠO HÓA ĐƠN
    // =========================================================================

    /**
     * Tạo hóa đơn mới cho phòng đã CLOSED (được bảo vệ bởi Distributed Lock trên roomId).
     * Tự động tính toán và lưu ExpenseSplit theo kiểu chia được chọn.
     *
     * @param request Thông tin hóa đơn cần tạo
     * @return ExpenseResponse đầy đủ
     */
    public ExpenseResponse createExpense(ExpenseCreateRequest request) {
        return redisLockHelper.executeWithRoomLock(request.getRoomId(), () -> {
            return new TransactionTemplate(transactionManager).execute(status -> {
                return createExpenseInternal(request);
            });
        });
    }

    private ExpenseResponse createExpenseInternal(ExpenseCreateRequest request) {
        // 1. Kiểm tra phòng (Dùng row lock PESSIMISTIC_WRITE để chống race condition)
        Room room = roomRepository.findByIdForUpdate(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getStatus() != RoomStatus.CLOSED) {
            throw new AppException(ErrorCode.ROOM_NOT_CLOSED);
        }

        if (expenseRepository.existsByRoomId(room.getId())) {
            throw new AppException(ErrorCode.EXPENSE_ALREADY_EXISTS);
        }

        // 2. Kiểm tra Payer (Người trả tiền)
        User payer = userRepository.findById(request.getPayerId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Set<UUID> memberIds = roomMemberRepository.findAllByRoomId(room.getId())
                .stream()
                .map(rm -> rm.getUser().getId())
                .collect(Collectors.toSet());

        if (!memberIds.contains(payer.getId())) {
            throw new AppException(ErrorCode.PAYER_NOT_IN_ROOM);
        }

        // 3. Validate tổng tiền
        BigDecimal computedTotal = request.getItems().stream()
                .map(ExpenseItemRequest::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (computedTotal.compareTo(request.getTotalAmount()) != 0) {
            throw new AppException(ErrorCode.BILL_AMOUNT_MISMATCH);
        }

        // 4. Khởi tạo đối tượng Expense
        Expense expense = Expense.builder()
                .room(room)
                .payer(payer)
                .totalAmount(request.getTotalAmount())
                .splitType(request.getSplitType())
                .status(ExpenseStatus.UNPAID)
                .items(new ArrayList<>())
                .splits(new ArrayList<>())
                .build();

        // 5. Thêm các món ăn vào items
        for (ExpenseItemRequest itemReq : request.getItems()) {
            List<UUID> sharerIds = (itemReq.getSharerIds() == null || itemReq.getSharerIds().isEmpty())
                    ? new ArrayList<>(memberIds)
                    : itemReq.getSharerIds();

            for (UUID sharerId : sharerIds) {
                if (!memberIds.contains(sharerId)) {
                    throw new AppException(ErrorCode.SHARER_NOT_IN_ROOM);
                }
            }

            ExpenseItem item = ExpenseItem.builder()
                    .expense(expense)
                    .itemName(itemReq.getItemName())
                    .price(itemReq.getPrice())
                    .sharers(new ArrayList<>())
                    .build();

            for (UUID sharerId : sharerIds) {
                User sharerUser = userRepository.getReferenceById(sharerId);

                ItemSharerId sharerEmbeddedId = new ItemSharerId();
                sharerEmbeddedId.setUserId(sharerId);

                ItemSharer sharer = ItemSharer.builder()
                        .id(sharerEmbeddedId)
                        .user(sharerUser)
                        .expenseItem(item)
                        .build();

                item.getSharers().add(sharer);
            }

            expense.getItems().add(item);
        }

        // 6. Tính toán khoản nợ
        List<ExpenseSplit> splits = calculateSplits(expense, memberIds);
        expense.getSplits().addAll(splits);

        // 7. Lưu Expense (CascadeType.ALL tự động lưu toàn bộ items, sharers và splits)
        Expense savedExpense = expenseRepository.save(expense);

        return expenseMapper.toExpenseResponse(savedExpense);
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

        // Cập nhật trạng thái của hóa đơn
        updateExpenseStatus(expense);

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

        // 1. Kiểm tra quyền: Chỉ Payer mới có quyền xác nhận thanh toán toàn bộ
        if (!expense.getPayer().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.NOT_EXPENSE_PAYER);
        }

        // 2. Cập nhật tất cả khoản nợ (Splits) trực tiếp trên Managed Collection
        if (expense.getSplits() != null) {
            expense.getSplits().forEach(split -> split.setPaid(true));
        }

        // 3. Cập nhật trạng thái hóa đơn
        expense.setStatus(ExpenseStatus.FULLY_PAID);

        // 4. Lưu và trả về DTO
        Expense savedExpense = expenseRepository.save(expense);
        return expenseMapper.toExpenseResponse(savedExpense);
    }

    // =========================================================================
    // 4. XÓA HÓA ĐƠN
    // =========================================================================
    @Transactional
    public void deleteExpense(UUID expenseId) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Expense expense = expenseRepository.findByIdWithDetails(expenseId)
                .orElseThrow(() -> new AppException(ErrorCode.EXPENSE_NOT_FOUND));

        // 1. Kiểm tra quyền: Chỉ Payer hoặc Host của phòng mới được xóa
        boolean isPayer = expense.getPayer().getId().equals(currentUser.getId());
        boolean isHost = expense.getRoom().getHost().getId().equals(currentUser.getId());

        if (!isPayer && !isHost) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 2. Chỉ cho xóa khi chưa có ai thanh toán (UNPAID)
        if (expense.getStatus() != ExpenseStatus.UNPAID) {
            throw new AppException(ErrorCode.CANNOT_DELETE_PAID_EXPENSE);
        }

        expenseRepository.delete(expense);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private List<ExpenseSplit> calculateSplits(Expense expense, Set<UUID> memberIds) {
        Map<UUID, BigDecimal> splitAmounts;

        if (expense.getSplitType() == SplitType.EVENLY) {
            splitAmounts = calculateEvenSplit(expense.getTotalAmount(), memberIds);
        } else {
            splitAmounts = calculateByItemSplit(expense.getItems(), memberIds);
        }

        List<ExpenseSplit> splits = new ArrayList<>();
        UUID payerId = expense.getPayer().getId();

        for (Map.Entry<UUID, BigDecimal> entry : splitAmounts.entrySet()) {
            UUID userId = entry.getKey();
            BigDecimal amount = entry.getValue();

            User user = userRepository.getReferenceById(userId);

            ExpenseSplit split = ExpenseSplit.builder()
                    .id(new ExpenseSplitId(expense.getId(), userId))
                    .expense(expense)
                    .user(user)
                    .amountOwed(amount)
                    // Payer (người trả tiền) sẽ được tự động đánh dấu paid = true
                    .paid(userId.equals(payerId))
                    .build();

            splits.add(split);
        }

        return splits;
    }

    private void updateExpenseStatus(Expense expense) {
        List<ExpenseSplit> splits = expenseSplitRepository.findAllById_ExpenseId(expense.getId());

        long paidCount = splits.stream().filter(ExpenseSplit::isPaid).count();
        int totalCount = splits.size();

        if (paidCount == 0) {
            expense.setStatus(ExpenseStatus.UNPAID);
        } else if (paidCount == totalCount) {
            expense.setStatus(ExpenseStatus.FULLY_PAID);
        } else {
            expense.setStatus(ExpenseStatus.PARTIALLY_PAID);
        }

        expenseRepository.save(expense);
    }

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
                splitMap.put(memberId, baseAmount.add(remainder));
                firstAdded = true;
            } else {
                splitMap.put(memberId, baseAmount);
            }
        }

        return splitMap;
    }

    private Map<UUID, BigDecimal> calculateByItemSplit(
            List<ExpenseItem> items,
            Set<UUID> allMemberIds) {

        Map<UUID, BigDecimal> splitMap = new HashMap<>();
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
}
