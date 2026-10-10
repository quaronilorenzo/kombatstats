package com.example.demo.post.repository;

import com.example.demo.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface PostRepository extends PagingAndSortingRepository<Post, Long>, JpaRepository<Post, Long> {

    List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    boolean existsByTitleIgnoreCase(String title);
}

