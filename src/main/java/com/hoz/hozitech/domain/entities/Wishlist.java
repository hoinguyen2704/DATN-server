package com.hoz.hozitech.domain.entities;

import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "wishlists", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_product_wishlist", columnNames = {"user_id", "product_id"})
})
public class Wishlist extends AbstractAuditingEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
