package com.nbjgroup.service;

import com.nbjgroup.entity.Payment;
import com.nbjgroup.entity.Payment.PaymentStatus;
import com.nbjgroup.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    public Page<Payment> getPaymentHistory(String tenantName, PaymentStatus status, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return paymentRepository.findAll((Specification<Payment>) (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tenantName != null && !tenantName.isEmpty()) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("tenant").get("user").get("firstName")), "%" + tenantName.toLowerCase() + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("tenant").get("user").get("lastName")), "%" + tenantName.toLowerCase() + "%")
                ));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("paymentDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("paymentDate"), endDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }
}