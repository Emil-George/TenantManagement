package com.nbjgroup.controller;

import com.nbjgroup.entity.User;
import com.nbjgroup.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.annotation.PostConstruct;
import java.util.Map;

@RestController
@RequestMapping("/stripe" )
public class StripeController {

//    @Value("${stripe.secret-key:}")
    private String stripeSecretKey = "sk_test_51SFFTo0JWONq8U474tzNLPALRnAdzIvtYlfiUlaPpCINzBJ4UADS1TNyKPwoRkKjaYfqjt14UAGaRiJRYTsci72F00J8jWZ7XJ";

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }


    @Autowired
    private UserRepository userRepository; // Inject the user repository

    @PostMapping("/create-connect-account")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createConnectAccount() {
        try {
            // 1. Get the currently logged-in admin's email
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            User adminUser = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Admin user not found."));

            // 2. Create the Stripe Connect Account
            AccountCreateParams params = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)
                    .setCountry("US") // This should ideally be configurable
                    .setEmail(adminUser.getEmail())
                    .setCapabilities(
                            AccountCreateParams.Capabilities.builder()
                                    .setCardPayments(AccountCreateParams.Capabilities.CardPayments.builder().setRequested(true).build())
                                    .setTransfers(AccountCreateParams.Capabilities.Transfers.builder().setRequested(true).build())
                                    .build()
                    )
                    .build();

            Account account = Account.create(params);

            // 3. CRUCIAL: Save the Stripe Account ID to your database
            adminUser.setStripeAccountId(account.getId());
            userRepository.save(adminUser);

            // 4. Create the Onboarding Link
            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                    .setAccount(account.getId())
                    .setRefreshUrl("http://localhost:5173/admin/settings" ) // Redirect here if link expires
                    .setReturnUrl("http://localhost:5173/admin/settings" ) // Redirect here after successful onboarding
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();

            AccountLink accountLink = AccountLink.create(linkParams);

            // 5. Return the URL to the frontend
            return ResponseEntity.ok(Map.of("onboardingUrl", accountLink.getUrl()));

        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", true, "message", e.getMessage()));
        }
    }
}
