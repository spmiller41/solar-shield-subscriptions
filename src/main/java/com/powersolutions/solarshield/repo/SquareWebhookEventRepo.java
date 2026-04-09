package com.powersolutions.solarshield.repo;

import com.powersolutions.solarshield.entity.SquareWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SquareWebhookEventRepo extends JpaRepository<SquareWebhookEvent, String> {

    boolean existsByEventId(String eventId);

}