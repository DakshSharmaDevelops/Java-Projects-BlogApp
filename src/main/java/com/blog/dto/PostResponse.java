package com.blog.dto;

import com.blog.entity.Post;

import java.time.LocalDateTime;

public record PostResponse(
        Long id, String title, String excerpt, String content, String author,
        LocalDateTime publishedAt, Boolean published, LocalDateTime createdAt,
        LocalDateTime updatedAt, java.util.List<String> tags
) {
    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getTitle(), post.getExcerpt(),
                post.getContent(), post.getAuthor(), post.getPublishedAt(),
                post.getIsPublished(), post.getCreatedAt(), post.getUpdatedAt(),
                post.getTags().stream().map(tag -> tag.getName()).sorted().toList());
    }
}
