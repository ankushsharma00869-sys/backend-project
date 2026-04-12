package in.ankush.cloudshareapi.repository;

import in.ankush.cloudshareapi.document.PaymentTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentTransactionRepository extends MongoRepository<PaymentTransaction, String> {

   List<PaymentTransaction> findByClerkId(String clerkId);

  List<PaymentTransaction> findByClerkIdOrderByTransactionDateDesc(String clerkId, String status );
 List<PaymentTransaction> findByClerkIdAndStatusOrderByTransactionDateDesc(String clerkId ,String status );

}
