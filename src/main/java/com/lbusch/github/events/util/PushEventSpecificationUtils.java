package com.lbusch.github.events.util;

import com.lbusch.github.events.entity.PushEvent;
import org.springframework.data.jpa.domain.Specification;

public final class PushEventSpecificationUtils {

    private PushEventSpecificationUtils() {}

    public static Specification<PushEvent> hasRepositoryId(Long repositoryId) {
        return (root, query, cb) -> repositoryId == null ? null : cb.equal(root.get("repositoryId"), repositoryId);
    }

    public static Specification<PushEvent> hasPushId(Long pushId) {
        return (root, query, cb) -> pushId == null ? null : cb.equal(root.get("pushId"), pushId);
    }

    public static Specification<PushEvent> hasRef(String ref) {
        return (root, query, cb) -> ref == null ? null : cb.equal(root.get("ref"), ref);
    }

    public static Specification<PushEvent> hasHead(String head) {
        return (root, query, cb) -> head == null ? null : cb.equal(root.get("head"), head);
    }

    public static Specification<PushEvent> hasBefore(String before) {
        return (root, query, cb) -> before == null ? null : cb.equal(root.get("before"), before);
    }
}
