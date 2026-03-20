package com.ebikes.workforce.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.ebikes.workforce.database.entities.PaymentMethod;
import com.ebikes.workforce.dtos.responses.paymentmethods.PaymentMethodDetailResponse;
import com.ebikes.workforce.dtos.responses.paymentmethods.PaymentMethodSummaryResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMethodMapper {

  PaymentMethodDetailResponse toDetailResponse(PaymentMethod paymentMethod);

  PaymentMethodSummaryResponse toSummaryResponse(
      PaymentMethod paymentMethod, String maskedIdentifier);
}
