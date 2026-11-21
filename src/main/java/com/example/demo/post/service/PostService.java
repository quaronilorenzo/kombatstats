package com.example.demo.post.service;

import com.example.demo.post.dto.PostCursor;
import com.example.demo.post.dto.PostRequest;
import com.example.demo.post.dto.PostResponse;
import com.example.demo.post.dto.PostUpdateRequest;
import com.example.demo.post.dto.mapper.PostMapper;
import com.example.demo.post.entity.Post;
import com.example.demo.post.exceptions.DuplicatedPostException;
import com.example.demo.post.exceptions.NotPostAuthorException;
import com.example.demo.post.exceptions.PostNotFoundException;
import com.example.demo.post.repository.PostRepository;
import com.example.demo.user.entity.User;
import com.example.demo.user.service.UserService;
import com.example.demo.utils.dto.CursorPage;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.data.domain.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    // authorId comes from the token (the controller's @AuthenticationPrincipal), never from the request body:
    // this service knows nothing about security, it just receives who the author is.
    @Transactional
    public PostResponse addPost(PostRequest postRequest, Long authorId){
        if (postRepository.existsByTitleIgnoreCase(postRequest.title())) {
            throw new DuplicatedPostException(postRequest.title());
        }

        // UserNotFoundException (404) if the user was deleted after the token was issued.
        User author = userService.getUserById(authorId);
        Post post = postMapper.postRequestToPost(postRequest);
        post.setAuthor(author);

        return postMapper.postToPostResponse(postRepository.save(post));
    }

    public CursorPage<PostResponse, PostCursor> findNextPosts(Instant lastCreatedAt, Long lastId, int size){
        Pageable pageable = PageRequest.of(0, size);
        Slice<Post> posts = (lastCreatedAt == null || lastId == null)
                ? postRepository.findFirstPage(pageable)
                : postRepository.findNextPage(lastCreatedAt, lastId, pageable);

        PostCursor nextCursor = null;
        if (posts.hasNext()) {
            Post last = posts.getContent().getLast();
            nextCursor = new PostCursor(last.getCreatedAt(), last.getId());
        }

        List<PostResponse> items = postMapper.postsToPostResponses(posts.getContent());
        return new CursorPage<>(items, nextCursor, posts.hasNext());
    }

    public Page<PostResponse> findAll(Pageable pageable){
        return postRepository.findAll(withStableSort(pageable))
                .map(postMapper::postToPostResponse);
    }

    @Transactional
    public PostResponse updatePost(PostUpdateRequest postUpdateRequest, Long authorId){
        Optional<Post> post = postRepository.findById(postUpdateRequest.id());
        if(post.isEmpty()){
            throw new PostNotFoundException("Post does not exists");
        } else if ( !Objects.equals(post.get().getAuthor().getId(), authorId)) {
            throw new NotPostAuthorException(post.get().getId());
        }
        if(postUpdateRequest.title() != null){
            post.get().setTitle(postUpdateRequest.title());
        }
        if(postUpdateRequest.content() != null){
            post.get().setContent(postUpdateRequest.content());
        }
        return postMapper.postToPostResponse(postRepository.save((post.get())));
    }

    @Transactional
    public void deletePost(Long postId, Long authorId){
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post " + postId + " does not exist"));
        if (!Objects.equals(post.getAuthor().getId(), authorId)) {
            throw new NotPostAuthorException(postId);
        }
        postRepository.delete(post);
    }

    private Pageable withStableSort(Pageable pageable){
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort()
                .and(Sort.by(Sort.Direction.DESC, "id")));
    }
}
