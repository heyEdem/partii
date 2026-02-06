package com.theinside.partii.dto;

import lombok.*;

/**
 * Request DTO for blocking a user.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockUserRequest {

    private String reason;
}
