package detectatron;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
public class Application extends AsyncConfigurerSupport {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

    /**
     * Configure our specific max limits around pool size and amount of requests we can queue.
     *
     * @return
     */
     @Override
     public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(25);
        executor.setMaxPoolSize(25);
        executor.setQueueCapacity(5000);
        executor.setThreadNamePrefix("DetectatronAsync-");
        executor.initialize();
        return executor;
     }
}
