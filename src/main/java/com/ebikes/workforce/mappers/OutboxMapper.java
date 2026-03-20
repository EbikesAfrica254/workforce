package com.ebikes.workforce.mappers;

import org.mapstruct.Mapper;

import com.ebikes.workforce.database.entities.Outbox;
import com.ebikes.workforce.dtos.responses.outbox.OutboxResponse;

@Mapper(componentModel = "spring")
public interface OutboxMapper {

  OutboxResponse toResponse(Outbox outbox);
}
