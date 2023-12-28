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
import org.json4s.Formats
import org.json4s.JsonDSL.string2jvalue
import org.json4s.jackson.JsonMethods._
import collection.JavaConverters.mapAsJavaMapConverter
import java.util.UUID

// @main def main: Unit =
// //   readFromDB()
//     readFromBillboard()

object LoadDataIntoElasticsearch {
    def main(args: Array[String]): Unit = {
        readFromBillboard()
    }

    def escapeBadCharacters(str: String): String = {
        str.replaceAll("\"", "\\\\\"")
        .replaceAll("\\\\'", "'")
        .replaceAll("\\p{Cntrl}", "")
    }

    def readFromBillboard(): Unit = {
        val folderPath = "/Users/duvalle/Documents/GitHub/dl4s/data/Songs/billboard/data/years" // Replace with the actual folder path
        
        val indexName = "billboard"
        val mapping = """{
            "properties": {
                "lyrics": { "type": "text" },
                "tags": { "type": "text" },
                "year": { "type": "integer" },
                "title": { "type": "keyword" },
                "artist": { "type": "keyword" }
            }
        }"""

        createIndex(mapping, indexName)

        val files = Files.list(Paths.get(folderPath)).toArray
        for (file <- files) {
            val filePath = file.toString
            if (filePath.endsWith(".json")) {
                val jsonContent = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8)
                val jsonArray = parse(jsonContent).asInstanceOf[JArray]
                
                jsonArray.arr.foreach { element =>
                    val stringed = compact(render(element))

                    //Only keep the fields that are in the mapping
                    val fields = (parse(mapping) \ "properties").asInstanceOf[JObject].obj
                    val elementObj = element.asInstanceOf[JObject]
                    val filteredElement = JObject(elementObj.obj.filter {
                        case JField(name, _) => fields.exists(_._1 == name)
                    })
                    val elementString = compact(render(filteredElement))

                    insertIntoIndex(indexName, elementString, None)
                }
            }
        }
    }

    def readFromDB(): Unit = {
        // Connect to the database
        val url = "jdbc:sqlite:/Users/duvalle/Documents/GitHub/dl4s/data/Songs/track_metadata.db"
        val connection: Connection = DriverManager.getConnection(url)

        // Define the mapping for the index
        val indexName = "songs"
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
        createIndex(mapping, indexName)

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
                insertIntoIndex(indexName, jsonResult.stripMargin.replaceAll("\n", ""), Some(resultSet.getString("track_id")))
                
            }
        } finally {
            // Close the connection
            connection.close()
        }
    }

    def createIndex(mapping: String, indexName: String): Unit = {
        val client = ElasticClient.getElasticSearchClient()

        try {
            // Check if the index exists
            val indexExistsRequest = new GetIndexRequest(indexName)
            val indexExistsResponse: Boolean = client.indices().exists(indexExistsRequest, RequestOptions.DEFAULT)

            // If the index does not exist, create it
            if (!indexExistsResponse) {
                val createIndexRequest = new CreateIndexRequest(indexName)
                client.indices().create(createIndexRequest, RequestOptions.DEFAULT)

                val request: PutMappingRequest = new PutMappingRequest(indexName)
                request.source(mapping, XContentType.JSON)
                val response: AcknowledgedResponse = client.indices().putMapping(request, RequestOptions.DEFAULT)
            }
        } finally {
            // Close the client
            client.close()
        }
    }

    def insertIntoIndex(indexName: String, jsonObject: String, docId: Option[String]): Unit = {
        val client = ElasticClient.getElasticSearchClient()

        try {
            // Check if the index exists
            val indexExistsRequest = new GetIndexRequest(indexName)
            val indexExistsResponse: Boolean = client.indices().exists(indexExistsRequest, RequestOptions.DEFAULT)

            if (!indexExistsResponse) {
                throw new Exception("Index does not exist")
            }

            // Insert the JSON object into the index
            val indexRequest = new IndexRequest(indexName)
                .source(jsonObject, XContentType.JSON)

            if docId != None then {
                indexRequest.id(docId.get)
            }
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
}