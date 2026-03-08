package com.hoz.hozitech.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_name"),
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "phone_number")
})
public class User extends AbstractAuditingEntity {

    @Column(name = "user_name", nullable = false, length = 50)
    @Size(min = 3, max = 50)
    private String userName;

    @JsonIgnore
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Email
    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();
}
