package com.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CommentRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String comment
) {
}
