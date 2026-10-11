package com.example.demo.post.controller;

import com.example.demo.post.dto.PostRequest;
import com.example.demo.post.dto.PostResponse;
import com.example.demo.post.entity.Post;
import com.example.demo.post.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("posts")
@RestController
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/postsbyauthor")
    public ResponseEntity<List<PostResponse>> findByAuthorIdOrderByCreatedAtDesc(@RequestParam Long authorId){
        return ResponseEntity.ok(postService.findByAuthorIdOrderByCreatedAtDesc(authorId));
    }
    @GetMapping("/recentposts")
    public ResponseEntity<Page<PostResponse>> findAll(@PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable){
        return postService.findAll(pageable);
        //return ResponseEntity.ok((Page) postService.findAllPosts(pageable));
    }

    @PostMapping
    public ResponseEntity<PostResponse> addPost(@RequestBody @Valid PostRequest postRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.addPost(postRequest));
    }
}
