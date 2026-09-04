package com.agrawalpulse.family.controller;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.family.dto.PincodeLookupResponse;
import com.agrawalpulse.family.entity.Pincode;
import com.agrawalpulse.family.repository.PincodeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Backed by our own pincodes table (see V4__pincode_reference_data.sql) rather than a live call
// to India Post's public API - that API proved unreliably reachable from Java on this network
// (TLS errors and plain connection resets, even though curl/the OS trust the same endpoint).
// Kept separate from FamilyController: this isn't family data and needs no family-specific
// authorization - any authenticated caller may look up a PIN code (same "authenticated is
// enough" bar as ChapterController's list/get, which also carry no @PreAuthorize of their own;
// the global SecurityConfig rule already requires a valid JWT for anything not explicitly
// permitted).
@RestController
@RequestMapping("/api/v1/families/pincode")
public class PincodeLookupController {

    private final PincodeRepository pincodeRepository;

    public PincodeLookupController(PincodeRepository pincodeRepository) {
        this.pincodeRepository = pincodeRepository;
    }

    @GetMapping("/{pincode}")
    public PincodeLookupResponse lookup(@PathVariable String pincode) {
        if (!pincode.matches("^[0-9]{6}$")) {
            throw new IllegalArgumentException("PIN code must be exactly 6 digits");
        }
        Pincode found = pincodeRepository.findById(pincode)
                .orElseThrow(() -> new ResourceNotFoundException("No location found for PIN code " + pincode));
        return new PincodeLookupResponse(found.getDistrict(), found.getState(), found.getCountry());
    }
}
