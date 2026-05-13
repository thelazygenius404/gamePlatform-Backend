package ma.emsi.game_platform_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableMongoAuditing    // active @CreatedDate et @LastModifiedDate sur les entités
@EnableScheduling       // active @Scheduled sur les services (processExpirations, refreshPlays30d)
public class MongoConfig {
}