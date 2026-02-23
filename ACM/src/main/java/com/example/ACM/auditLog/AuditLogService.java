package com.example.ACM.auditLog;

import com.example.ACM.repository.AuditLogRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepo auditLogRepo;

    @Async
    public void log(String action, String entityType, String entityId, Long performedBy, String details) {
        try {
            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .performedBy(performedBy)
                    .details(details)
                    .build();
            auditLogRepo.save(entry);
        } catch (Exception e) {
            log.error("Failed to save audit log: action={}, entity={}", action, entityType, e);
        }
    }
}
