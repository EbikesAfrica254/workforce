package com.ebikes.workforce.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.ebikes.workforce.dtos.events.incoming.OrderPendingAssignmentEvent;
import com.ebikes.workforce.dtos.events.incoming.OrderReassignmentRequestedEvent;
import com.ebikes.workforce.dtos.internal.ShortlistRequest;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ShortlistRequestMapper {

  ShortlistRequest toShortlistRequest(OrderPendingAssignmentEvent event);

  ShortlistRequest toShortlistRequest(OrderReassignmentRequestedEvent event);
}
