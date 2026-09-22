package com.blog.service;

import com.blog.entity.Post;
import com.blog.entity.Tag;
import com.blog.repository.PostRepository;
import com.blog.repository.TagRepository;
import com.blog.specification.PostSpecification;
import com.blog.util.OffsetBasedPageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;

    public PostService(PostRepository postRepository, TagRepository tagRepository) {
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
    }

    public Page<Post> getFilteredPosts(int start, int limit, List<String> authors, List<Long> tagIds,
                                       String search, String sortField, String order) {
        long offset = Math.max(0, start - 1);
        Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, (sortField == null || sortField.isEmpty()) ? "publishedAt" : sortField);

        Pageable pageable = new OffsetBasedPageRequest(offset, limit, sort);
        return postRepository.findAll(PostSpecification.filterPosts(authors, tagIds, search), pageable);
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id).orElseThrow(() -> new RuntimeException("Post not found: " + id));
    }

    public void savePost(Post post, String rawTags) {
        Set<Tag> tags = processTags(rawTags);
        post.setTags(tags);
        if (post.getExcerpt() == null || post.getExcerpt().isEmpty()) {
            post.setExcerpt(post.getContent().length() > 150 ? post.getContent().substring(0, 150) + "..." : post.getContent());
        }
        postRepository.save(post);
    }

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    public List<String> getAllAuthors() {

        return postRepository.findAllDistinctAuthors();
    }

    private Set<Tag> processTags(String rawTags) {
        Set<Tag> tagSet = new HashSet<>();
        if (rawTags != null && !rawTags.trim().isEmpty()) {
            String[] tokens = rawTags.split(",");
            for (String token : tokens) {
                String cleanName = token.trim().toLowerCase();
                if (!cleanName.isEmpty()) {
                    Tag tag = tagRepository.findByName(cleanName)
                            .orElseGet(() -> tagRepository.save(Tag.builder().name(cleanName).build()));
                    tagSet.add(tag);
                }
            }
        }
        return tagSet;
    }
}
