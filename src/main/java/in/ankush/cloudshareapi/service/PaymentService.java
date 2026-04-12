package in.ankush.cloudshareapi.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import in.ankush.cloudshareapi.document.PaymentTransaction;
import in.ankush.cloudshareapi.document.ProfileDocument;
import in.ankush.cloudshareapi.dto.PaymentDTO;
import in.ankush.cloudshareapi.dto.PaymentVerificationDTO;
import in.ankush.cloudshareapi.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor

public class PaymentService {

    private final ProfileService profileService;
    private final UserCreditsService userCreditsService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")

    private String razorPayKeySecret;


    public PaymentDTO createOrder(PaymentDTO paymentDTO) {
        try {
            // Current user
            ProfileDocument currentProfile = profileService.getCurrentProfile();
            String clerkId = currentProfile.getClerkId();

            // Razorpay client
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorPayKeySecret);

            // Amount decide backend करेगा (SECURE)
            int amount = 0;
            switch (paymentDTO.getPlanId()) {
                case "premium":
                    amount = 50000;   // ₹500
                    break;
                case "ultimate":
                    amount = 250000;  // ₹2500
                    break;
                default:
                    throw new RuntimeException("Invalid plan");
            }

            // Order request
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_" + System.currentTimeMillis());

            // Create order
            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id").toString();

            // Save transaction in DB
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .clerkId(clerkId)
                    .orderId(orderId)
                    .planId(paymentDTO.getPlanId())
                    .amount(amount)
                    .status("PENDING")
                    .transactionDate(LocalDateTime.now())
                    .userEmail(currentProfile.getEmail())
                    .userName(currentProfile.getFirstName() + " " + currentProfile.getLastName())
                    .build();

            paymentTransactionRepository.save(transaction);

            // ✅ FINAL RESPONSE (IMPORTANT 🔥)
            return PaymentDTO.builder()
                    .orderId(orderId)
                    .amount(amount)     // 🔥 YAHI LINE MISSING THI
                    .success(true)
                    .message("Order created successfully")
                    .build();

        } catch (Exception e) {
            e.printStackTrace();

            return PaymentDTO.builder()
                    .success(false)
                    .message("Error creating order: " + e.getMessage())
                    .build();
        }
    }

    public PaymentDTO verifyPayment(PaymentVerificationDTO request){
        try {
              ProfileDocument currentProfile =  profileService.getCurrentProfile();
              String clerkId = currentProfile.getClerkId();

            String data = request.getRazorpay_order_id() + "|"+request.getRazorpay_payment_id();
           String generatedSignature = generateHmacSha256Signature(data,razorPayKeySecret);
               if (!generatedSignature.equals(request.getRazorpay_signature())){
                   updateTransactionStatus(request.getRazorpay_order_id(), "FAILED", request.getRazorpay_payment_id(),null);
                   return PaymentDTO.builder()
                           .success(false)
                           .message("Payment signature verification failed")
                           .build();
               }
               //Add credits based on plan

               int creditsToAdd =0;
               String plan = "BASIC";

               switch (request.getPlanId()){
                   case "premium":
                       creditsToAdd=500;
                       plan="PREMIUM";
                       break;
                   case "ultimate":
                       creditsToAdd=5000;
                       plan="ULTIMATE";
                       break;
               }
               if (creditsToAdd > 0){
                   userCreditsService.addCredits(clerkId,creditsToAdd,plan);
                   updateTransactionStatus(request.getRazorpay_order_id(), "SUCCESS", request.getRazorpay_payment_id(), creditsToAdd);
                   return PaymentDTO.builder()
                           .success(true)
                           .message("Payment verified and credits added successfully")
                           .credits(userCreditsService.getUserCredits(clerkId).getCredits())
                           .build();

               }
               else {
                   updateTransactionStatus(request.getRazorpay_order_id(), "FAILED", request.getRazorpay_payment_id(),null );
                   return PaymentDTO.builder()
                           .success(false)
                           .message("Invalid plan selected ")
                           .build();
               }

        }catch (Exception e){
            try {
                updateTransactionStatus(request.getRazorpay_order_id(),"ERROR", request.getRazorpay_payment_id(),null );

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            return PaymentDTO.builder()
                    .success(false)
                    .message("ERROR verifying payment:" +e.getMessage())
                    .build();

        }
    }

    private void updateTransactionStatus(String razorpayOrderId, String status, String razorpayPaymentId, Integer creditsToAdd) {
        paymentTransactionRepository.findAll().stream()
                .filter(t ->t.getOrderId() != null && t.getOrderId().equals(razorpayOrderId))
                .findFirst()
                .map(transaction -> {
                    transaction.setStatus(status);
                    transaction.setPaymentId(razorpayPaymentId);
                    if (creditsToAdd != null){
                        transaction.setCreditsAdded(creditsToAdd);
                    }
                    return paymentTransactionRepository.save(transaction);
                })
                .orElse(null);
    }

    private String generateHmacSha256Signature(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey =
                    new SecretKeySpec(secret.getBytes(), "HmacSHA256");

            mac.init(secretKey);

            byte[] hash = mac.doFinal(data.getBytes());

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String s = Integer.toHexString(0xff & b);
                if (s.length() == 1) hex.append('0');
                hex.append(s);
            }

            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error generating signature", e);
        }
    }


}
