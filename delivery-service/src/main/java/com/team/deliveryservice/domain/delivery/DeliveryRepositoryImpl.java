package com.team.deliveryservice.domain.delivery;

import com.team.common.page.SortDirection;
import com.team.deliveryservice.application.delivery.DeliverySearchCondition;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

public class DeliveryRepositoryImpl implements DeliveryRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Delivery> search(DeliverySearchCondition condition, int size) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Delivery> cq = cb.createQuery(Delivery.class);
        Root<Delivery> root = cq.from(Delivery.class);

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
        List<Predicate> countPredicates = new ArrayList<>();
        countPredicates.add(cb.isNull(countRoot.get("deletedAt")));

        if (condition.orderId() != null) {
            countPredicates.add(cb.equal(countRoot.get("orderId"), condition.orderId()));
        }
        if (condition.deliveryStatus() != null) {
            countPredicates.add(cb.equal(countRoot.get("deliveryStatus"), condition.deliveryStatus()));
        }
        if (condition.originHubId() != null) {
            countPredicates.add(cb.equal(countRoot.get("originHubId"), condition.originHubId()));
        }
        if (condition.destinationHubId() != null) {
            countPredicates.add(cb.equal(countRoot.get("destinationHubId"), condition.destinationHubId()));
        }
        if (condition.receiverCompanyId() != null) {
            countPredicates.add(cb.equal(countRoot.get("receiverCompanyId"), condition.receiverCompanyId()));
        }
        if (condition.companyDeliveryManagerId() != null) {
            countPredicates.add(cb.equal(countRoot.get("companyDeliveryManagerId"), condition.companyDeliveryManagerId()));
        }

        countQuery.select(cb.count(countRoot));
        countQuery.where(countPredicates.toArray(new Predicate[0]));

        Long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }
}
