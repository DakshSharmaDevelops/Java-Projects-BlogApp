package com.blog.specification;

import com.blog.entity.Post;
import com.blog.entity.Tag;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public class PostSpecification {

    public static Specification<Post> filterPosts(List<String> authors, List<Long> tagIds, String searchKeyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            if (authors != null && !authors.isEmpty()) {
                predicates.add(root.get("author").in(authors));
            }

            if (tagIds != null && !tagIds.isEmpty()) {
                Join<Post, Tag> tagJoin = root.join("tags", JoinType.INNER);
                predicates.add(tagJoin.get("id").in(tagIds));
            }

            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                String keyword = "%" + searchKeyword.trim().toLowerCase() + "%";

                Predicate titleMatch = cb.like(cb.lower(root.get("title")), keyword);
                Predicate contentMatch = cb.like(cb.lower(root.get("content")), keyword);
                Predicate excerptMatch = cb.like(cb.lower(root.get("excerpt")), keyword);
                Predicate authorMatch = cb.like(cb.lower(root.get("author")), keyword);

                Join<Post, Tag> tagSearchJoin = root.join("tags", JoinType.LEFT);
                Predicate tagMatch = cb.like(cb.lower(tagSearchJoin.get("name")), keyword);

                predicates.add(cb.or(titleMatch, contentMatch, excerptMatch, authorMatch, tagMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
