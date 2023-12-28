import java.nio.file.{Files, Paths}
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.io.ByteArrayInputStream
import javax.net.ssl.SSLContext
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.ssl.SSLContexts
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient


object ElasticClient {
    def getElasticSearchClient(): RestHighLevelClient = {
        val elasticPassword: String = sys.env("ELASTIC_PASSWORD")
        // Create a CredentialsProvider and set the basic authentication credentials
        val credentialsProvider: CredentialsProvider = new BasicCredentialsProvider()
        credentialsProvider.setCredentials(
            AuthScope.ANY,
            new UsernamePasswordCredentials("elastic", elasticPassword)
        )

        // Load the certificate
        val certPath = "/Users/duvalle/Documents/GitHub/dl4s/http_ca.crt"
        val certContent = Files.readAllBytes(Paths.get(certPath))

        // Create a KeyStore and add the certificate to it
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
        keyStore.load(null, null)
        keyStore.setCertificateEntry(
            "cert",
            CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(certContent))
        )

        // Create an SSLContext that uses the KeyStore
        val sslContext = SSLContexts.custom()
            .loadTrustMaterial(keyStore, new TrustSelfSignedStrategy())
            .build()

        // Create a REST client to connect to Elasticsearch
        new RestHighLevelClient(
            RestClient
                .builder(new HttpHost("localhost", 9200, "https"))
                .setHttpClientConfigCallback(
                    httpClientBuilder =>
                        httpClientBuilder
                            .setDefaultCredentialsProvider(credentialsProvider)
                            .setSSLContext(sslContext)
                )
        )
    }
}
