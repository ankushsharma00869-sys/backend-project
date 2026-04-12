package in.ankush.cloudshareapi.controller;

import in.ankush.cloudshareapi.document.PaymentTransaction;
import in.ankush.cloudshareapi.document.ProfileDocument;
import in.ankush.cloudshareapi.repository.PaymentTransactionRepository;
import in.ankush.cloudshareapi.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ProfileService profileService;

    public TransactionController(PaymentTransactionRepository paymentTransactionRepository, ProfileService profileService) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<?> getUserTransaction(){
        ProfileDocument currentProfile = profileService.getCurrentProfile();

        String clerkId = currentProfile.getClerkId();

        List<PaymentTransaction>transactions = paymentTransactionRepository.findByClerkIdOrderByTransactionDateDesc(clerkId, "SUCCESS");
        return  ResponseEntity.ok(transactions);
    }
}
