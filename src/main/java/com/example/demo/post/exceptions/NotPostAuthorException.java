package com.example.demo.post.exceptions;

public class NotPostAuthorException extends RuntimeException {
    private final Long postId;

    public NotPostAuthorException(Long postId) {
        super("you are not the author of post " + postId);
        this.postId = postId;
    }

    public Long getPostId() {
        return postId;
    }
}
