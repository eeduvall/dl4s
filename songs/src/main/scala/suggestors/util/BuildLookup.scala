import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.core.WhitespaceAnalyzer
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import org.apache.lucene.index.Term
import org.apache.lucene.search.suggest.Lookup
import org.apache.lucene.search.suggest.Lookup.LookupResult
import org.apache.lucene.search.suggest.analyzing.FuzzySuggester
import org.apache.lucene.search.suggest.analyzing.FreeTextSuggester
import org.apache.lucene.search.suggest.DocumentDictionary
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.Version
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.xcontent.XContentType
import java.nio.file.Paths
import org.deeplearning4j.nn.weights.WeightInit
// import org.deeplearning4j.optimize.api.Updater
// import org.deeplearning4j.nn.conf.Activation
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.{IUpdater, RmsProp}
import suggestors.models.CharLSTMNeuralLookup
import scala.collection.JavaConverters._

class BuildLookup {
    val lstmLayerSize = 100
    val miniBatchSize = 40
    val exampleLength = 1000
    val tbpttLength = 50
    val numEpochs = 10
    val noHiddenLayers = 1
    val learningRate = 0.1F
    val weightInit = WeightInit.XAVIER
    val updater = new RmsProp() //Updater.RMSPROP
    val activation = Activation.TANH

    val lookup = new CharLSTMNeuralLookup(lstmLayerSize, miniBatchSize, exampleLength, tbpttLength, numEpochs, noHiddenLayers, learningRate, weightInit, updater, activation)
    
    def buildLook(): Unit = {
        // val reader = DirectoryReader.open(FSDirectory.open(Paths.get(sys.env("DOCKER_BIND_MOUNT_LOCATION_717") + getIndexDirectory("billboard") + "/0/index/")))
        val reader = DirectoryReader.open(FSDirectory.open(Paths.get(sys.env("LUCENE_LOCATION") + "/billboard")))
        val dictionary = new DocumentDictionary(reader, "title", null)
        // // val lookup = new AnalyzingInfixLookup(Version.LATEST, new StandardAnalyzer())
        // val lookup = new FreeTextSuggester(new WhitespaceAnalyzer())

        lookup.build(dictionary)
    }

    def getIndexDirectory(indexName: String): String = {
        val request = new GetIndexRequest(indexName)
        val response = ElasticClient.getElasticSearch717Client().indices().get(request, RequestOptions.DEFAULT)
        val settings = response.getSettings.get(indexName)
        settings.get("index.uuid")
    }

    def getSuggestors(sequence: String): List[String] = {
        val results = lookup.lookup(sequence, null, false, 5)
        val suggestions = results.asScala.map(_.toString).toList
        suggestions
    }
}