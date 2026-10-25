package com.example.demo.post.repository;

import com.example.demo.post.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PostRepository extends PagingAndSortingRepository<Post, Long>, JpaRepository<Post, Long> {

    List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    boolean existsByTitleIgnoreCase(String title);
    // Prima pagina: nessun cursore. Separata dalla successiva perché ":param IS NULL" con un
    // parametro null fa fallire Postgres ("could not determine data type of parameter $1").
    @Query("""
        SELECT p FROM Post p
        ORDER BY p.createdAt DESC, p.id DESC
        """)
    Slice<Post> findFirstPage(Pageable pageable);

    @Query("""
        SELECT p FROM Post p
        WHERE p.createdAt < :lastCreatedAt
           OR (p.createdAt = :lastCreatedAt AND p.id < :lastId)
        ORDER BY p.createdAt DESC, p.id DESC
        """)
    Slice<Post> findNextPage(@Param("lastCreatedAt") Instant lastCreatedAt,
                             @Param("lastId") Long lastId,
                             Pageable pageable);
}

