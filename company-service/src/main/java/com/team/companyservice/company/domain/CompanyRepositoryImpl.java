package com.team.companyservice.company.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.team.companyservice.company.application.search.CompanySearchCondition;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CompanyRepositoryImpl implements CompanyRepositoryCustom {

    private final EntityManager em;

    @Override
    public Page<Company> search(CompanySearchCondition condition, Pageable pageable) {
        var cb = em.getCriteriaBuilder();

        var cq = cb.createQuery(Company.class);
        var root = cq.from(Company.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isNull(root.get("deletedAt")));

        if (condition.keyword() != null && !condition.keyword().isBlank()) {
            predicates.add(
                    cb.like(
                            cb.lower(root.get("name")),
                            "%" + condition.keyword().toLowerCase() + "%"
                    )
            );
        }

        if (condition.companyType() != null) {
            predicates.add(cb.equal(root.get("companyType"), condition.companyType()));
        }

        if (condition.hubId() != null) {
            predicates.add(cb.equal(root.get("hubId"), condition.hubId()));
        }

        if (condition.isActive() != null) {
            predicates.add(cb.equal(root.get("isActive"), condition.isActive()));
        }

        cq.where(predicates.toArray(new Predicate[0]));

        pageable.getSort().forEach(order -> {
            if (order.isAscending()) {
                cq.orderBy(cb.asc(root.get(order.getProperty())));
            } else {
                cq.orderBy(cb.desc(root.get(order.getProperty())));
            }
        });

        TypedQuery<Company> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Company> content = query.getResultList();

        var countQuery = cb.createQuery(Long.class);
        var countRoot = countQuery.from(Company.class);

        List<Predicate> countPredicates = new ArrayList<>();
        countPredicates.add(cb.isNull(countRoot.get("deletedAt")));

        if (condition.keyword() != null && !condition.keyword().isBlank()) {
            countPredicates.add(
                    cb.like(
                            cb.lower(countRoot.get("name")),
                            "%" + condition.keyword().toLowerCase() + "%"
                    )
            );
        }

        if (condition.companyType() != null) {
            countPredicates.add(cb.equal(countRoot.get("companyType"), condition.companyType()));
        }

        if (condition.hubId() != null) {
            countPredicates.add(cb.equal(countRoot.get("hubId"), condition.hubId()));
        }

        if (condition.isActive() != null) {
            countPredicates.add(cb.equal(countRoot.get("isActive"), condition.isActive()));
        }

        countQuery.select(cb.count(countRoot));
        countQuery.where(countPredicates.toArray(new Predicate[0]));
        long total = em.createQuery(countQuery).getSingleResult();

        return new org.springframework.data.domain.PageImpl<>(content, pageable, total);
    }
}
