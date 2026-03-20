package com.ebikes.workforce.dtos.internal;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.ebikes.workforce.enums.FieldType;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldChange(
    @NotBlank String fieldName, @NotNull FieldType fieldType, String newValue, String oldValue)
    implements Serializable {}
