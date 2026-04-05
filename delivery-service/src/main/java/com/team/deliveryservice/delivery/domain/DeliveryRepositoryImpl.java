package com.team.deliveryservice.delivery.domain;

import com.team.common.page.SortDirection;
import com.team.deliveryservice.delivery.application.search.DeliverySearchCondition;
import com.team.deliveryservice.global.common.CurrentUser;
import com.team.deliveryservice.global.error.DeliveryErrorCode;
import com.team.deliveryservice.global.error.ServiceException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

public class DeliveryRepositoryImpl implements DeliveryRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Delivery> search(DeliverySearchCondition condition, int size, CurrentUser currentUser) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Delivery> cq = cb.createQuery(Delivery.class);
        Root<Delivery> root = cq.from(Delivery.class);

        List<Predicate> predicates = buildPredicates(cb, cq, root, condition, currentUser);
        cq.where(predicates.toArray(new Predicate[0]));

        String sortBy = (condition.sortBy() == null || condition.sortBy().isBlank())
            ? "createdAt"
            : condition.sortBy();

        if (!sortBy.equals("createdAt") && !sortBy.equals("updatedAt")) {
            sortBy = "createdAt";
        }

        SortDirection direction = SortDirection.from(condition.direction());

        if (direction == SortDirection.ASC) {
            cq.orderBy(cb.asc(root.get(sortBy)));
        } else {
            cq.orderBy(cb.desc(root.get(sortBy)));
        }

        TypedQuery<Delivery> query = em.createQuery(cq);

        int page = condition.page() == null || condition.page() < 0 ? 0 : condition.page();
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        List<Delivery> content = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Delivery> countRoot = countQuery.from(Delivery.class);
        List<Predicate> countPredicates = buildPredicates(cb, countQuery, countRoot, condition, currentUser);

        countQuery.select(cb.count(countRoot));
        countQuery.where(countPredicates.toArray(new Predicate[0]));

        Long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    private List<Predicate> buildPredicates(
        CriteriaBuilder cb,
        CriteriaQuery<?> query,
        Root<Delivery> root,
        DeliverySearchCondition condition,
        CurrentUser currentUser
    ) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isNull(root.get("deletedAt")));

        if (condition.orderId() != null) {
            predicates.add(cb.equal(root.get("orderId"), condition.orderId()));
        }
        if (condition.deliveryStatus() != null) {
            predicates.add(cb.equal(root.get("deliveryStatus"), condition.deliveryStatus()));
        }
        if (condition.originHubId() != null) {
            predicates.add(cb.equal(root.get("originHubId"), condition.originHubId()));
        }
        if (condition.destinationHubId() != null) {
            predicates.add(cb.equal(root.get("destinationHubId"), condition.destinationHubId()));
        }
        if (condition.receiverCompanyId() != null) {
            predicates.add(cb.equal(root.get("receiverCompanyId"), condition.receiverCompanyId()));
        }
        if (condition.companyDeliveryManagerId() != null) {
            predicates.add(cb.equal(root.get("companyDeliveryManagerId"), condition.companyDeliveryManagerId()));
        }

        predicates.add(buildScopePredicate(cb, query, root, currentUser));

        return predicates;
    }

    private Predicate buildScopePredicate(
        CriteriaBuilder cb,
        CriteriaQuery<?> query,
        Root<Delivery> root,
        CurrentUser currentUser
    ) {
        if (currentUser.isMasterAdmin()) {
            return cb.conjunction();
        }

        if (currentUser.isHubAdmin()) {
            UUID hubId = currentUser.hubId();
            if (hubId == null) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }

            return cb.or(
                cb.equal(root.get("originHubId"), hubId),
                cb.equal(root.get("destinationHubId"), hubId)
            );
        }

        if (currentUser.isCompanyManager()) {
            UUID companyId = currentUser.companyId();
            if (companyId == null) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }

            return cb.equal(root.get("receiverCompanyId"), companyId);
        }

        if (currentUser.isCompanyDeliveryManager()) {
            return cb.equal(root.get("companyDeliveryManagerId"), currentUser.userId());
        }

        if (currentUser.isHubDeliveryManager()) {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<DeliveryRouteLog> routeLogRoot = subquery.from(DeliveryRouteLog.class);

            subquery.select(routeLogRoot.get("deliveryId"));
            subquery.where(
                cb.equal(routeLogRoot.get("deliveryId"), root.get("id")),
                cb.equal(routeLogRoot.get("deliveryManagerId"), currentUser.userId()),
                cb.isNull(routeLogRoot.get("deletedAt"))
            );

            return cb.exists(subquery);
        }

        throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
    }
}
