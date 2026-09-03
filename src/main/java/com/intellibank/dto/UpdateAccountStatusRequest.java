package com.intellibank.dto;

import com.intellibank.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountStatusRequest {

    @NotNull(message = "Account status is required (PENDING, ACTIVE, BLOCKED, or CLOSED)")
    private AccountStatus status;
}
