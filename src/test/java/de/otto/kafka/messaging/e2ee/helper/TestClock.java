package de.otto.kafka.messaging.e2ee.helper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TestClock extends Clock {

  private ZonedDateTime currentTime;

  public TestClock(String currentTime) {
    this.currentTime = ZonedDateTime.parse(currentTime);
  }

  public TestClock(ZonedDateTime currentTime) {
    this.currentTime = currentTime;
  }

  public void setCurrentTime(String currentTime) {
    this.currentTime = ZonedDateTime.parse(currentTime);
  }

  @Override
  public ZoneId getZone() {
    return currentTime.getZone();
  }

  @Override
  public TestClock withZone(ZoneId zone) {
    return new TestClock(this.currentTime.withZoneSameInstant(zone));
  }

  @Override
  public Instant instant() {
    return currentTime.toInstant();
  }
}
