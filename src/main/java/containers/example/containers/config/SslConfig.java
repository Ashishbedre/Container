package containers.example.containers.config;



import containers.example.containers.Service.Imp.DockerServiceImp;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;

@Configuration
public class SslConfig {

    @Value("${docker.cert.path}")
    private String certPath;

    @Value("${docker.key.path}")
    private String keyPath;

    @Value("${docker.ca-cert.path}")
    private String caCertPath;

    private static final Logger logger = LoggerFactory.getLogger(DockerServiceImp.class);
    @Bean
    public WebClient webClient(@Value("${docker.cert.path}") String certPath,
                               @Value("${docker.key.path}") String keyPath,
                               @Value("${docker.ca-cert.path}") String caCertPath) {

        logger.info("Cert path: {}", certPath);
        logger.info("Key path: {}", keyPath);
        logger.info("CA cert path: {}", caCertPath);

        File certFile = new File(certPath);
        File keyFile = new File(keyPath);
        File caCertFile = new File(caCertPath);

        logger.info("Cert file exists: {}", certFile.exists());
        logger.info("Key file exists: {}", keyFile.exists());
        logger.info("CA cert file exists: {}", caCertFile.exists());

        if (!certFile.exists() || !keyFile.exists() || !caCertFile.exists()) {
            throw new RuntimeException("One or more SSL certificate files not found");
        }

        // Create an SSL context with custom key and trust managers
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
                .keyManager(certFile, keyFile)
                .trustManager(caCertFile);

        HttpClient httpClient = HttpClient.create()
                .secure(sslSpec -> sslSpec.sslContext(sslContextBuilder));

        return  WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}


