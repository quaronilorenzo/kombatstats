package com.example.demo.post.controller;

import com.example.demo.post.costants.PostErrors;
import com.example.demo.testcontainers.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PostControllerIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;
    private static final String CURSOR_URL = "/posts/recentpostscursor";
    private static final String OFFSET_URL = "/posts/recentposts";
    private static final Instant BASE_TIME = Instant.parse("2026-01-01T10:00:00Z");

    // Same ordering the endpoints promise: createdAt DESC, then id DESC as tie-breaker.
    private static final Comparator<SeededPost> NEWEST_FIRST =
            Comparator.comparing(SeededPost::createdAt).thenComparing(SeededPost::id).reversed();

    private long authorId;

    private record SeededPost(long id, Instant createdAt) {}

    private record CursorResult(List<Long> ids, boolean hasNext, Instant nextCreatedAt, Long nextId) {}

    @BeforeEach
    void seedAuthor() {
        // AbstractIntegrationTest truncates users CASCADE, which empties post too (FK author_id).
        authorId = jdbcClient.sql("""
                        INSERT INTO users (id, first_name, last_name, email, birth_date, hash_password)
                        VALUES (nextval('users_seq'), 'Post', 'Author', :email, DATE '1999-04-23', 'not-a-real-hash')
                        RETURNING id
                        """)
                .param("email", "author-" + UUID.randomUUID() + "@example.com")
                .query(Long.class)
                .single();
    }

    // Inserted via JDBC so created_at is under test control (ties included), not left to @CreationTimestamp.
    private SeededPost insertPost(Instant createdAt) {
        long id = jdbcClient.sql("""
                        INSERT INTO post (author_id, title, content, created_at)
                        VALUES (:authorId, :title, 'content', :createdAt)
                        RETURNING id
                        """)
                .param("authorId", authorId)
                .param("title", "post " + UUID.randomUUID())
                .param("createdAt", OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC))
                .query(Long.class)
                .single();
        return new SeededPost(id, createdAt);
    }

    /** {@code count} posts, one second apart: the last inserted is the newest. */
    private List<SeededPost> insertPostsOneSecondApart(int count) {
        List<SeededPost> posts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            posts.add(insertPost(BASE_TIME.plusSeconds(i)));
        }
        return posts;
    }

    private List<SeededPost> insertPostsWithSameCreatedAt(int count) {
        List<SeededPost> posts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            posts.add(insertPost(BASE_TIME));
        }
        return posts;
    }

    private static List<Long> idsNewestFirst(List<SeededPost> posts) {
        return posts.stream().sorted(NEWEST_FIRST).map(SeededPost::id).toList();
    }

    private static List<Long> readIds(String json, String path) {
        List<Number> ids = JsonPath.read(json, path);
        return ids.stream().map(Number::longValue).toList();
    }

    @Nested
    @DisplayName("GET /posts/recentpostscursor")
    class CursorPagination {

        private ResultActions getCursorPage(Integer size, Instant lastCreatedAt, Long lastId) throws Exception {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(CURSOR_URL).with(asUser(authorId))
                    .accept(MediaType.APPLICATION_JSON);
            if (size != null) {
                request.param("size", size.toString());
            }
            if (lastCreatedAt != null) {
                request.param("lastCreatedAt", lastCreatedAt.toString());
            }
            if (lastId != null) {
                request.param("lastId", lastId.toString());
            }
            return mockMvc.perform(request);
        }

        private CursorResult fetch(Integer size, Instant lastCreatedAt, Long lastId) throws Exception {
            String json = getCursorPage(size, lastCreatedAt, lastId)
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn().getResponse().getContentAsString();

            boolean hasNext = JsonPath.read(json, "$.hasNext");
            Object cursor = JsonPath.read(json, "$.nextCursor");
            Instant nextCreatedAt = null;
            Long nextId = null;
            if (cursor != null) {
                nextCreatedAt = Instant.parse(JsonPath.read(json, "$.nextCursor.createdAt"));
                nextId = ((Number) JsonPath.read(json, "$.nextCursor.id")).longValue();
            }
            return new CursorResult(readIds(json, "$.items[*].id"), hasNext, nextCreatedAt, nextId);
        }

        /** Follows nextCursor until hasNext is false, checking the page contract at every step. */
        private List<CursorResult> walkAllPages(int size) throws Exception {
            List<CursorResult> pages = new ArrayList<>();
            CursorResult page = fetch(size, null, null);
            pages.add(page);
            while (page.hasNext()) {
                assertThat(page.ids()).as("a page with hasNext=true is full").hasSize(size);
                page = fetch(size, page.nextCreatedAt(), page.nextId());
                pages.add(page);
                assertThat(pages).as("pagination must terminate").hasSizeLessThan(1_000);
            }
            assertThat(page.nextCreatedAt()).as("last page nextCursor").isNull();
            assertThat(page.nextId()).as("last page nextCursor").isNull();
            return pages;
        }

        private static List<Long> flatten(List<CursorResult> pages) {
            return pages.stream().flatMap(p -> p.ids().stream()).toList();
        }

        @Test
        @DisplayName("200 - empty table: no items, hasNext false, nextCursor null")
        void shouldReturnEmptyPage_whenNoPosts() throws Exception {
            getCursorPage(null, null, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items", hasSize(0)))
                    .andExpect(jsonPath("$.hasNext").value(false))
                    .andExpect(jsonPath("$.nextCursor").isEmpty());
        }

        @Test
        @DisplayName("200 - no size param: default size is 10")
        void shouldUseDefaultSize10_whenSizeIsMissing() throws Exception {
            List<SeededPost> posts = insertPostsOneSecondApart(12);

            CursorResult page = fetch(null, null, null);

            assertThat(page.ids()).as("first page ids").isEqualTo(idsNewestFirst(posts).subList(0, 10));
            assertThat(page.hasNext()).isTrue();
        }

        @Test
        @DisplayName("200 - first page: newest first, nextCursor points at the last item")
        void shouldReturnNewestFirst_andCursorOfLastItem() throws Exception {
            List<SeededPost> posts = insertPostsOneSecondApart(5);
            List<SeededPost> expected = posts.stream().sorted(NEWEST_FIRST).toList();

            CursorResult page = fetch(3, null, null);

            assertThat(page.ids()).isEqualTo(expected.subList(0, 3).stream().map(SeededPost::id).toList());
            assertThat(page.hasNext()).isTrue();
            assertThat(page.nextId()).as("nextCursor.id").isEqualTo(expected.get(2).id());
            assertThat(page.nextCreatedAt()).as("nextCursor.createdAt").isEqualTo(expected.get(2).createdAt());
        }

        @ParameterizedTest(name = "size={0}")
        @ValueSource(ints = {1, 2, 3, 7, 10, 50})
        @DisplayName("200 - walking every page returns all posts once, in order, for any size")
        void shouldReturnEveryPostExactlyOnce_whenWalkingAllPages(int size) throws Exception {
            List<SeededPost> posts = insertPostsOneSecondApart(23);

            List<CursorResult> pages = walkAllPages(size);

            assertThat(flatten(pages)).isEqualTo(idsNewestFirst(posts));
            assertThat(pages).as("number of pages").hasSize((int) Math.ceil(23.0 / size));
        }

        @Test
        @DisplayName("200 - total is an exact multiple of size: last full page has hasNext false")
        void shouldNotReportNext_whenLastPageIsExactlyFull() throws Exception {
            List<SeededPost> posts = insertPostsOneSecondApart(4);

            CursorResult first = fetch(2, null, null);
            CursorResult second = fetch(2, first.nextCreatedAt(), first.nextId());

            assertThat(first.hasNext()).isTrue();
            assertThat(second.ids()).isEqualTo(idsNewestFirst(posts).subList(2, 4));
            assertThat(second.hasNext()).as("no empty extra page").isFalse();
            assertThat(second.nextId()).isNull();
        }

        @Test
        @DisplayName("200 - same createdAt on every post: id tie-breaker, no duplicates or gaps")
        void shouldUseIdAsTieBreaker_whenCreatedAtIsEqual() throws Exception {
            List<SeededPost> posts = insertPostsWithSameCreatedAt(7);

            List<Long> ids = flatten(walkAllPages(2));

            assertThat(ids).isEqualTo(idsNewestFirst(posts));
            assertThat(new HashSet<>(ids)).as("no duplicates").hasSize(posts.size());
        }

        @Test
        @DisplayName("200 - ties across page boundary mixed with distinct timestamps")
        void shouldKeepOrder_whenTiesSpanPageBoundary() throws Exception {
            List<SeededPost> posts = new ArrayList<>();
            posts.addAll(insertPostsOneSecondApart(3));
            for (int i = 0; i < 4; i++) {
                posts.add(insertPost(BASE_TIME.plusSeconds(10)));
            }
            posts.add(insertPost(BASE_TIME.plusSeconds(20)));

            assertThat(flatten(walkAllPages(3))).isEqualTo(idsNewestFirst(posts));
        }

        @Test
        @DisplayName("200 - size can change between requests: the cursor still continues correctly")
        void shouldContinueFromCursor_whenSizeChangesBetweenPages() throws Exception {
            List<Long> expected = idsNewestFirst(insertPostsOneSecondApart(10));

            CursorResult first = fetch(2, null, null);
            CursorResult second = fetch(5, first.nextCreatedAt(), first.nextId());
            CursorResult third = fetch(1, second.nextCreatedAt(), second.nextId());
            CursorResult rest = fetch(50, third.nextCreatedAt(), third.nextId());

            assertThat(first.ids()).hasSize(2);
            assertThat(second.ids()).hasSize(5);
            assertThat(third.ids()).hasSize(1);
            assertThat(rest.ids()).hasSize(2);
            assertThat(rest.hasNext()).isFalse();
            assertThat(flatten(List.of(first, second, third, rest))).isEqualTo(expected);
        }

        @Test
        @DisplayName("200 - post inserted after the first page does not shift the next page")
        void shouldBeStable_whenNewerPostIsInsertedWhilePaging() throws Exception {
            List<Long> expected = idsNewestFirst(insertPostsOneSecondApart(6));

            CursorResult first = fetch(3, null, null);
            insertPost(BASE_TIME.plusSeconds(100));
            CursorResult second = fetch(3, first.nextCreatedAt(), first.nextId());

            assertThat(second.ids()).as("unlike offset, no duplicate from the shift").isEqualTo(expected.subList(3, 6));
        }

        @Test
        @DisplayName("200 - incomplete cursor (only lastId): treated as first page")
        void shouldReturnFirstPage_whenOnlyLastIdIsGiven() throws Exception {
            List<Long> expected = idsNewestFirst(insertPostsOneSecondApart(5));

            CursorResult page = fetch(2, null, expected.get(1));

            assertThat(page.ids()).isEqualTo(expected.subList(0, 2));
        }

        @Test
        @DisplayName("200 - incomplete cursor (only lastCreatedAt): treated as first page")
        void shouldReturnFirstPage_whenOnlyLastCreatedAtIsGiven() throws Exception {
            List<Long> expected = idsNewestFirst(insertPostsOneSecondApart(5));

            CursorResult page = fetch(2, BASE_TIME.plusSeconds(2), null);

            assertThat(page.ids()).isEqualTo(expected.subList(0, 2));
        }

        @Test
        @DisplayName("200 - cursor older than every post: empty page")
        void shouldReturnEmptyPage_whenCursorIsPastTheEnd() throws Exception {
            insertPostsOneSecondApart(3);

            CursorResult page = fetch(10, BASE_TIME.minusSeconds(1), Long.MAX_VALUE);

            assertThat(page.ids()).isEmpty();
            assertThat(page.hasNext()).isFalse();
        }

        @Test
        @DisplayName("200 - size at the boundaries (1 and 50) is accepted")
        void shouldAcceptSizeBoundaries() throws Exception {
            insertPostsOneSecondApart(60);

            assertThat(fetch(1, null, null).ids()).hasSize(1);
            assertThat(fetch(50, null, null).ids()).hasSize(50);
        }

        @ParameterizedTest(name = "size={0}")
        @ValueSource(ints = {0, -1, 51, 1000})
        @DisplayName("400 - size outside 1..50")
        void shouldReturn400_whenSizeOutOfRange(int size) throws Exception {
            getCursorPage(size, null, null)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(PROBLEM_JSON));
        }

        @Test
        @DisplayName("400 - size is not a number")
        void shouldReturn400_whenSizeIsNotANumber() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(CURSOR_URL).with(asUser(authorId)).param("size", "abc"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 - lastCreatedAt is not an ISO-8601 instant")
        void shouldReturn400_whenLastCreatedAtIsMalformed() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(CURSOR_URL).with(asUser(authorId))
                            .param("lastCreatedAt", "yesterday")
                            .param("lastId", "1"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /posts/recentposts")
    class OffsetPagination {

        private ResultActions getOffsetPage(String... params) throws Exception {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(OFFSET_URL).with(asUser(authorId))
                    .accept(MediaType.APPLICATION_JSON);
            for (int i = 0; i < params.length; i += 2) {
                request.param(params[i], params[i + 1]);
            }
            return mockMvc.perform(request);
        }

        private String fetchJson(String... params) throws Exception {
            return getOffsetPage(params)
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
        }

        @Test
        @DisplayName("200 - empty table: empty content, totalElements 0")
        void shouldReturnEmptyPage_whenNoPosts() throws Exception {
            getOffsetPage()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0));
        }

        @Test
        @DisplayName("200 - no params: page 0, size 20, newest first")
        void shouldUseDefaults_whenNoParams() throws Exception {
            List<Long> expected = idsNewestFirst(insertPostsOneSecondApart(25));

            String json = fetchJson();

            assertThat(readIds(json, "$.content[*].id")).isEqualTo(expected.subList(0, 20));
            assertThat((Integer) JsonPath.read(json, "$.size")).isEqualTo(20);
            assertThat((Integer) JsonPath.read(json, "$.number")).isZero();
            assertThat((Integer) JsonPath.read(json, "$.totalElements")).isEqualTo(25);
            assertThat((Integer) JsonPath.read(json, "$.totalPages")).isEqualTo(2);
        }

        @Test
        @DisplayName("200 - page and size select the right slice and the totals follow size")
        void shouldReturnRequestedSlice_whenPageAndSizeAreGiven() throws Exception {
            List<Long> expected = idsNewestFirst(insertPostsOneSecondApart(11));

            String json = fetchJson("page", "1", "size", "3");

            assertThat(readIds(json, "$.content[*].id")).isEqualTo(expected.subList(3, 6));
            assertThat((Integer) JsonPath.read(json, "$.number")).isEqualTo(1);
            assertThat((Integer) JsonPath.read(json, "$.size")).isEqualTo(3);
            assertThat((Integer) JsonPath.read(json, "$.totalPages")).isEqualTo(4);
            assertThat((Boolean) JsonPath.read(json, "$.last")).isFalse();
        }

        @ParameterizedTest(name = "size={0}")
        @ValueSource(ints = {1, 4, 5, 11, 20})
        @DisplayName("200 - walking every page returns all posts once, in order, for any size")
        void shouldReturnEveryPostExactlyOnce_whenWalkingAllPages(int size) throws Exception {
            List<SeededPost> posts = new ArrayList<>(insertPostsOneSecondApart(6));
            posts.addAll(insertPostsWithSameCreatedAt(5));
            int expectedPages = (int) Math.ceil(posts.size() / (double) size);

            List<Long> ids = new ArrayList<>();
            for (int page = 0; page < expectedPages; page++) {
                String json = fetchJson("page", String.valueOf(page), "size", String.valueOf(size));
                ids.addAll(readIds(json, "$.content[*].id"));
                assertThat((Integer) JsonPath.read(json, "$.totalPages")).isEqualTo(expectedPages);
                assertThat((Boolean) JsonPath.read(json, "$.last")).isEqualTo(page == expectedPages - 1);
            }

            assertThat(ids).isEqualTo(idsNewestFirst(posts));
        }

        @Test
        @DisplayName("200 - page past the end: empty content, still 200")
        void shouldReturnEmptyContent_whenPageIsPastTheEnd() throws Exception {
            insertPostsOneSecondApart(3);

            getOffsetPage("page", "5", "size", "2")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(3));
        }

        @Test
        @DisplayName("200 - size=0 falls back to the @PageableDefault size (20)")
        void shouldFallBackToDefaultSize_whenSizeIsZero() throws Exception {
            insertPostsOneSecondApart(3);

            getOffsetPage("size", "0")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size").value(20));
        }

        @Test
        @DisplayName("200 - sort=createdAt,asc reverses the order")
        void shouldSortAscending_whenRequested() throws Exception {
            List<SeededPost> posts = insertPostsOneSecondApart(4);
            List<Long> oldestFirst = posts.stream().map(SeededPost::id).toList();

            String json = fetchJson("sort", "createdAt,asc");

            assertThat(readIds(json, "$.content[*].id")).isEqualTo(oldestFirst);
        }

        @Test
        @DisplayName("400 - sort on a property that does not exist")
        void shouldReturn400_whenSortPropertyIsUnknown() throws Exception {
            getOffsetPage("sort", "doesNotExist,desc")
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.title").value(PostErrors.sortPropertyInvalidMessage))
                    .andExpect(jsonPath("$.detail").value("unknown property: doesNotExist"))
                    .andExpect(jsonPath("$.type").value(PostErrors.sortPropertyInvalidUri));
        }
    }

    @Nested
    @DisplayName("POST /posts - author comes from the token")
    class AddPost {

        private ResultActions postAs(Long callerId, long bodyAuthorId) throws Exception {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"authorId": %d, "title": "post %s", "content": "content"}
                            """.formatted(bodyAuthorId, UUID.randomUUID()));
            if (callerId != null) {
                request.with(asUser(callerId));
            }
            return mockMvc.perform(request);
        }

        private List<Long> authorsOfAllPosts() {
            return jdbcClient.sql("SELECT author_id FROM post").query(Long.class).list();
        }

        @Test
        @DisplayName("201 - caller posts as themself")
        void shouldReturn201_whenCallerIsTheAuthor() throws Exception {
            postAs(authorId, authorId)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.authorId").value(authorId));

            assertThat(authorsOfAllPosts()).containsExactly(authorId);
        }

        @Test
        @DisplayName("never attributed to another user: either 201 as the caller, or 403")
        void shouldNotAttributePostToAnotherUser_whenBodyNamesSomeoneElse() throws Exception {
            long otherUserId = insertUser("victim-" + UUID.randomUUID() + "@example.com");

            int status = postAs(authorId, otherUserId).andReturn().getResponse().getStatus();

            assertThat(status).as("status when authorId in the body is someone else").isIn(201, 403);
            assertThat(authorsOfAllPosts()).as("authors in the post table").doesNotContain(otherUserId);
        }

        @Test
        @DisplayName("401 - no token: nothing is written")
        void shouldReturn401_whenNoToken() throws Exception {
            postAs(null, authorId).andExpect(status().isUnauthorized());

            assertThat(authorsOfAllPosts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("PATCH /posts/updatepost - only the author can update")
    class UpdatePost {

        private record StoredPost(long authorId, String title, String content) {}

        private ResultActions patchAs(Long callerId, String body) throws Exception {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders.patch("/posts/updatepost")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body);
            if (callerId != null) {
                request.with(asUser(callerId));
            }
            return mockMvc.perform(request);
        }

        private StoredPost stored(long postId) {
            return jdbcClient.sql("SELECT author_id, title, content FROM post WHERE id = :id")
                    .param("id", postId)
                    .query((rs, n) -> new StoredPost(rs.getLong("author_id"), rs.getString("title"), rs.getString("content")))
                    .single();
        }

        @Test
        @DisplayName("200 - author updates title and content")
        void shouldReturn200_andUpdate_whenCallerIsTheAuthor() throws Exception {
            long postId = insertPost(BASE_TIME).id();

            patchAs(authorId, """
                    {"id": %d, "title": "updated title", "content": "updated content"}
                    """.formatted(postId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(postId))
                    .andExpect(jsonPath("$.authorId").value(authorId))
                    .andExpect(jsonPath("$.title").value("updated title"))
                    .andExpect(jsonPath("$.content").value("updated content"));

            assertThat(stored(postId)).isEqualTo(new StoredPost(authorId, "updated title", "updated content"));
            assertThat(jdbcClient.sql("SELECT count(*) FROM post").query(Long.class).single())
                    .as("update, not insert").isEqualTo(1L);
        }

        @Test
        @DisplayName("200 - only title sent: content is left untouched")
        void shouldKeepContent_whenOnlyTitleIsSent() throws Exception {
            long postId = insertPost(BASE_TIME).id();

            patchAs(authorId, """
                    {"id": %d, "title": "only title"}
                    """.formatted(postId))
                    .andExpect(status().isOk());

            StoredPost after = stored(postId);
            assertThat(after.title()).isEqualTo("only title");
            assertThat(after.content()).isEqualTo("content");
        }

        @Test
        @DisplayName("200 - only content sent: title is left untouched")
        void shouldKeepTitle_whenOnlyContentIsSent() throws Exception {
            long postId = insertPost(BASE_TIME).id();
            String originalTitle = stored(postId).title();

            patchAs(authorId, """
                    {"id": %d, "content": "only content"}
                    """.formatted(postId))
                    .andExpect(status().isOk());

            StoredPost after = stored(postId);
            assertThat(after.title()).isEqualTo(originalTitle);
            assertThat(after.content()).isEqualTo("only content");
        }

        @Test
        @DisplayName("403 - caller is not the author: post unchanged")
        void shouldReturn403_whenCallerIsNotTheAuthor() throws Exception {
            long postId = insertPost(BASE_TIME).id();
            StoredPost before = stored(postId);
            long otherUserId = insertUser("intruder-" + UUID.randomUUID() + "@example.com");

            patchAs(otherUserId, """
                    {"id": %d, "title": "hacked", "content": "hacked"}
                    """.formatted(postId))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.title").value(PostErrors.notPostAuthorMessage))
                    .andExpect(jsonPath("$.type").value(PostErrors.notPostAuthorUri))
                    .andExpect(jsonPath("$.detail").value("you are not the author of post " + postId));

            assertThat(stored(postId)).isEqualTo(before);
        }

        @Test
        @DisplayName("404 - post does not exist")
        void shouldReturn404_whenPostDoesNotExist() throws Exception {
            patchAs(authorId, """
                    {"id": %d, "title": "t", "content": "c"}
                    """.formatted(Long.MAX_VALUE))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.title").value(PostErrors.postNotFoundMessage))
                    .andExpect(jsonPath("$.type").value(PostErrors.postNotFoundUri));
        }

        @Test
        @DisplayName("400 - id missing")
        void shouldReturn400_whenIdIsMissing() throws Exception {
            patchAs(authorId, """
                    {"title": "t", "content": "c"}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("id is mandatory"));
        }

        @ParameterizedTest(name = "title=\"{0}\"")
        @ValueSource(strings = {"", " ", "   "})
        @DisplayName("400 - title sent but blank: post unchanged")
        void shouldReturn400_whenTitleIsBlank(String title) throws Exception {
            long postId = insertPost(BASE_TIME).id();
            StoredPost before = stored(postId);

            patchAs(authorId, """
                    {"id": %d, "title": "%s"}
                    """.formatted(postId, title))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("title is invalid"));

            assertThat(stored(postId)).isEqualTo(before);
        }

        @ParameterizedTest(name = "content=\"{0}\"")
        @ValueSource(strings = {"", " ", "\\n\\t "})
        @DisplayName("400 - content sent but blank: post unchanged")
        void shouldReturn400_whenContentIsBlank(String blankContent) throws Exception {
            long postId = insertPost(BASE_TIME).id();
            StoredPost before = stored(postId);

            patchAs(authorId, """
                    {"id": %d, "content": "%s"}
                    """.formatted(postId, blankContent))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("content is invalid"));

            assertThat(stored(postId)).isEqualTo(before);
        }

        @Test
        @DisplayName("200 - multi-line content is accepted")
        void shouldAcceptMultilineContent() throws Exception {
            long postId = insertPost(BASE_TIME).id();

            patchAs(authorId, """
                    {"id": %d, "content": "line one\\nline two"}
                    """.formatted(postId))
                    .andExpect(status().isOk());

            assertThat(stored(postId).content()).isEqualTo("line one\nline two");
        }

        @Test
        @DisplayName("400 - title longer than 255 characters")
        void shouldReturn400_whenTitleIsTooLong() throws Exception {
            long postId = insertPost(BASE_TIME).id();

            patchAs(authorId, """
                    {"id": %d, "title": "%s"}
                    """.formatted(postId, "a".repeat(256)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("title is invalid"));
        }

        @Test
        @DisplayName("200 - title of exactly 255 characters is accepted")
        void shouldAcceptTitleOf255Characters() throws Exception {
            long postId = insertPost(BASE_TIME).id();
            String title = "a".repeat(255);

            patchAs(authorId, """
                    {"id": %d, "title": "%s"}
                    """.formatted(postId, title))
                    .andExpect(status().isOk());

            assertThat(stored(postId).title()).isEqualTo(title);
        }

        @Test
        @DisplayName("401 - no token: post unchanged")
        void shouldReturn401_whenNoToken() throws Exception {
            long postId = insertPost(BASE_TIME).id();
            StoredPost before = stored(postId);

            patchAs(null, """
                    {"id": %d, "title": "t", "content": "c"}
                    """.formatted(postId))
                    .andExpect(status().isUnauthorized());

            assertThat(stored(postId)).isEqualTo(before);
        }
    }
}
