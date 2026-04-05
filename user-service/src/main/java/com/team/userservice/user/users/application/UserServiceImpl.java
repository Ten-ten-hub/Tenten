package com.team.userservice.user.users.application;

import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.exception.UserException;
import com.team.userservice.user.companies.application.CompanyService;
import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.AffiliatedStatus;
import com.team.userservice.user.core.enums.Affiliation;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
import com.team.userservice.user.core.vo.UserUpdateInfo;
import com.team.userservice.user.hubs.application.HubService;
import com.team.userservice.user.users.application.dto.LoginServiceDto;
import com.team.userservice.user.users.application.dto.SignUpResultDto;
import com.team.userservice.user.users.application.dto.SignUpServiceDto;
import com.team.userservice.user.users.application.dto.UpdateUserServiceDto;
import com.team.userservice.user.users.application.dto.UserDataDto;
import com.team.userservice.user.users.domain.UserRepository;
import com.team.userservice.user.users.infrastructure.feignClient.CompanyInternalClient;
import com.team.userservice.user.users.infrastructure.feignClient.HubInternalClient;
import feign.FeignException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final HubService hubService;
    private final CompanyService companyService;
    private final HubInternalClient hubInternalClient;
    private final CompanyInternalClient companyInternalClient;

    @Override
    public SignUpResultDto signUp(SignUpServiceDto serviceDto) {

        if (userRepository.existsByLoginId(serviceDto.loginId())) {
            throw new UserException(UserErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (userRepository.existsByEmail(serviceDto.email())) {
            throw new UserException(UserErrorCode.DUPLICATE_EMAIL);
        }

        try {
            User user = userRepository.save(User.create()
                .loginId(serviceDto.loginId())
                .password(passwordEncoder.encode(serviceDto.password()))
                .name(serviceDto.name())
                .slackId(serviceDto.slackId())
                .email(serviceDto.email())
                .phoneNumber(serviceDto.phoneNumber())
                .build());
            userRepository.flush();
            return SignUpResultDto.from(user);
        } catch (DataIntegrityViolationException e) {
            throw new UserException(UserErrorCode.DUPLICATE_USER_INFO);
        }


    }

    @Override
    @Transactional
    public void register(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        user.register();
    }

    @Override
    public LoginServiceDto loginService(String loginId, String password) {
        User user = userRepository.findByLoginId(loginId);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UserException(UserErrorCode.INVALID_CREDENTIALS);
        }
        return LoginServiceDto.from(user.getId(), user.getRole());
    }

    @Override
    @Transactional
    public void updateUserRole(UUID userId, Role role) {
        userRepository.findById(userId)
            .ifPresentOrElse(
                user -> userRepository.updateUserRole(user, role),
                () -> {
                    throw new UserException(UserErrorCode.USER_NOT_FOUND);
                }
            );
    }

    @Override
    @Transactional
    public void userUpdate(UUID userId, UpdateUserServiceDto serviceDto) {
        UserUpdateInfo info = serviceDto.toEntityDto();
        userRepository.findById(userId)
            .ifPresentOrElse(
                user -> user.userUpdate(info),
                () -> {
                    throw new UserException(UserErrorCode.USER_NOT_FOUND);
                }
            );
    }

    @Override
    @Transactional
    public void updateUserAffiliation(UUID userId, Affiliation affiliation, UUID affiliationId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        Role role = user.getRole();
        AffiliatedStatus affiliatedStatus = user.getAffiliatedStatus();

        verifyAffiliationId(affiliation, affiliationId);

        if(affiliatedStatus == AffiliatedStatus.NOT_APPLICABLE) {
            throw new UserException(UserErrorCode.NOT_APPLICABLE);
        }

        if (affiliation == Affiliation.HUB) {
            if(role == Role.COMPANY_MANAGER || role == Role.COM_DELIVERY_MANAGER) {
                throw new UserException(UserErrorCode.ROLE_AFFILIATION_CONFLICT);
            }

            if(affiliatedStatus == AffiliatedStatus.HUB_AFFILIATED){
                hubService.findByUser(user).updateHubId(affiliationId);
            }else if(affiliatedStatus == AffiliatedStatus.UNAFFILIATED){
                hubService.save(user, affiliationId);
                user.updateUserAffiliation(AffiliatedStatus.HUB_AFFILIATED);
            }

        }

        if (affiliation == Affiliation.COMPANY){
            if(role == Role.HUB_ADMIN || role == Role.HUB_DELIVERY_MANAGER){
                throw new UserException(UserErrorCode.ROLE_AFFILIATION_CONFLICT);
            }

            if(affiliatedStatus == AffiliatedStatus.COM_AFFILIATED){
                companyService.findByUser(user).updateCompanyId(affiliationId);
            }else if(affiliatedStatus == AffiliatedStatus.UNAFFILIATED){
                companyService.save(user, affiliationId);
                user.updateUserAffiliation(AffiliatedStatus.COM_AFFILIATED);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDataDto getUserInfo(UUID userId) {

        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        if (user.getAffiliatedStatus() == AffiliatedStatus.HUB_AFFILIATED) {
            return UserDataDto.fromUserInfo(user, hubService.findByUser(user).getHubId());
        } else if (user.getAffiliatedStatus() == AffiliatedStatus.COM_AFFILIATED) {
            return UserDataDto.fromUserInfo(user, companyService.findByUser(user).getCompanyId());
        } else if (user.getAffiliatedStatus() == AffiliatedStatus.UNAFFILIATED
            || user.getAffiliatedStatus() == AffiliatedStatus.NOT_APPLICABLE) {
            return UserDataDto.fromUserInfo(user);
        } else{
            throw new IllegalStateException("Unexpected affiliatedStatus: " + user.getAffiliatedStatus());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUserInfo(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUserInfo(List<Role> roles, AffiliatedStatus affiliatedStatus, Pageable pageable) {
        return userRepository.findAll(roles, affiliatedStatus, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUserInfoInternal() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUserInfoInternal(List<Role> roles, AffiliatedStatus affiliatedStatus) {
        return userRepository.findAll(roles, affiliatedStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUserInfoBySignUpStatus(SignupStatus signupStatus, Pageable pageable) {
        return userRepository.findAllBySignupStatus(signupStatus, pageable);
    }

    @Override
    @Transactional
    public void deleteUser(UUID targetId, UUID deletedBy) {
        userRepository.findById(targetId).ifPresentOrElse(
            user -> user.softDelete(deletedBy),
            () -> {
                throw new UserException(UserErrorCode.USER_NOT_FOUND);
            }
        );
    }

    @Override
    @Transactional
    public void updateLastLoginAt(UUID userId) {
        userRepository.findById(userId).ifPresentOrElse(
            user -> user.updateLastLoginAt(LocalDateTime.now()),
            () -> {
                throw new UserException(UserErrorCode.USER_NOT_FOUND);
            }
        );
    }

    @Override
    public Role getUserRole(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND)).getRole();
    }

    @Override
    public void verifyAffiliationId(Affiliation affiliation, UUID affiliationId) {
        if(affiliation == Affiliation.HUB){

            try{
                hubInternalClient.isValidID(affiliationId);
            }catch (FeignException.NotFound e){
                throw new UserException(UserErrorCode.INVALID_HUB_ID);
            }
        }else if(affiliation == Affiliation.COMPANY){
            try{
                companyInternalClient.isValidID(affiliationId);
            }catch (FeignException.NotFound e){
                throw new UserException(UserErrorCode.INVALID_COMPANY_ID);
            }
        }else{
            throw new UserException(UserErrorCode.NOT_EXIST_AFFILIATION);
        }
    }

    @Override
    public String getUserSlackId(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND)).getSlackId();
    }
}
