package com.nbjgroup.controller;

import com.nbjgroup.dto.PaymentHistoryDTO;
import com.nbjgroup.entity.Payment;
import com.nbjgroup.entity.Payment.PaymentStatus;
import com.nbjgroup.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/history")
    public ResponseEntity<Page<PaymentHistoryDTO>> getPaymentHistory(
            @RequestParam(required = false) String tenantName,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paymentDate,desc") String[] sort) {

        Sort sorting = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sorting);

        Page<Payment> payments = paymentService.getPaymentHistory(tenantName, status, startDate, endDate, pageable);
        Page<PaymentHistoryDTO> paymentHistoryDTOs = payments.map(PaymentHistoryDTO::new);

        return ResponseEntity.ok(paymentHistoryDTOs);
    }
}