import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Document, Field, FieldType}
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.FSDirectory
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.charset.StandardCharsets
import org.json4s._
import org.json4s.jackson.JsonMethods._
// import org.apache.lucene.document.NumericField
import org.apache.lucene.document.TextField

object LoadDataIntoLucene {
    def main(args: Array[String]): Unit = {
        readFromBillboard()
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
                "artist": { "type": "keyword" },
                "sentiment": {
                    "type": "nested",
                    "properties": {
                        "neg": { "type": "float" },
                        "neu": { "type": "float" },
                        "pos": { "type": "float" },
                        "compound": { "type": "float" }
                    }
                }
            }
        }"""

        
        
        val indexPath = sys.env("LUCENE_LOCATION") + "/billboard"
        val analyzer = new StandardAnalyzer()
        val indexConfig = new IndexWriterConfig(analyzer)
        val indexDir = FSDirectory.open(Paths.get(indexPath))
        val indexWriter = new IndexWriter(indexDir, indexConfig)

        // Add documents to the index
        // Example:
        // val doc = new Document()
        // doc.add(new Field("field_name", "field_value", fieldType))
        // indexWriter.addDocument(doc)

        val files = Files.list(Paths.get(folderPath)).toArray
        for (file <- files) {
            val filePath = file.toString
            if (filePath.endsWith(".json")) {
                val jsonContent = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8)
                val jsonArray = parse(jsonContent).asInstanceOf[JArray]
                
                jsonArray.arr.foreach { element =>
                    val stringed = compact(render(element))

                    //Only keep the fields that are in the mapping
                    val json = parse(mapping)
                    val props = json.values.asInstanceOf[Map[String, Any]].get("properties").get.asInstanceOf[Map[String, Any]]
                    val propsChildren = props.keys.toList
                    val elementObj = element.asInstanceOf[JObject]
                    val filteredElement = JObject(elementObj.obj.filter {
                        case JField(name, _) => propsChildren.contains(name)
                    })
                    val elementString = compact(render(filteredElement))
                    val doc = new Document()
                    val fields = parse(elementString).asInstanceOf[JObject].obj
                    fields.foreach { case (name, value) =>
                        // if (value.isInstanceOf[Double] || value.isInstanceOf[Float]) {
                        //     System.out.println("Adding numeric field")
                        //     doc.add(new NumericField(name, value.toString, None))
                        // } else {
                        //     System.out.println("Adding string field")
                        //     doc.add(new TextField(name, value.toString, Field.Store.YES))
                        // }
                        doc.add(new TextField(name, value.toString, Field.Store.YES))
                    }
                    indexWriter.addDocument(doc)
                }
            }
        }

        indexWriter.close()
    }
}