package com.example.demo.post.costants;

public interface PostErrors {
    String postDuplicatedUri = "http://localhost:8080/errors/duplicate-post";
    String postDuplicatedMessage = "Post title already in use";

    String sortPropertyInvalidUri = "http://localhost:8080/errors/invalid-sort-property";
    String sortPropertyInvalidMessage = "Invalid sort property";
}
