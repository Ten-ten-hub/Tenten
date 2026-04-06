package com.team.deliveryservice.delivery.domain;

import com.team.common.page.SortDirection;
import com.team.deliveryservice.delivery.application.search.DeliverySearchCondition;
import com.team.deliveryservice.global.common.CurrentUser;
import com.team.deliveryservice.global.error.DeliveryErrorCode;
import com.team.deliveryservice.global.error.ServiceException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.AbstractQuery;
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
import org.springframework.stereotype.Repository;

@Repository
public class DeliveryRepositoryImpl implements DeliveryRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Delivery> search(DeliverySearchCondition condition, CurrentUser currentUser, int size) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Delivery> cq = cb.createQuery(Delivery.class);
        Root<Delivery> root = cq.from(Delivery.class);

        List<Predicate> predicates = buildPredicates(condition, currentUser, cb, cq, root);
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
        List<Predicate> countPredicates = buildPredicates(condition, currentUser, cb, countQuery, countRoot);

        countQuery.select(cb.count(countRoot));
        countQuery.where(countPredicates.toArray(new Predicate[0]));

        Long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    private List<Predicate> buildPredicates(
        DeliverySearchCondition condition,
        CurrentUser currentUser,
        CriteriaBuilder cb,
        AbstractQuery<?> query,
        Root<Delivery> root
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

        applyRolePredicate(currentUser, cb, query, root, predicates);

        return predicates;
    }

    private void applyRolePredicate(
        CurrentUser currentUser,
        CriteriaBuilder cb,
        AbstractQuery<?> query,
        Root<Delivery> root,
        List<Predicate> predicates
    ) {
        if (currentUser == null) {
            throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
        }

        if (currentUser.isMasterAdmin()) {
            return;
        }

        if (currentUser.isHubAdmin()) {
            if (currentUser.hubId() == null) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }

            predicates.add(cb.or(
                cb.equal(root.get("originHubId"), currentUser.hubId()),
                cb.equal(root.get("destinationHubId"), currentUser.hubId())
            ));
            return;
        }

        if (currentUser.isCompanyManager()) {
            if (currentUser.companyId() == null) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }

            predicates.add(cb.equal(root.get("receiverCompanyId"), currentUser.companyId()));
            return;
        }

        if (currentUser.isCompanyDeliveryManager()) {
            if (currentUser.userId() == null) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }

            predicates.add(cb.equal(root.get("companyDeliveryManagerId"), currentUser.userId()));
            return;
        }

        if (currentUser.isHubDeliveryManager()) {
            if (currentUser.userId() == null) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }

            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<DeliveryRouteLog> routeRoot = subquery.from(DeliveryRouteLog.class);

            subquery.select(routeRoot.get("deliveryId"));
            subquery.where(
                cb.equal(routeRoot.get("deliveryId"), root.get("id")),
                cb.equal(routeRoot.get("deliveryManagerId"), currentUser.userId()),
                cb.isNull(routeRoot.get("deletedAt"))
            );

            predicates.add(cb.exists(subquery));
            return;
        }

        throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
    }
}
