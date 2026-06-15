package com.example.railway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.domain.UserRole;
import com.example.railway.dto.TicketChangePageResponse;
import com.example.railway.dto.TicketChangeResponse;
import com.example.railway.security.RequiredRole;
import com.example.railway.service.TicketChangeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Ticket Change Tracking", description = "Ticket change record query and management tracking APIs")
@RequiredRole({UserRole.ADMIN, UserRole.OPERATOR, UserRole.RISK_OFFICER})
@RestController
@RequestMapping("/api/ticket-changes")
public class TicketChangeController {

    private final TicketChangeService ticketChangeService;

    public TicketChangeController(TicketChangeService ticketChangeService) {
        this.ticketChangeService = ticketChangeService;
    }

    @Operation(summary = "Query ticket change records")
    @GetMapping
    public TicketChangePageResponse list(@RequestParam(value = "status", required = false) String status,
                                         @RequestParam(value = "changeNo", required = false) String changeNo,
                                         @RequestParam(value = "userId", required = false) Long userId,
                                         @RequestParam(value = "page", required = false) Integer page,
                                         @RequestParam(value = "size", required = false) Integer size) {
        return ticketChangeService.adminListChanges(status, changeNo, userId, page, size);
    }

    @Operation(summary = "Query ticket change detail")
    @GetMapping("/{id}")
    public TicketChangeResponse detail(@PathVariable("id") Long id) {
        return ticketChangeService.adminChangeDetail(id);
    }
}
