package learning.routing.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document("jobs")
public class JobDoc {
    @Id
    public String id;
    public String lang;
    public String questionId;
    public String typedCode;
    public String dataInput;         // raw payload testcases
    public JobStatus state = JobStatus.PENDING;
    public Instant createAt = Instant.now();

    public Map<String, Object> result;
    public List<Map<String, Object>> tests;

    public long updateAt = System.currentTimeMillis();
}
