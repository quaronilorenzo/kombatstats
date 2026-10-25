package com.example.demo.post.service;

import com.example.demo.post.dto.PostCursor;
import com.example.demo.post.dto.PostRequest;
import com.example.demo.post.dto.PostResponse;
import com.example.demo.post.dto.mapper.PostMapper;
import com.example.demo.post.entity.Post;
import com.example.demo.post.exceptions.DuplicatedPostException;
import com.example.demo.post.repository.PostRepository;
import com.example.demo.user.entity.User;
import com.example.demo.user.service.UserService;
import com.example.demo.utils.dto.CursorPage;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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


    private Pageable withStableSort(Pageable pageable){
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort()
                .and(Sort.by(Sort.Direction.DESC, "id")));
    }
}
