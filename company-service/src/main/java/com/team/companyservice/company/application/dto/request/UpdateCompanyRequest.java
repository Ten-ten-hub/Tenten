package com.team.companyservice.company.application.dto.request;

import java.util.UUID;

import com.team.companyservice.company.domain.CompanyType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateCompanyRequest {

    @NotBlank
    private String name;

    @NotNull
    private CompanyType companyType;

    @NotNull
    private UUID hubId;

    @NotBlank
    private String address;

    private String addressDetail;
    private String zipcode;
    private String contactName;
    private String contactPhone;
    private String contactSlackId;
}
