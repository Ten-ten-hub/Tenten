package com.team.deliveryservice.deliverymanager.domain;

import com.team.deliveryservice.deliverymanager.application.search.DeliveryManagerSearchCondition;
import com.team.deliveryservice.global.common.CurrentUser;
import com.team.deliveryservice.global.error.DeliveryErrorCode;
import com.team.deliveryservice.global.error.ServiceException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class DeliveryManagerRepositoryImpl implements DeliveryManagerRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<DeliveryManager> search(
        DeliveryManagerSearchCondition condition,
        CurrentUser currentUser,
        Pageable pageable
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DeliveryManager> cq = cb.createQuery(DeliveryManager.class);
        Root<DeliveryManager> root = cq.from(DeliveryManager.class);

        List<Predicate> predicates = buildPredicates(condition, currentUser, cb, root);

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(buildOrder(condition, cb, root));

        TypedQuery<DeliveryManager> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<DeliveryManager> content = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<DeliveryManager> countRoot = countQuery.from(DeliveryManager.class);
        List<Predicate> countPredicates = buildPredicates(condition, currentUser, cb, countRoot);

        countQuery.select(cb.count(countRoot));
        countQuery.where(countPredicates.toArray(new Predicate[0]));

        Long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private List<Predicate> buildPredicates(
        DeliveryManagerSearchCondition condition,
        CurrentUser currentUser,
        CriteriaBuilder cb,
        Root<DeliveryManager> root
    ) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.isNull(root.get("deletedAt")));

        if (condition.hubId() != null) {
            predicates.add(cb.equal(root.get("hubId"), condition.hubId()));
        }

        if (condition.type() != null) {
            predicates.add(cb.equal(root.get("type"), condition.type()));
        }

        applyRolePredicate(currentUser, cb, root, predicates);

        return predicates;
    }

    private void applyRolePredicate(
        CurrentUser currentUser,
        CriteriaBuilder cb,
        Root<DeliveryManager> root,
        List<Predicate> predicates
    ) {
        String role = currentUser.role();

        if ("MASTER_ADMIN".equals(role)) {
            return;
        }

        if ("HUB_ADMIN".equals(role)) {
            UUID hubId = currentUser.hubId();
            if (hubId == null) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }
            predicates.add(cb.equal(root.get("hubId"), hubId));
            return;
        }

        if ("HUB_DELIVERY_MANAGER".equals(role) || "COM_DELIVERY_MANAGER".equals(role)) {
            predicates.add(cb.equal(root.get("id"), currentUser.userId()));
            return;
        }

        throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
    }

    private Order buildOrder(
        DeliveryManagerSearchCondition condition,
        CriteriaBuilder cb,
        Root<DeliveryManager> root
    ) {
        String sortBy = (condition.sortBy() == null || condition.sortBy().isBlank())
            ? "deliverySequence"
            : condition.sortBy();

        boolean asc = "ASC".equalsIgnoreCase(condition.direction());

        return switch (sortBy) {
            case "createdAt" -> asc ? cb.asc(root.get("createdAt")) : cb.desc(root.get("createdAt"));
            case "updatedAt" -> asc ? cb.asc(root.get("updatedAt")) : cb.desc(root.get("updatedAt"));
            case "deliverySequence" -> asc ? cb.asc(root.get("deliverySequence")) : cb.desc(root.get("deliverySequence"));
            default -> asc ? cb.asc(root.get("deliverySequence")) : cb.desc(root.get("deliverySequence"));
        };
    }
}
