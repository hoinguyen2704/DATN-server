package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandRepository extends JpaRepository<Brand, UUID> {
    Optional<Brand> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Page<Brand> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    @Query(
            value = """
                    select b
                    from Brand b
                    where exists (
                        select 1
                        from Product p
                        where p.brand = b
                          and p.category.id = :categoryId
                    )
                    """,
            countQuery = """
                    select count(b)
                    from Brand b
                    where exists (
                        select 1
                        from Product p
                        where p.brand = b
                          and p.category.id = :categoryId
                    )
                    """
    )
    Page<Brand> findByCategoryId(UUID categoryId, Pageable pageable);

    @Query(
            value = """
                    select b
                    from Brand b
                    where lower(b.name) like lower(concat('%', :keyword, '%'))
                      and exists (
                          select 1
                          from Product p
                          where p.brand = b
                            and p.category.id = :categoryId
                      )
                    """,
            countQuery = """
                    select count(b)
                    from Brand b
                    where lower(b.name) like lower(concat('%', :keyword, '%'))
                      and exists (
                          select 1
                          from Product p
                          where p.brand = b
                            and p.category.id = :categoryId
                      )
                    """
    )
    Page<Brand> findByKeywordAndCategoryId(String keyword, UUID categoryId, Pageable pageable);
}
