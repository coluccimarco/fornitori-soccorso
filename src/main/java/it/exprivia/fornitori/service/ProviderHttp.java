package it.exprivia.fornitori.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Trusted administrator-configured destinations only; no redirects or unbounded bodies. */
final class ProviderHttp {
    private ProviderHttp() { }
    static RestClient.Builder client(String baseUrl) {
        URI base = URI.create(baseUrl);
        if (!Set.of("https", "http").contains(base.getScheme()) || base.getHost() == null || base.getUserInfo() != null)
            throw new IllegalArgumentException("Invalid provider configuration");
        var factory = new SimpleClientHttpRequestFactory() {
            @Override protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(8));
        return RestClient.builder().requestFactory(factory).requestInterceptor((request, body, execution) -> {
            ClientHttpResponse response = execution.execute(request, body);
            try {
                if (response.getStatusCode().is3xxRedirection()) throw new IOException("Provider redirect rejected");
                byte[] bytes = response.getBody().readNBytes(1_048_577);
                if (bytes.length > 1_048_576) throw new IOException("Provider response exceeds limit");
                return new ClientHttpResponse() {
                    private final InputStream input = new ByteArrayInputStream(bytes);
                    @Override public HttpStatusCode getStatusCode() throws IOException { return response.getStatusCode(); }
                    @Override public String getStatusText() throws IOException { return response.getStatusText(); }
                    @Override public HttpHeaders getHeaders() { return response.getHeaders(); }
                    @Override public InputStream getBody() { return input; }
                    @Override public void close() { response.close(); }
                };
            } catch (IOException | RuntimeException exception) {
                response.close();
                throw exception;
            }
        });
    }
}
