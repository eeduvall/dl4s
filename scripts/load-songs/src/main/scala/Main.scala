import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import org.apache.http.HttpHost
import org.apache.http.ssl.SSLContexts
import org.apache.http.ssl.TrustStrategy
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.CredentialsProvider
import org.apache.http.impl.client.BasicCredentialsProvider
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.client.indices.GetIndexResponse
import org.elasticsearch.client.indices.PutMappingRequest
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import org.elasticsearch.action.support.master.AcknowledgedResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.xcontent.XContentType
import java.sql.{Connection, DriverManager, ResultSet}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import collection.JavaConverters.mapAsJavaMapConverter

@main def main: Unit =
  readFromDB()

def escapeBadCharacters(str: String): String = {
    str.replaceAll("\"", "\\\\\"")
    .replaceAll("\\\\'", "'")
    .replaceAll("\\p{Cntrl}", "")
}

def readFromDB(): Unit = {
    // Connect to the database
    val url = "jdbc:sqlite:/Users/duvalle/Documents/GitHub/dl4s/data/track_metadata.db"
    val connection: Connection = DriverManager.getConnection(url)

    try {
        // Execute a query to retrieve the entries
        val statement = connection.createStatement()
        val resultSet: ResultSet = statement.executeQuery("SELECT track_id, title, song_id, release as album, artist_id, artist_name, duration, year, artist_familiarity FROM songs")

        // Iterate over the entries
        while (resultSet.next()) {
            // Create a JSON object using XContentBuilder
            val jsonResult = f"""{
                |"track_id":"${escapeBadCharacters(resultSet.getString("track_id"))}",
                |"title":"${escapeBadCharacters(resultSet.getString("title"))}",
                |"song_id":"${escapeBadCharacters(resultSet.getString("song_id"))}",
                |"album":"${escapeBadCharacters(resultSet.getString("album"))}",
                |"artist_id":"${escapeBadCharacters(resultSet.getString("artist_id"))}",
                |"artist_name":"${escapeBadCharacters(resultSet.getString("artist_name"))}",
                |"duration":"${escapeBadCharacters(resultSet.getString("duration"))}",
                |"year":"${escapeBadCharacters(resultSet.getString("year"))}",
                |"artist_familiarity":"${escapeBadCharacters(resultSet.getString("artist_familiarity"))}"
              |}"""
            
            // Process the JSON object by inserting into Elasticsearch index
            insertIntoIndex(jsonResult.stripMargin.replaceAll("\n", ""), resultSet.getString("track_id"))
        }
    } finally {
        // Close the connection
        connection.close()
    }
}


def insertIntoIndex(jsonObject: String, docId: String): Unit = {
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
    keyStore.setCertificateEntry("cert", CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(certContent)))

    // Create an SSLContext that uses the KeyStore
    val sslContext = SSLContexts.custom()
        .loadTrustMaterial(keyStore, new TrustSelfSignedStrategy())
        .build()


    // Create a REST client to connect to Elasticsearch
    val client = new RestHighLevelClient(
        RestClient.builder(new HttpHost("localhost", 9200, "https"))
        .setHttpClientConfigCallback(httpClientBuilder => httpClientBuilder
            .setDefaultCredentialsProvider(credentialsProvider)
            .setSSLContext(sslContext))
    )

    val indexName = "songs"
    try {
        // Check if the index exists
        val indexExistsRequest = new GetIndexRequest(indexName)
        val indexExistsResponse: Boolean = client.indices().exists(indexExistsRequest, RequestOptions.DEFAULT)

        // If the index does not exist, create it
        if (!indexExistsResponse) {
            val createIndexRequest = new CreateIndexRequest(indexName)
            client.indices().create(createIndexRequest, RequestOptions.DEFAULT)

            // Define the mapping for the index
            val mapping = """{
                "properties": {
                    "track_id": { "type": "keyword" },
                    "title": { "type": "text" },
                    "song_id": { "type": "keyword" },
                    "album": { "type": "text" },
                    "artist_id": { "type": "keyword" },
                    "artist_name": { "type": "text" },
                    "duration": { "type": "float" },
                    "year": { "type": "integer" },
                    "artist_familiarity": { "type": "float" }
                }
            }"""

            val request: PutMappingRequest = new PutMappingRequest(indexName)
            request.source(mapping, XContentType.JSON)
            val response: AcknowledgedResponse = client.indices().putMapping(request, RequestOptions.DEFAULT)
        }

        // Insert the JSON object into the index
        val indexRequest = new IndexRequest(indexName)
            .id(docId)
            .source(jsonObject, XContentType.JSON)
        try {
          client.index(indexRequest, RequestOptions.DEFAULT)
        } catch {
          case e: java.io.IOException => {}
          case e: Exception => throw e
        }
    } finally {
        // Close the client
        client.close()
    }
}