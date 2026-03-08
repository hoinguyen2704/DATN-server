package com.hoz.hozitech.domain.entities;

import com.hoz.hozitech.domain.enums.RoleType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @Column(name = "id", length = 50)
    @Enumerated(EnumType.STRING)
    private RoleType id;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;
}
