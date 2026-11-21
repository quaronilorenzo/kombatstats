package com.example.demo.post.costants;

public interface PostErrors {
    String postDuplicatedUri = "http://localhost:8080/errors/duplicate-post";
    String postDuplicatedMessage = "Post title already in use";

    String sortPropertyInvalidUri = "http://localhost:8080/errors/invalid-sort-property";
    String sortPropertyInvalidMessage = "Invalid sort property";

    String postNotFoundUri = "http://localhost:8080/errors/post-not-found";
    String postNotFoundMessage = "Post not found";

    String notPostAuthorUri = "http://localhost:8080/errors/not-post-author";
    String notPostAuthorMessage = "Only the author can modify or delete this post";
}
