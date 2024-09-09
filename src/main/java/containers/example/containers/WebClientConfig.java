package containers.example.containers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.tcp.SslProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;


@Configuration
public class WebClientConfig {

    @Value("${cert.path}")
    private Resource certPath; // Path to client-cert.pem

    @Value("${key.path}")
    private Resource keyPath; // Path to client-key.pem

//    @Bean
//    public WebClient.Builder webClientBuilder() throws Exception {
//        // Use Netty's SslContextBuilder to load PEM files directly
//        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
//                .keyManager(certPath.getInputStream(), keyPath.getInputStream());
//
//        // Configure HttpClient to use the SslContext
//        HttpClient httpClient = HttpClient.create()
//                .secure(sslContextSpec -> {
//                    try {
//                        sslContextSpec.sslContext(sslContextBuilder.build());
//                    } catch (SSLException e) {
//                        throw new RuntimeException(e);
//                    }
//                });
//
//        // Return the configured WebClient builder
//        return WebClient.builder()
//                .clientConnector(new ReactorClientHttpConnector(httpClient));
//    }
}


