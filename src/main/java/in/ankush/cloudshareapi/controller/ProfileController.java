package in.ankush.cloudshareapi.controller;

import in.ankush.cloudshareapi.dto.ProfileDTO;
import in.ankush.cloudshareapi.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProfileController {
private  final ProfileService profileService;
@PostMapping("/profile/register")
public ResponseEntity<?> registerProfile(@RequestBody ProfileDTO profileDTO){
    HttpStatus status = profileService.existsByClerkId(profileDTO.getClerkId()) ? HttpStatus.OK : HttpStatus.CREATED;
    ProfileDTO savedProfile= profileService.createProfile(profileDTO);
      return ResponseEntity.status(status).body(savedProfile);
}
}
