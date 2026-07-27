package com.lbusch.github.events.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import com.lbusch.github.events.entity.PushEvent;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class PushEventSpecificationUtilsTest {

    @Mock
    private Root<PushEvent> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Path<Object> path;

    @Mock
    private Predicate predicate;

    @BeforeEach
    void setUp() {
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(criteriaBuilder.equal(any(), any(Object.class))).thenReturn(predicate);
    }

    @Test
    void hasActorId_returnsNull_whenValueIsNull() {
        Specification<PushEvent> spec = PushEventSpecificationUtils.hasActorId(null);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
        assertThat(result).isNull();
    }

    @Test
    void hasActorId_returnsPredicate_whenValueIsPresent() {
        Specification<PushEvent> spec = PushEventSpecificationUtils.hasActorId(123L);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
        assertThat(result).isEqualTo(predicate);
        verify(root).get("actorId");
        verify(criteriaBuilder).equal(path, 123L);
    }

    @Test
    void hasRepositoryId_returnsNull_whenValueIsNull() {
        Specification<PushEvent> spec = PushEventSpecificationUtils.hasRepositoryId(null);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
        assertThat(result).isNull();
    }

    @Test
    void hasRepositoryId_returnsPredicate_whenValueIsPresent() {
        Specification<PushEvent> spec = PushEventSpecificationUtils.hasRepositoryId(456L);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
        assertThat(result).isEqualTo(predicate);
        verify(root).get("repositoryId");
        verify(criteriaBuilder).equal(path, 456L);
    }

    @Test
    void hasPushId_returnsNull_whenValueIsNull() {
        Specification<PushEvent> spec = PushEventSpecificationUtils.hasPushId(null);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
        assertThat(result).isNull();
    }

    @Test
    void hasPushId_returnsPredicate_whenValueIsPresent() {
        Specification<PushEvent> spec = PushEventSpecificationUtils.hasPushId(789L);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
        assertThat(result).isEqualTo(predicate);
        verify(root).get("pushId");
        verify(criteriaBuilder).equal(path, 789L);
    }

    @Test
    void hasRef_returnsNull_whenValueIsNull() {
        Specification<PushEvent> spec = PushEventSpecificationUtils.hasRef(null);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
        assertThat(result).isNull();
    }

    @Test
    void hasRef_returnsPredicate_whenValueIsPresent() {
        Specification<PushEvent> spec = PushEventSpecificationUtils.hasRef("refs/heads/main");
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
        assertThat(result).isEqualTo(predicate);
        verify(root).get("ref");
        verify(criteriaBuilder).equal(path, "refs/heads/main");
    }

    @Test
    void hasHead_returnsNull_whenValueIsNull() {
        Specification<PushEvent> spec = PushEventSpecificationUtils.hasHead(null);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
        assertThat(result).isNull();
    }

    @Test
    void hasHead_returnsPredicate_whenValueIsPresent() {
        Specification<PushEvent> spec = PushEventSpecificationUtils.hasHead("abc123");
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
        assertThat(result).isEqualTo(predicate);
        verify(root).get("head");
        verify(criteriaBuilder).equal(path, "abc123");
    }

    @Test
    void hasBefore_returnsNull_whenValueIsNull() {
        Specification<PushEvent> spec = PushEventSpecificationUtils.hasBefore(null);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
        assertThat(result).isNull();
    }

    @Test
    void hasBefore_returnsPredicate_whenValueIsPresent() {
        Specification<PushEvent> spec = PushEventSpecificationUtils.hasBefore("def456");
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);
        assertThat(result).isEqualTo(predicate);
        verify(root).get("before");
        verify(criteriaBuilder).equal(path, "def456");
    }
}
