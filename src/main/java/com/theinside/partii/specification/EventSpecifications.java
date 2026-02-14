package com.theinside.partii.specification;

import com.theinside.partii.dto.EventSearchRequest;
import com.theinside.partii.entity.Event;
import com.theinside.partii.enums.EventStatus;
import com.theinside.partii.enums.EventType;
import com.theinside.partii.enums.EventVisibility;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for dynamic Event queries.
 * Provides type-safe, composable predicates for searching events.
 */
public class EventSpecifications {

    /**
     * Creates a Specification from EventSearchRequest.
     * Only non-null fields are included in the query.
     */
    public static Specification<Event> fromSearchRequest(EventSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter to PUBLIC visibility for search
            predicates.add(cb.equal(root.get("visibility"), EventVisibility.PUBLIC));

            // Event types
            if (request.eventTypes() != null && !request.eventTypes().isEmpty()) {
                predicates.add(root.get("eventType").in(request.eventTypes()));
            }

            // Statuses
            if (request.statuses() != null && !request.statuses().isEmpty()) {
                predicates.add(root.get("status").in(request.statuses()));
            }

            // Date range
            if (request.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), request.startDate()));
            }
            if (request.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), request.endDate()));
            }

            // Budget range
            if (request.minBudget() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("estimatedBudget"), request.minBudget()));
            }
            if (request.maxBudget() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("estimatedBudget"), request.maxBudget()));
            }

            // Location-based search (Haversine formula)
            if (request.latitude() != null && request.longitude() != null && request.radiusKm() != null) {
                // Haversine formula: distance = 6371 * acos(cos(lat1) * cos(lat2) * cos(lng2 - lng1) + sin(lat1) * sin(lat2))
                predicates.add(
                    cb.lessThanOrEqualTo(
                        cb.prod(
                            cb.literal(6371.0),
                            cb.function("acos", Double.class,
                                cb.sum(
                                    cb.prod(
                                        cb.function("cos", Double.class, cb.function("radians", Double.class, cb.literal(request.latitude()))),
                                        cb.prod(
                                            cb.function("cos", Double.class, cb.function("radians", Double.class, root.get("latitude"))),
                                            cb.function("cos", Double.class,
                                                cb.diff(
                                                    cb.function("radians", Double.class, root.get("longitude")),
                                                    cb.function("radians", Double.class, cb.literal(request.longitude()))
                                                )
                                            )
                                        )
                                    ),
                                    cb.prod(
                                        cb.function("sin", Double.class, cb.function("radians", Double.class, cb.literal(request.latitude()))),
                                        cb.function("sin", Double.class, cb.function("radians", Double.class, root.get("latitude")))
                                    )
                                )
                            )
                        ),
                        request.radiusKm()
                    )
                );
                // Ensure lat/lng are not null
                predicates.add(cb.isNotNull(root.get("latitude")));
                predicates.add(cb.isNotNull(root.get("longitude")));
            }

            // Keyword search (title, description, location)
            if (request.keyword() != null && !request.keyword().isBlank()) {
                String pattern = "%" + request.keyword().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                Predicate locMatch = cb.like(cb.lower(root.get("locationAddress")), pattern);
                predicates.add(cb.or(titleMatch, descMatch, locMatch));
            }

            // Age restriction
            if (request.maxAgeRestriction() != null) {
                predicates.add(
                    cb.or(
                        cb.isNull(root.get("ageRestriction")),
                        cb.lessThanOrEqualTo(root.get("ageRestriction"), request.maxAgeRestriction())
                    )
                );
            }

            // Available spots
            if (request.hasAvailableSpots() != null && request.hasAvailableSpots()) {
                predicates.add(cb.lessThan(root.get("currentAttendees"), root.get("maxAttendees")));
            }

            // Organizer
            if (request.organizerId() != null) {
                predicates.add(cb.equal(root.get("organizer").get("id"), request.organizerId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filter by event type.
     */
    public static Specification<Event> hasEventType(EventType eventType) {
        return (root, query, cb) -> eventType == null ? null : cb.equal(root.get("eventType"), eventType);
    }

    /**
     * Filter by status.
     */
    public static Specification<Event> hasStatus(EventStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    /**
     * Filter by visibility.
     */
    public static Specification<Event> hasVisibility(EventVisibility visibility) {
        return (root, query, cb) -> visibility == null ? null : cb.equal(root.get("visibility"), visibility);
    }

    /**
     * Filter events after a specific date.
     */
    public static Specification<Event> eventDateAfter(LocalDateTime date) {
        return (root, query, cb) -> date == null ? null : cb.greaterThan(root.get("eventDate"), date);
    }

    /**
     * Filter events before a specific date.
     */
    public static Specification<Event> eventDateBefore(LocalDateTime date) {
        return (root, query, cb) -> date == null ? null : cb.lessThan(root.get("eventDate"), date);
    }

    /**
     * Filter by organizer ID.
     */
    public static Specification<Event> hasOrganizer(Long organizerId) {
        return (root, query, cb) -> organizerId == null ? null : cb.equal(root.get("organizer").get("id"), organizerId);
    }

    /**
     * Public events only.
     */
    public static Specification<Event> isPublic() {
        return (root, query, cb) -> cb.equal(root.get("visibility"), EventVisibility.PUBLIC);
    }

    /**
     * Active events only.
     */
    public static Specification<Event> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), EventStatus.ACTIVE);
    }

    /**
     * Events with available spots.
     */
    public static Specification<Event> hasAvailableSpots() {
        return (root, query, cb) -> cb.lessThan(root.get("currentAttendees"), root.get("maxAttendees"));
    }
}
