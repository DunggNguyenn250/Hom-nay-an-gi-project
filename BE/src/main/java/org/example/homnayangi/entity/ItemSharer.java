package org.example.homnayangi.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "item_sharers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemSharer {

    @EmbeddedId
    ItemSharerId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("expenseItemId")
    @JoinColumn(name = "expense_item_id")
    ExpenseItem expenseItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    User user;
}
