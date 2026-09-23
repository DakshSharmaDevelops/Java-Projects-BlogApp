package com.blog.dto;

import com.blog.entity.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id, Long postId, String name, String email, String comment,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getPost().getId(),
                comment.getName(), comment.getEmail(), comment.getComment(),
                comment.getCreatedAt(), comment.getUpdatedAt());
    }
}
