package com.powersolutions.solarshield.service.api;

import com.powersolutions.solarshield.entity.Contact;

public interface ContactService {

    Contact upsertAndGet(Contact incoming);

}