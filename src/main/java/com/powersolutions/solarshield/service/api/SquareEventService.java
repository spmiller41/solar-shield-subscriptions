package com.powersolutions.solarshield.service.api;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;

import java.time.LocalDateTime;

public interface SquareEventService {

    boolean hasProcessed(String eventId);

    void recordProcessedEvent(SquareInvoicePaymentRequest request);

}
