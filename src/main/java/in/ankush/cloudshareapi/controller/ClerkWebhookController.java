package in.ankush.cloudshareapi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.svix.Webhook;
import com.svix.exceptions.WebhookVerificationException;

import in.ankush.cloudshareapi.dto.ProfileDTO;
import in.ankush.cloudshareapi.service.ProfileService;
import in.ankush.cloudshareapi.service.UserCreditsService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class ClerkWebhookController {

    @Value("${clerk.webhook.secret}")
    private String webhookSecret;

    private final ProfileService profileService;
    private final UserCreditsService usersCreditsService;

    @PostMapping("/clerk")
    public ResponseEntity<?> handleClerkWebhook(
            @RequestHeader("svix-id") String svixId,
            @RequestHeader("svix-timestamp") String svixTimestamp,
            @RequestHeader("svix-signature") String svixSignature,
            @RequestBody String payload) {

        try {

            boolean isValid = verifyWebhooksSignature(
                    svixId,
                    svixTimestamp,
                    svixSignature,
                    payload
            );

            if (!isValid) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body("Invalid webhook signature");
            }

            ObjectMapper mapper = new ObjectMapper();

            JsonNode rootNode = mapper.readTree(payload);

            String eventType = rootNode.path("type").asText();

            switch (eventType) {

                case "user.created":
                    handleUserCreated(rootNode.path("data"));
                    break;

                case "user.updated":
                    handleUserUpdated(rootNode.path("data"));
                    break;

                case "user.deleted":
                    handleUserDeleted(rootNode.path("data"));
                    break;

                default:
                    System.out.println("Unhandled event type: " + eventType);
            }

            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    private void handleUserCreated(JsonNode data) {

        String clerkId = data.path("id").asText();

        String email = "";

        JsonNode emailAddresses = data.path("email_addresses");

        if (emailAddresses.isArray() && emailAddresses.size() > 0) {

            email = emailAddresses
                    .get(0)
                    .path("email_address")
                    .asText();
        }

        String firstName = data.path("first_name").asText("");

        String lastName = data.path("last_name").asText("");

        String photoUrl = data.path("image_url").asText("");

        ProfileDTO newProfile = ProfileDTO.builder()
                .clerkId(clerkId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .photoUrl(photoUrl)
                .build();

        profileService.createProfile(newProfile);

        usersCreditsService.createInitialCredits(clerkId);

        System.out.println("USER CREATED SUCCESSFULLY");
    }

    private void handleUserUpdated(JsonNode data) {

        String clerkId = data.path("id").asText();

        String email = "";

        JsonNode emailAddresses = data.path("email_addresses");

        if (emailAddresses.isArray() && emailAddresses.size() > 0) {

            email = emailAddresses
                    .get(0)
                    .path("email_address")
                    .asText();
        }

        String firstName = data.path("first_name").asText("");

        String lastName = data.path("last_name").asText("");

        String photoUrl = data.path("image_url").asText("");

        ProfileDTO updatedProfile = ProfileDTO.builder()
                .clerkId(clerkId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .photoUrl(photoUrl)
                .build();

        updatedProfile = profileService.updateProfile(updatedProfile);

        if (updatedProfile == null) {
            handleUserCreated(data);
        }

        System.out.println("USER UPDATED SUCCESSFULLY");
    }

    private void handleUserDeleted(JsonNode data) {

        String clerkId = data.path("id").asText();

        profileService.deleteProfile(clerkId);

        System.out.println("USER DELETED SUCCESSFULLY");
    }

    private boolean verifyWebhooksSignature(
            String svixId,
            String svixTimestamp,
            String svixSignature,
            String payload) {

        try {

            Webhook webhook = new Webhook(webhookSecret);

            webhook.verify(
                    payload,
                    svixId,
                    svixTimestamp,
                    svixSignature
            );

            return true;

        } catch (WebhookVerificationException e) {

            System.out.println("Webhook verification failed");

            return false;
        }
    }
}
