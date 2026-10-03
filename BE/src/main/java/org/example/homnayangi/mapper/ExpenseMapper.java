package org.example.homnayangi.mapper;

import org.example.homnayangi.dto.response.ExpenseItemResponse;
import org.example.homnayangi.dto.response.ExpenseResponse;
import org.example.homnayangi.dto.response.ExpenseSplitResponse;
import org.example.homnayangi.entity.Expense;
import org.example.homnayangi.entity.ExpenseItem;
import org.example.homnayangi.entity.ExpenseSplit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {

    @Mapping(target = "expenseId", source = "id.expenseId")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "amountOwed", source = "amountOwed")
    @Mapping(target = "paid", source = "paid")
    ExpenseSplitResponse toExpenseSplitResponse(ExpenseSplit split);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "itemName", source = "itemName")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "sharerCount", expression = "java(item.getSharers().size())")
    @Mapping(target = "pricePerPerson", expression = "java(item.getSharers().isEmpty() ? item.getPrice() : item.getPrice().divide(java.math.BigDecimal.valueOf(item.getSharers().size()), 2, java.math.RoundingMode.HALF_UP))")
    ExpenseItemResponse toExpenseItemResponse(ExpenseItem item);

    @Mapping(target = "room", source = "room")
    @Mapping(target = "payer", source = "payer")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "splits", source = "splits")
    @Mapping(target = "splitType", expression = "java(expense.getSplitType() != null ? expense.getSplitType().name() : \"EVENLY\")")
    @Mapping(target = "fullyPaid", expression = "java(expense.getSplits().stream().allMatch(s -> s.isPaid()))")
    ExpenseResponse toExpenseResponse(Expense expense);
}
