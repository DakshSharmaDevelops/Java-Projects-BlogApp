package com.blog.dto;

import jakarta.validation.constraints.NotBlank;

public record PostRequest(
        @NotBlank String title,
        String excerpt,
        @NotBlank String content,
        String rawTags,
        Boolean published
) {
}
