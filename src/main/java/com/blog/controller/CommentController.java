package com.blog.controller;

import com.blog.entity.Comment;
import com.blog.service.CommentService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {

        this.commentService = commentService;
    }

    @PostMapping("/create")
    public String createComment(@RequestParam("postId") Long postId,
                                @RequestParam("name") String name,
                                @RequestParam("email") String email,
                                @RequestParam("comment") String commentText) {

        Comment comment = Comment.builder()
                .name(name)
                .email(email)
                .comment(commentText)
                .build();

        commentService.addComment(postId, comment);
        return "redirect:/post/" + postId;
    }

    @PostMapping("/update")
    public String updateComment(@RequestParam("commentId") Long commentId,
                                @RequestParam("postId") Long postId,
                                @RequestParam("commentText") String commentText) {
        commentService.updateComment(commentId, commentText);
        return "redirect:/post/" + postId;
    }

    @PostMapping("/delete")
    public String deleteComment(@RequestParam("commentId") Long commentId,
                                @RequestParam("postId") Long postId) {
        commentService.deleteComment(commentId);
        return "redirect:/post/" + postId;
    }
}