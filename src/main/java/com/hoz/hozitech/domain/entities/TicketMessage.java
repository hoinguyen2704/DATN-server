package com.hoz.hozitech.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ticket_messages", indexes = {
        @Index(name = "idx_ticket_message_ticket", columnList = "ticket_id")
})
public class TicketMessage extends AbstractAuditingEntity {

    @Column(name = "sender_type", nullable = false, length = 50)
    private String senderType; // USER, ADMIN, AI_BOT

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "attachments_json", columnDefinition = "jsonb")
    private String attachmentsJson;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;
}
