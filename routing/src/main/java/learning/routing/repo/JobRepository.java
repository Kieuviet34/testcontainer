package learning.routing.repo;

import learning.routing.model.JobDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface JobRepository extends MongoRepository<JobDoc, String> {
}
