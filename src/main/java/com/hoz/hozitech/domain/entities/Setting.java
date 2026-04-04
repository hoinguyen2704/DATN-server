package com.hoz.hozitech.domain.entities;

import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@lombok.experimental.SuperBuilder
@Entity
@Table(name = "settings", indexes = {
        @Index(name = "idx_setting_key", columnList = "setting_key", unique = true),
        @Index(name = "idx_setting_group", columnList = "group_name")
})
public class Setting extends AbstractAuditingEntity {

    @Column(name = "group_name", nullable = false, length = 50)
    private String groupName;

    @Column(name = "setting_key", nullable = false, length = 100, unique = true)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    @Builder.Default
    @Column(name = "value_type", nullable = false, length = 20)
    private String valueType = "STRING"; // STRING, BOOLEAN, NUMBER

    @Column(name = "description")
    private String description;
}
