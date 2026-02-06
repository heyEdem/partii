package com.theinside.partii.repository;

import com.theinside.partii.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserBlock entity.
 */
@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    /**
     * Find a block where blocker blocked blocked.
     */
    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /**
     * Check if a specific block exists.
     */
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /**
     * Find all users blocked by a specific user.
     */
    List<UserBlock> findByBlockerId(Long blockerId);

    /**
     * Find all users who have blocked a specific user.
     */
    List<UserBlock> findByBlockedId(Long blockedId);

    /**
     * Count users blocked by a specific user.
     */
    long countByBlockerId(Long blockerId);

    /**
     * Count users who have blocked a specific user.
     */
    long countByBlockedId(Long blockedId);

    /**
     * Check if two users have blocked each other (mutual block).
     */
    @Query("""
        SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END
        FROM UserBlock ub1
        WHERE (ub1.blocker.id = :userId1 AND ub1.blocked.id = :userId2)
        AND EXISTS (
            SELECT 1 FROM UserBlock ub2
            WHERE ub2.blocker.id = :userId2 AND ub2.blocked.id = :userId1
        )
        """)
    boolean isMutuallyBlocked(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * Check if user1 is blocked by user2.
     */
    default boolean isBlockedBy(Long userId1, Long userId2) {
        return existsByBlockerIdAndBlockedId(userId2, userId1);
    }

    /**
     * Check if user1 has blocked user2.
     */
    default boolean hasBlocked(Long userId1, Long userId2) {
        return existsByBlockerIdAndBlockedId(userId1, userId2);
    }

    /**
     * Delete block between two users.
     */
    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /**
     * Delete all blocks involving a specific user.
     */
    void deleteByBlockerIdOrBlockedId(Long blockerId, Long blockedId);
}
