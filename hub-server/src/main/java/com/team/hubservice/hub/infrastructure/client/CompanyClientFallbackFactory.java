package com.team.hubservice.hub.infrastructure.client;

import com.team.common.exception.BusinessException;
import com.team.common.exception.CommonErrorCode;
import com.team.hubservice.hub.infrastructure.client.dto.CompanyExistsResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class CompanyClientFallbackFactory implements FallbackFactory<CompanyClient> {

    private static final Logger log = LoggerFactory.getLogger(CompanyClientFallbackFactory.class);

    @Override
    public CompanyClient create(Throwable cause) {
        return new CompanyClient() {
            @Override
            public CompanyExistsResponse checkCompanyExistsByHubId(UUID hubId) {
                log.error("업체 서비스 통신 실패: {}", cause.getMessage());
                throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
            }
        };
    }
}
