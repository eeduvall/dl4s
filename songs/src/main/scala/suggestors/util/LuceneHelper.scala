import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.store.FSDirectory
import java.nio.file.Paths

object LuceneHelper {
    def main(args: Array[String]): Unit = {
        // val indexPath = sys.env("LUCENE_LOCATION") + "/billboard"
        val indexPath = sys.env("LUCENE_LOCATION") + "/wikisongs"

        val directory = FSDirectory.open(Paths.get(indexPath))
        val reader = DirectoryReader.open(directory)

        val numDocs = reader.numDocs()

        println(s"Number of documents in the index: $numDocs")

        reader.close()
        directory.close()
    }
}