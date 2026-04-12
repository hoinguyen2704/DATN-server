package com.hoz.hozitech.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import com.hoz.hozitech.domain.enums.ReturnRequestStatus;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "return_status_history", indexes = {
        @Index(name = "idx_return_history_return_id", columnList = "return_request_id")
})
public class ReturnStatusHistory extends AbstractAuditingEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReturnRequestStatus status;

    @Column(name = "description", length = 500)
    private String description;
}
