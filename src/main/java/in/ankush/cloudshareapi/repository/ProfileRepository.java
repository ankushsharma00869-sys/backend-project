package in.ankush.cloudshareapi.repository;

import in.ankush.cloudshareapi.document.ProfileDocument;
import in.ankush.cloudshareapi.dto.ProfileDTO;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ProfileRepository extends MongoRepository<ProfileDocument, String> {
    Optional<ProfileDocument>findByEmail(String email);
    ProfileDocument findByClerkId(String clerkId);
    boolean existsByClerkId(String clerkId);
}
