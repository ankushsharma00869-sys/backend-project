package in.ankush.cloudshareapi.service;


import in.ankush.cloudshareapi.document.UserCredits;
import in.ankush.cloudshareapi.repository.UserCreditsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCreditsService {
    private  final UserCreditsRepository userCreditsRepository;
    private final  ProfileService profileService;
    public UserCredits createInitialCredits(String clerkId){
        UserCredits userCredit = UserCredits.builder()
                .clerkId(clerkId)
                .credits(10)
                .plan("BASIC")
                .build();

        return  userCreditsRepository.save(userCredit);

    }

    public UserCredits getUserCredits(String clerkId){
        return userCreditsRepository
                .findByClerkId(clerkId)
                .orElseGet(() -> createInitialCredits(clerkId));
    }

    public UserCredits getUserCredits(){
        String clerkId = profileService.getCurrentProfile().getClerkId();
        return getUserCredits(clerkId);
    }
   public Boolean hashEnoughCredits(int requiredCredits){
        UserCredits userCredits = getUserCredits();
        return userCredits.getCredits() >= requiredCredits;
   }


   public UserCredits consumeCredit(){
        UserCredits userCredits = getUserCredits();

        if (userCredits.getCredits() <= 0){
            return null;
        }
        userCredits.setCredits(userCredits.getCredits() - 1);
        return userCreditsRepository.save(userCredits);
   }

   public UserCredits addCredits(String clerkId, Integer creditsToAdd, String plan){
      UserCredits userCredits =  userCreditsRepository.findByClerkId(clerkId)
                .orElseGet(() -> createInitialCredits(clerkId));

      userCredits.setCredits(userCredits.getCredits() + creditsToAdd);
      userCredits.setPlan(plan);
      return userCreditsRepository.save(userCredits);
   }
}
