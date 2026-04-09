package com.powersolutions.solarshield.service.api;

import java.time.LocalDateTime;

public interface SquareEventService {

    boolean hasProcessed(String eventId);

    void recordProcessedEvent(String eventId, LocalDateTime createdAt);

}
