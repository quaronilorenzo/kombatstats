package com.example.demo.post.service;

import com.example.demo.post.dto.PostRequest;
import com.example.demo.post.dto.PostResponse;
import com.example.demo.post.dto.mapper.PostMapper;
import com.example.demo.post.entity.Post;
import com.example.demo.post.exceptions.DuplicatedPostException;
import com.example.demo.post.repository.PostRepository;
import com.example.demo.user.entity.User;
import com.example.demo.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserService userService;
    private final PostMapper postMapper;
    public PostService(PostRepository postRepository, UserService userService, PostMapper postMapper) {
        this.postRepository = postRepository;
        this.userService = userService;
        this.postMapper = postMapper;
    }

    public List<PostResponse> findByAuthorIdOrderByCreatedAtDesc(Long authorId){
        return postMapper.postsToPostResponses(postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId));
    }

    @Transactional
    public PostResponse addPost(PostRequest postRequest){
        if (postRepository.existsByTitleIgnoreCase(postRequest.title())) {
            throw new DuplicatedPostException(postRequest.title());
        }
        User author = userService.getUserById(postRequest.authorId());
        Post post = postMapper.postRequestToPost(postRequest);
        post.setAuthor(author);

        return postMapper.postToPostResponse(postRepository.save(post));
    }

    public Page<Post> findAllPosts(Pageable pageable){
        return postRepository.findAll(pageable);
        // return postMapper.postsToPostResponses(postRepository.findAll(Sort.by("createdAt").descending()));
    }
}
