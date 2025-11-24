package learning.routing;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

@SpringBootApplication
public class RoutingApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoutingApplication.class, args);
    }
    @Bean
    public CommandLineRunner testMongoConnection(MongoTemplate mongoTemplate) {
        return args -> {
            try {
                // Thử lấy tên database hiện tại
                String dbName = mongoTemplate.getDb().getName();
                System.out.println("✅ [MONGODB CHECK] Đã kết nối thành công tới DB: " + dbName);

                // Liệt kê các collection đang có
                System.out.println("📂 [MONGODB CHECK] Danh sách Collections: ");
                for (String name : mongoTemplate.getCollectionNames()) {
                    System.out.println("   - " + name);
                }
            } catch (Exception e) {
                System.err.println("❌ [MONGODB CHECK] KẾT NỐI THẤT BẠI!");
                e.printStackTrace();
            }
        };
    }
}
