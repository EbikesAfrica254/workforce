package com.ebikes.workforce.dtos.requests.agents;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.ebikes.workforce.dtos.requests.documents.DocumentUploadInfo;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.NationalIdType;

public record CreateAgentRequest(
    @Pattern(
            regexp = "^\\+[1-9]\\d{6,14}$",
            message = "Alternate phone number must be in E.164 format")
        @Size(max = 20, message = "Alternate phone number must not exceed 20 characters") String alternatePhoneNumber,
    @NotNull(message = "Capability class is required") CapabilityClass capabilityClass,
    @NotNull(message = "Documents are required") @Size(min = 2, message = "At least NATIONAL_ID_FRONT and NATIONAL_ID_BACK are required") @Valid List<DocumentUploadInfo> documents,
    @Email(message = "Email must be valid") @Size(max = 255, message = "Email must not exceed 255 characters") String email,
    @NotBlank(message = "First name is required") @Size(max = 100, message = "First name must not exceed 100 characters") String firstName,
    @NotBlank(message = "Last name is required") @Size(max = 100, message = "Last name must not exceed 100 characters") String lastName,
    @Positive(message = "Max concurrentOrders must be positive") Short maxConcurrentOrders,
    @NotBlank(message = "National ID number is required") @Size(max = 50, message = "National ID number must not exceed 50 characters") String nationalIdNumber,
    @NotNull(message = "National ID type is required") NationalIdType nationalIdType,
    @NotBlank(message = "Phone number is required") @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Phone number must be in E.164 format") @Size(max = 20, message = "Phone number must not exceed 20 characters") String phoneNumber,
    @NotBlank(message = "User ID is required") @Size(max = 36, message = "User ID must not exceed 36 characters") String userId) {

  public CreateAgentRequest {
    documents = documents == null ? null : List.copyOf(documents);
  }
}
