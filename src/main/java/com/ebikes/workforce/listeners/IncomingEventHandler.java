package com.ebikes.workforce.listeners;

public interface IncomingEventHandler {

  void handle(byte[] payload);

  boolean matches(String routingKey);
}
