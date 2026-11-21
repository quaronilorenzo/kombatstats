package com.example.demo.post.service;

import com.example.demo.post.dto.PostResponse;
import com.example.demo.post.dto.PostUpdateRequest;
import com.example.demo.post.dto.mapper.PostMapper;
import com.example.demo.post.entity.Post;
import com.example.demo.post.exceptions.NotPostAuthorException;
import com.example.demo.post.exceptions.PostNotFoundException;
import com.example.demo.post.repository.PostRepository;
import com.example.demo.user.entity.User;
import com.example.demo.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserService userService;
    @Mock
    private PostMapper postMapper;
    @InjectMocks
    private PostService postService;

    private static final long POST_ID = 10L;
    private static final long AUTHOR_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final String OLD_TITLE = "old title";
    private static final String OLD_CONTENT = "old content";

    private static Post existingPost() {
        User author = new User();
        author.setId(AUTHOR_ID);
        Post post = new Post(author, OLD_TITLE, OLD_CONTENT);
        post.setId(POST_ID);
        return post;
    }

    @Nested
    @DisplayName("updatePost(PostUpdateRequest, Long)")
    class UpdatePost {

        @Test
        @DisplayName("throws PostNotFoundException and saves nothing when the post does not exist")
        void shouldThrowPostNotFound_whenPostDoesNotExist() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.updatePost(
                    new PostUpdateRequest(POST_ID, "new title", "new content"), AUTHOR_ID))
                    .isInstanceOf(PostNotFoundException.class);

            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws NotPostAuthorException with the post id and saves nothing when the caller is not the author")
        void shouldThrowNotPostAuthor_whenCallerIsNotTheAuthor() {
            Post post = existingPost();
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.updatePost(
                    new PostUpdateRequest(POST_ID, "new title", "new content"), OTHER_USER_ID))
                    .isInstanceOfSatisfying(NotPostAuthorException.class,
                            ex -> assertThat(ex.getPostId()).isEqualTo(POST_ID));

            verify(postRepository, never()).save(any());
            assertThat(post.getTitle()).isEqualTo(OLD_TITLE);
            assertThat(post.getContent()).isEqualTo(OLD_CONTENT);
        }

        @Test
        @DisplayName("updates title and content of the loaded post and returns the mapped response")
        void shouldUpdateTitleAndContent_whenCallerIsTheAuthor() {
            Post post = existingPost();
            PostResponse response = new PostResponse(POST_ID, AUTHOR_ID, "new title", "new content");
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
            when(postMapper.postToPostResponse(any(Post.class))).thenReturn(response);

            PostResponse result = postService.updatePost(
                    new PostUpdateRequest(POST_ID, "new title", "new content"), AUTHOR_ID);

            ArgumentCaptor<Post> saved = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(saved.capture());
            assertThat(saved.getValue()).as("the loaded entity, not a new one").isSameAs(post);
            assertThat(saved.getValue().getId()).isEqualTo(POST_ID);
            assertThat(saved.getValue().getAuthor().getId()).isEqualTo(AUTHOR_ID);
            assertThat(saved.getValue().getTitle()).isEqualTo("new title");
            assertThat(saved.getValue().getContent()).isEqualTo("new content");
            assertThat(result).isSameAs(response);
        }

        @Test
        @DisplayName("PATCH semantics: only title sent, content is left untouched")
        void shouldKeepContent_whenOnlyTitleIsSent() {
            Post post = existingPost();
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            postService.updatePost(new PostUpdateRequest(POST_ID, "new title", null), AUTHOR_ID);

            assertThat(post.getTitle()).isEqualTo("new title");
            assertThat(post.getContent()).isEqualTo(OLD_CONTENT);
        }

        @Test
        @DisplayName("PATCH semantics: only content sent, title is left untouched")
        void shouldKeepTitle_whenOnlyContentIsSent() {
            Post post = existingPost();
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            postService.updatePost(new PostUpdateRequest(POST_ID, null, "new content"), AUTHOR_ID);

            assertThat(post.getTitle()).isEqualTo(OLD_TITLE);
            assertThat(post.getContent()).isEqualTo("new content");
        }

        @Test
        @DisplayName("PATCH semantics: no fields sent, the post stays as it was")
        void shouldChangeNothing_whenNoFieldsAreSent() {
            Post post = existingPost();
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            postService.updatePost(new PostUpdateRequest(POST_ID, null, null), AUTHOR_ID);

            assertThat(post.getTitle()).isEqualTo(OLD_TITLE);
            assertThat(post.getContent()).isEqualTo(OLD_CONTENT);
        }
    }
}
