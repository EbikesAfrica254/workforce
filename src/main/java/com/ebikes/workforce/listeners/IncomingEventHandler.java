package com.ebikes.workforce.listeners;

import java.io.IOException;

public interface IncomingEventHandler {

  void handle(byte[] payload) throws IOException;

  boolean matches(String routingKey);
}
