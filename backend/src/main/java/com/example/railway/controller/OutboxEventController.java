package com.example.railway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.domain.UserRole;
import com.example.railway.dto.OutboxDispatchResponse;
import com.example.railway.dto.OutboxEventPageResponse;
import com.example.railway.dto.OutboxEventResponse;
import com.example.railway.dto.OutboxEventSummaryResponse;
import com.example.railway.dto.OutboxRetryResponse;
import com.example.railway.security.RequiredRole;
import com.example.railway.service.outbox.OutboxEventDispatcher;
import com.example.railway.service.outbox.OutboxEventQueryService;

@RequiredRole(UserRole.ADMIN)
@RestController
@RequestMapping("/api/outbox-events")
public class OutboxEventController {

    private final OutboxEventQueryService outboxEventQueryService;
    private final OutboxEventDispatcher outboxEventDispatcher;

    public OutboxEventController(OutboxEventQueryService outboxEventQueryService,
                                 OutboxEventDispatcher outboxEventDispatcher) {
        this.outboxEventQueryService = outboxEventQueryService;
        this.outboxEventDispatcher = outboxEventDispatcher;
    }

    @GetMapping
    public OutboxEventPageResponse listEvents(@RequestParam(value = "status", required = false) String status,
                                              @RequestParam(value = "eventType", required = false) String eventType,
                                              @RequestParam(value = "page", required = false) Integer page,
                                              @RequestParam(value = "size", required = false) Integer size) {
        return outboxEventQueryService.listEvents(status, eventType, page, size);
    }

    @GetMapping("/summary")
    public OutboxEventSummaryResponse summary() {
        return outboxEventQueryService.summary();
    }

    @PostMapping("/dispatch")
    public OutboxDispatchResponse dispatch() {
        return new OutboxDispatchResponse(outboxEventDispatcher.dispatchOnce());
    }

    @PostMapping("/{id}/retry")
    public OutboxEventResponse retry(@PathVariable Long id) {
        return outboxEventQueryService.retryFailedEvent(id);
    }

    @PostMapping("/retry-failed")
    public OutboxRetryResponse retryFailed() {
        return new OutboxRetryResponse(outboxEventQueryService.retryFailedEvents());
    }
}
