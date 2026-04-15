package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.service.api.SquareEventService;

import java.time.LocalDateTime;

public class SquareEventServiceImpl implements SquareEventService {
    @Override
    public boolean hasProcessed(String eventId) {
        return false;
    }

    @Override
    public void recordProcessedEvent(String eventId, LocalDateTime createdAt) {

    }
}
