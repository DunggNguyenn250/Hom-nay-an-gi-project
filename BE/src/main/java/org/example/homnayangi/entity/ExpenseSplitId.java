package org.example.homnayangi.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ExpenseSplitId implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID expenseId;
    private UUID userId;
}
