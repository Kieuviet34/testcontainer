package learning.routing.controller;

import learning.routing.config.RabbitConfig;
import learning.routing.model.JobDoc;
import learning.routing.model.JobStatus;
import learning.routing.repo.JobRepository;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;


@RestController
@RequestMapping("/api")
public class CodeController {
    private final AmqpTemplate amqp;
    private static final Logger log = (Logger) LoggerFactory.getLogger(CodeController.class);
    private final JobRepository jobRepo;

    public CodeController(AmqpTemplate amqp, JobRepository jobRepo) {
        this.amqp = amqp;
        this.jobRepo = jobRepo;
    }
    @PostMapping("/intepret_solution")
    public ResponseEntity<?> intepret(@RequestBody Map<String, Object> body){
        String id = UUID.randomUUID().toString();
        JobDoc doc = new JobDoc();
        doc.id = id;
        doc.lang = (String) body.getOrDefault("lang", "cpp");
        doc.questionId = (String) body.getOrDefault("question_id", null);
        doc.typedCode = (String) body.getOrDefault("typed_code", "");
        doc.dataInput = (String) body.getOrDefault("data_input", "");
        doc.state = JobStatus.PENDING;
        doc.createAt = Instant.now();
        doc.updateAt = System.currentTimeMillis();

        jobRepo.save(doc);
        try {
            amqp.convertAndSend(RabbitConfig.QUEUE, doc.id);
            log.info("Enqueued job {} lang={}");
        } catch (Exception ex) {
            ex.printStackTrace();
            // still return interpret id, worker may be unavailable
        }

        return ResponseEntity.ok(Map.of("interpret_id", doc.id, "state", "PENDING"));
    }
    @GetMapping("check/{id}")
    public ResponseEntity<?>check(@PathVariable String id){
        Optional<JobDoc> o = jobRepo.findById(id);
        if(o.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        JobDoc doc = o.get();
        if(doc.state == JobStatus.PENDING || doc.state == JobStatus.RUNNING) {
            return ResponseEntity.ok(Map.of("state", doc.state.name()));
        }
        return ResponseEntity.ok(doc.result);
    }
    @PostMapping("/callback")
    public ResponseEntity<?> callback(@RequestBody Map<String, Object> payload){
        if(payload == null){
            return ResponseEntity.badRequest().body(Map.of("error", "missing payload"));
        }
        String id = null;
        if (payload.containsKey("id")) id = String.valueOf(payload.get("id"));
        if ((id == null || id.isBlank()) && payload.containsKey("interpret_id"))
            id = String.valueOf(payload.get("interpret_id"));
        if ((id == null || id.isBlank()) && payload.containsKey("jobId"))
            id = String.valueOf(payload.get("jobId"));

        if (id == null || id.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing id in payload"));
        }

        String finalId = id;
        JobDoc doc = jobRepo.findById(id).orElseGet(() -> {
            JobDoc j = new JobDoc();
            j.id = finalId;
            j.createAt = Instant.now();
            return j;
        });
        Map<String, Object> resultMap = (Map<String, Object>)payload;
        doc.result = resultMap;
        Object stateObj = payload.get("state");
        if (stateObj != null) {
            try {
                doc.state = JobStatus.valueOf(String.valueOf(stateObj).toUpperCase());
            } catch (Exception ignored) {
                // fallback below
            }
        }
        if (doc.state == JobStatus.PENDING || doc.state == JobStatus.RUNNING) {
            // try to infer from status_code or status_msg
            Object sc = payload.get("status_code");
            if (sc instanceof Number) {
                int code = ((Number) sc).intValue();
                if (code == 10) doc.state = JobStatus.SUCCESS;
                else doc.state = JobStatus.FAIL;
            } else if (payload.containsKey("status_msg")) {
                String msg = String.valueOf(payload.get("status_msg")).toLowerCase();
                if (msg.contains("accepted") || msg.contains("ok") || msg.contains("success")) doc.state = JobStatus.SUCCESS;
                else doc.state = JobStatus.FAIL;
            } else {
                // default mark success for safety
                doc.state = JobStatus.SUCCESS;
            }
        }
        doc.updateAt = System.currentTimeMillis();
        jobRepo.save(doc);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
