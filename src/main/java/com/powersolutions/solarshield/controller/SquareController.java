package com.powersolutions.solarshield.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.powersolutions.solarshield.dto.SquareUpdateRequest;
import com.powersolutions.solarshield.mapper.SquareUpdateMapper;
import com.powersolutions.solarshield.service.security.SquareSignatureVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
public class SquareController {

    private final SquareSignatureVerifier signatureVerifier;

    @Autowired
    public SquareController(SquareSignatureVerifier signatureVerifier) {
        this.signatureVerifier = signatureVerifier;
    }

    @PostMapping("/updates")
    public void paymentUpdate(@RequestBody String squareRequest,
                              @RequestHeader("x-square-hmacsha256-signature") String signatureHeader) throws JsonProcessingException {

        boolean isValidSig = signatureVerifier.isValidSignature(squareRequest, signatureHeader);
        System.out.println("Is valid signature: " + isValidSig);

        if (isValidSig) {
            SquareUpdateRequest request = new SquareUpdateMapper(squareRequest).getRequest();
            System.out.println(request);
        }
    }

    @PostMapping("/updates_test")
    public void paymentUpdate(@RequestBody String squareRequest) throws JsonProcessingException {
        SquareUpdateRequest request = new SquareUpdateMapper(squareRequest).getRequest();
        System.out.println(request);
    }

}
