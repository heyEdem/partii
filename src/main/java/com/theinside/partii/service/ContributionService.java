package com.theinside.partii.service;

import com.theinside.partii.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ContributionService {

    // CRUD
    ContributionItemResponse createItem(Long eventId, Long organizerId, CreateContributionItemRequest request);
    Page<ContributionItemResponse> listItems(Long eventId, Long userId, String status, String category, String type, String priority, Pageable pageable);
    ContributionItemResponse getItem(Long eventId, Long itemId, Long userId);
    ContributionItemResponse updateItem(Long eventId, Long itemId, Long organizerId, UpdateContributionItemRequest request);
    void deleteItem(Long eventId, Long itemId, Long organizerId);

    // Lifecycle
    ContributionItemResponse claimItem(Long eventId, Long itemId, Long userId);
    ContributionItemResponse confirmItem(Long eventId, Long itemId, Long organizerId);
    ContributionItemResponse assignItem(Long eventId, Long itemId, Long organizerId, Long assigneeId);
    ContributionItemResponse acceptAssignment(Long eventId, Long itemId, Long userId);
    ContributionItemResponse declineAssignment(Long eventId, Long itemId, Long userId);
    ContributionItemResponse releaseItem(Long eventId, Long itemId, Long organizerId);
    ContributionItemResponse completeItem(Long eventId, Long itemId, Long organizerId);

    // Read-only
    ContributionSummaryResponse getSummary(Long eventId, Long userId);
    List<String> getCategories(Long eventId, Long userId);
    List<ContributionItemResponse> getMyContributions(Long userId);
}
