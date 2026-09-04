package com.agrawalpulse.family.dto;

// Normalized shape returned to the frontend - deliberately not India Post's raw response shape
// (an array wrapping a Status/PostOffice[] object with capitalized field names), which is an
// upstream implementation detail that stays inside PincodeClient.
public record PincodeLookupResponse(String district, String state, String country) {
}
