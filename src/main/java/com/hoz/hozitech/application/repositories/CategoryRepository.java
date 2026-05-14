package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Category> findByStatusTrue();

    Page<Category> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    @Query(
            value = """
                    select c
                    from Category c
                    where exists (
                        select 1
                        from Product p
                        where p.category = c
                          and p.brand.id = :brandId
                    )
                    """,
            countQuery = """
                    select count(c)
                    from Category c
                    where exists (
                        select 1
                        from Product p
                        where p.category = c
                          and p.brand.id = :brandId
                    )
                    """
    )
    Page<Category> findByBrandId(UUID brandId, Pageable pageable);

    @Query(
            value = """
                    select c
                    from Category c
                    where lower(c.name) like lower(concat('%', :keyword, '%'))
                      and exists (
                          select 1
                          from Product p
                          where p.category = c
                            and p.brand.id = :brandId
                      )
                    """,
            countQuery = """
                    select count(c)
                    from Category c
                    where lower(c.name) like lower(concat('%', :keyword, '%'))
                      and exists (
                          select 1
                          from Product p
                          where p.category = c
                            and p.brand.id = :brandId
                      )
                    """
    )
    Page<Category> findByKeywordAndBrandId(String keyword, UUID brandId, Pageable pageable);

    @Query("""
            select c.id, count(p.id)
            from Product p
            join p.category c
            where c.id in :categoryIds
            group by c.id
            """)
    List<Object[]> countProductsByCategoryIds(@Param("categoryIds") Collection<UUID> categoryIds);

    @Query("""
            select c.id, count(csa.id)
            from CategorySpecAttribute csa
            join csa.category c
            where c.id in :categoryIds
            group by c.id
            """)
    List<Object[]> countSpecAttributesByCategoryIds(@Param("categoryIds") Collection<UUID> categoryIds);
}
