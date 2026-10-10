package com.example.demo.post.controller;

import com.example.demo.post.dto.PostRequest;
import com.example.demo.post.dto.PostResponse;
import com.example.demo.post.service.PostService;
import jakarta.validation.Valid;
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
    public List<PostResponse> findAll(){
        return postService.findAllPosts();
    }

    @PostMapping
    public ResponseEntity<PostResponse> addPost(@RequestBody @Valid PostRequest postRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.addPost(postRequest));
    }
}
