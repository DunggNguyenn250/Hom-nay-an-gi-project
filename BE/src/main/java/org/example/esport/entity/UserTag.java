package org.example.esport.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "user_tags")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserTag {

    @EmbeddedId
    UserTagId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    User user;

    @ManyToOne
    @MapsId("tagId")
    @JoinColumn(name = "tag_id")
    Tag tag;

    @Column(name = "is_temporary")
    @Builder.Default
    boolean temporary = false;
}
