package com.agrawalpulse.common.security.local;

public record LocalTokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
