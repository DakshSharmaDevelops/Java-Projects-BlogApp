package com.blog.controller;

import com.blog.dto.PageResponse;
import com.blog.dto.PostRequest;
import com.blog.dto.PostResponse;
import com.blog.entity.Post;
import com.blog.entity.User;
import com.blog.service.PostService;
import com.blog.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostRestController {
    private final PostService postService;
    private final UserAccountService userAccountService;

    public PostRestController(PostService postService, UserAccountService userAccountService) {
        this.postService = postService;
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public PageResponse<PostResponse> getPosts(
            @RequestParam(defaultValue = "1") int start,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) List<String> author,
            @RequestParam(required = false) List<Long> tagId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "publishedAt") String sortField,
            @RequestParam(defaultValue = "desc") String order) {
        Page<PostResponse> posts = postService
                .getFilteredPosts(start, limit, author, tagId, search, sortField, order)
                .map(PostResponse::from);
        return PageResponse.from(posts);
    }

    @GetMapping("/{id}")
    public PostResponse getPost(@PathVariable Long id) {
        return PostResponse.from(postService.getPostById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(@Valid @RequestBody PostRequest request,
                                   Authentication authentication) {
        User user = userAccountService.findByEmail(authentication.getName());
        Post post = toPost(request);
        post.setAuthor(user.getName());
        post.setAuthorUser(user);
        return PostResponse.from(postService.savePost(post, request.rawTags()));
    }

    @PutMapping("/{id}")
    public PostResponse updatePost(@PathVariable Long id,
                                   @Valid @RequestBody PostRequest request,
                                   Authentication authentication) {
        Post post = postService.getPostById(id);
        ensureOwner(post, authentication);
        post.setTitle(request.title());
        post.setExcerpt(request.excerpt());
        post.setContent(request.content());
        post.setIsPublished(request.published() == null || request.published());
        return PostResponse.from(postService.savePost(post, request.rawTags()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Long id, Authentication authentication) {
        Post post = postService.getPostById(id);
        ensureOwner(post, authentication);
        postService.deletePost(id);
    }

    private Post toPost(PostRequest request) {
        return Post.builder()
                .title(request.title())
                .excerpt(request.excerpt())
                .content(request.content())
                .isPublished(request.published() == null || request.published())
                .build();
    }

    private void ensureOwner(Post post, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        boolean isOwner = post.getAuthorUser() != null
                && post.getAuthorUser().getEmail().equalsIgnoreCase(authentication.getName());
        if (!isAdmin && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only modify your own posts.");
        }
    }
}
