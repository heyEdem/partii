package com.theinside.partii.dto;

import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Cursor for keyset pagination.
 * Uses eventDate and id as the composite cursor for stable, high-performance pagination.
 */
public record EventCursor(
    LocalDateTime eventDate,
    Long id
) {
    /**
     * Encode cursor to Base64 string for API responses.
     * Format: "eventDate|id"
     */
    public String encode() {
        String raw = eventDate + "|" + id;
        return Base64.getUrlEncoder().encodeToString(raw.getBytes());
    }

    /**
     * Decode Base64 cursor string back to EventCursor.
     */
    public static EventCursor decode(String encodedCursor) {
        if (encodedCursor == null || encodedCursor.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(encodedCursor));
            String[] parts = decoded.split("\\|");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid cursor format");
            }
            LocalDateTime eventDate = LocalDateTime.parse(parts[0]);
            Long id = (long) Integer.parseInt(parts[1]);
            return new EventCursor(eventDate, id);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor: " + encodedCursor, e);
        }
    }
}
