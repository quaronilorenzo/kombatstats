package com.example.demo.post.dto.mapper;

import com.example.demo.post.dto.PostRequest;
import com.example.demo.post.dto.PostResponse;
import com.example.demo.post.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PostMapper {

    // author va caricato dal service tramite authorId: il mapper non accede al database.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    Post postRequestToPost(PostRequest postRequest);

    @Mapping(source = "author.id", target = "authorId")
    PostResponse postToPostResponse(Post post);

    List<PostResponse> postsToPostResponses(List<Post> posts);
}
