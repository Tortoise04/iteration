package com.smartschedule.repository;

import com.smartschedule.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    List<AiConversation> findByUserIdOrderByCreatedAtAsc(Long userId);
    List<AiConversation> findByUserIdAndSessionIdOrderByCreatedAtAsc(Long userId, String sessionId);
    void deleteByUserId(Long userId);

    @Query("SELECT c.sessionId FROM AiConversation c WHERE c.user.id = ?1 GROUP BY c.sessionId ORDER BY MAX(c.createdAt) DESC")
    List<String> findDistinctSessionIdsByUserId(Long userId);

    void deleteByUserIdAndSessionId(Long userId, String sessionId);
}
