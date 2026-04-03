package com.team.userservice.user.users.infrastructure;

import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.AffiliatedStatus;
import com.team.userservice.user.core.enums.Role;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> hasRoles(List<Role> roles) {
        return (root, query, cb) ->
            roles == null || roles.isEmpty() ? null : root.get("role").in(roles);
    }

    public static Specification<User> hasAffiliatedStatus(AffiliatedStatus affiliatedStatus) {
        return (root, query, cb) ->
            affiliatedStatus == null ? null : cb.equal(root.get("affiliatedStatus"), affiliatedStatus);
    }
}
