package learning.routing.controller;

import learning.routing.config.RabbitConfig;
import learning.routing.model.JobDoc;
import learning.routing.model.JobStatus;
import learning.routing.repo.JobRepository;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CodeController {
    private final AmqpTemplate amqp;
    private static final Logger log = LoggerFactory.getLogger(CodeController.class);
    private final JobRepository jobRepo;

    // Giới hạn độ dài Output lưu vào DB (ví dụ 5000 ký tự ~ 5KB)
    private static final int MAX_OUTPUT_LENGTH = 5000;

    public CodeController(AmqpTemplate amqp, JobRepository jobRepo) {
        this.amqp = amqp;
        this.jobRepo = jobRepo;
    }

    @PostMapping("/intepret_solution")
    public ResponseEntity<?> intepret(@RequestBody Map<String, Object> body) {
        String id = UUID.randomUUID().toString();
        JobDoc doc = new JobDoc();
        doc.id = id;
        doc.lang = (String) body.getOrDefault("lang", "cpp");
        doc.questionId = (String) body.getOrDefault("question_id", null);
        doc.typedCode = (String) body.getOrDefault("typed_code", "");

        // TỐI ƯU 1: Nếu input rỗng, set NULL để MongoDB không lưu trường này (Tiết kiệm không gian)
        String rawInput = (String) body.getOrDefault("data_input", "");
        doc.dataInput = (rawInput == null || rawInput.isBlank()) ? null : rawInput;

        doc.state = JobStatus.PENDING;
        doc.createAt = Instant.now();
        doc.updateAt = System.currentTimeMillis();

        jobRepo.save(doc);
        try {
            // Chuẩn bị payload gửi Worker
            // Lưu ý: dataInput gửi sang worker vẫn phải là chuỗi (kể cả rỗng) để worker không bị NullPointer
            Map<String, Object> workerPayload = Map.of(
                    "id", doc.id,
                    "language", doc.lang,
                    "code", doc.typedCode,
                    "stdin", doc.dataInput == null ? "" : doc.dataInput
            );

            amqp.convertAndSend(RabbitConfig.QUEUE, workerPayload);
            log.info("Enqueued job {} lang={}", doc.id, doc.lang);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return ResponseEntity.ok(Map.of("interpret_id", doc.id, "state", "PENDING"));
    }

    @GetMapping("check/{id}")
    public ResponseEntity<?> check(@PathVariable String id) {
        Optional<JobDoc> o = jobRepo.findById(id);
        if (o.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        JobDoc doc = o.get();
        if (doc.state == JobStatus.PENDING || doc.state == JobStatus.RUNNING) {
            return ResponseEntity.ok(Map.of("state", doc.state.name()));
        }
        return ResponseEntity.ok(doc.result);
    }

    @PostMapping("/callback")
    public ResponseEntity<?> callback(@RequestBody Map<String, Object> payload) {
        if (payload == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing payload"));
        }

        // Tạo bản sao Mutable (có thể sửa đổi) của payload để xử lý cắt gọt
        Map<String, Object> resultMap = new HashMap<>(payload);

        String id = null;
        if (resultMap.containsKey("id")) id = String.valueOf(resultMap.get("id"));
        if ((id == null || id.isBlank()) && resultMap.containsKey("interpret_id"))
            id = String.valueOf(resultMap.get("interpret_id"));

        if (id == null || id.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing id"));
        }

        String finalId = id;
        JobDoc doc = jobRepo.findById(id).orElseGet(() -> {
            JobDoc j = new JobDoc();
            j.id = finalId;
            j.createAt = Instant.now();
            return j;
        });

        // --- TỐI ƯU 2: CẮT NGẮN OUTPUT (Quan trọng nhất) ---
        if (resultMap.containsKey("output")) {
            String out = (String) resultMap.get("output");
            if (out != null && out.length() > MAX_OUTPUT_LENGTH) {
                // Cắt bớt và thêm cảnh báo
                String truncated = out.substring(0, MAX_OUTPUT_LENGTH)
                        + "\n... [Output truncated because it is too long]";
                resultMap.put("output", truncated);
            }
        }

        // --- TỐI ƯU 3: XÓA DỮ LIỆU TRÙNG LẶP ---
        // Worker có thể gửi lại code hoặc input trong callback, nhưng ta đã có trong JobDoc rồi.
        // Xóa đi để không lưu 2 lần.
        resultMap.remove("code");
        resultMap.remove("typed_code");
        resultMap.remove("stdin");
        resultMap.remove("input");

        // Lưu kết quả đã tối ưu vào doc
        doc.result = resultMap;

        // Xử lý trạng thái
        Object statusObj = resultMap.get("status");
        if (statusObj != null) {
            String s = String.valueOf(statusObj).toUpperCase();
            if(s.equals("DONE")) doc.state = JobStatus.SUCCESS;
            else if(s.equals("TIMEOUT")) doc.state = JobStatus.FAIL;
            else doc.state = JobStatus.FAIL;
        } else {
            doc.state = JobStatus.SUCCESS;
        }

        doc.updateAt = System.currentTimeMillis();
        jobRepo.save(doc); // Lưu vào MongoDB
        return ResponseEntity.ok(Map.of("ok", true));
    }
}