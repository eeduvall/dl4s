import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.core.WhitespaceAnalyzer
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
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


object BuildLookup {
    def buildLook(): Unit = {
        val reader = DirectoryReader.open(FSDirectory.open(Paths.get("index")))
        val dictionary = new DocumentDictionary(reader, "title", "rating")
        // val lookup = new AnalyzingInfixLookup(Version.LATEST, new StandardAnalyzer())
        val lookup = new FreeTextSuggester(new WhitespaceAnalyzer())
        lookup.build(dictionary)
    }

    def getIndexDirectory(indexName: String, client: RestHighLevelClient): String = {
        val request = new GetIndexRequest(indexName)
        val response = client.indices().get(request, RequestOptions.DEFAULT)
        val settings = response.getSettings.get(indexName)
        settings.get("index.store.type")
    }
}