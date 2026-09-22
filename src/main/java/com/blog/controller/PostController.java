package com.blog.controller;

import com.blog.entity.Comment;
import com.blog.entity.Post;
import com.blog.entity.Tag;
import com.blog.entity.User;
import com.blog.service.OpenAiService;
import com.blog.service.PostService;
import com.blog.service.TagService;
import com.blog.service.UserAccountService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/")
public class PostController {
    private final PostService postService;
    private final TagService tagService;
    private final OpenAiService openAiService;
    private final UserAccountService userAccountService;

    public PostController(PostService postService, TagService tagService, OpenAiService openAiService,
                          UserAccountService userAccountService) {
        this.postService = postService;
        this.tagService = tagService;
        this.openAiService = openAiService;
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public String index(
            @RequestParam(defaultValue = "1") int start,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) List<String> author,
            @RequestParam(required = false) List<Long> tagId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "publishedAt") String sortField,
            @RequestParam(defaultValue = "desc") String order,
            Authentication authentication,
            Model model) {

        Page<Post> pagePosts = postService.getFilteredPosts(start, limit, author, tagId, search, sortField, order);

        model.addAttribute("posts", pagePosts.getContent());
        model.addAttribute("totalElements", pagePosts.getTotalElements());
        model.addAttribute("start", start);
        model.addAttribute("limit", limit);
        model.addAttribute("authorsList", postService.getAllAuthors());
        model.addAttribute("tagsList", tagService.getAllTags());
        model.addAttribute("selectedAuthors", author);
        model.addAttribute("selectedTags", tagId);
        model.addAttribute("search", search);
        model.addAttribute("sortField", sortField);
        model.addAttribute("order", order);
        model.addAttribute("isAdmin", authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
        model.addAttribute("loggedIn", authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken));
        model.addAttribute("currentUserEmail", authentication == null ? null : authentication.getName());

        return "posts/index";
    }

    @GetMapping("/post/{id}")
    public String viewPost(@PathVariable Long id, Model model) {
        Post post = postService.getPostById(id);
        model.addAttribute("post", post);
        model.addAttribute("newComment", new Comment());
        return "posts/view";
    }

    @GetMapping("/post/new")
    public String showCreateForm(Authentication authentication, Model model) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        model.addAttribute("post", new Post());
        model.addAttribute("isAdmin", isAdmin);
        return "posts/form";
    }

    @PostMapping("/post/save")
    public String savePost(@ModelAttribute("post") Post post,
                           @RequestParam("rawTags") String rawTags,
                           Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!isAdmin) {
            User user = userAccountService.findByEmail(authentication.getName());
            if (post.getId() == null) {
                post.setAuthor(user.getName());
                post.setAuthorUser(user);
            } else {
                Post existingPost = postService.getPostById(post.getId());
                ensureOwner(existingPost, authentication);
                post.setAuthor(existingPost.getAuthor());
                post.setAuthorUser(existingPost.getAuthorUser());
            }
        } else if (post.getId() != null) {
            Post existingPost = postService.getPostById(post.getId());
            post.setAuthor(existingPost.getAuthor());
            post.setAuthorUser(existingPost.getAuthorUser());
        }
        postService.savePost(post, rawTags);
        return "redirect:/";
    }

    @GetMapping("/post/edit/{id}")
    public String showEditForm(@PathVariable Long id, Authentication authentication, Model model) {
        Post post = postService.getPostById(id);
        ensureOwner(post, authentication);
        String existingTags = post.getTags().stream().map(Tag::getName).collect(Collectors.joining(", "));
        model.addAttribute("post", post);
        model.addAttribute("rawTags", existingTags);
        model.addAttribute("isAdmin", true);
        return "posts/form";
    }

    @PostMapping("/post/delete/{id}")
    public String deletePost(@PathVariable Long id, Authentication authentication) {
        ensureOwner(postService.getPostById(id), authentication);
        postService.deletePost(id);
        return "redirect:/";
    }
    @PostMapping("/post/ai-suggest")
    @ResponseBody
    public String suggestExcerptAndTags(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        return openAiService.generateExcerptAndTags(content);
    }

    private void ensureOwner(Post post, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        boolean isOwner = post.getAuthorUser() != null
                && post.getAuthorUser().getEmail().equalsIgnoreCase(authentication.getName());

        if (!isAdmin && !isOwner) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only modify your own posts.");
        }
    }
}
