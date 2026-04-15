package com.ebikes.workforce.support.infrastructure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.workforce.support.context.EventContext;

@ExtendWith(MockitoExtension.class)
@Tag("listener")
public abstract class AbstractListenerTest {

  @AfterEach
  void clearEventContext() {
    EventContext.clear();
  }
}
