package com.team.companyservice.application.company;

import java.util.UUID;

import com.team.companyservice.domain.company.CompanyType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CreateCompanyRequest {

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