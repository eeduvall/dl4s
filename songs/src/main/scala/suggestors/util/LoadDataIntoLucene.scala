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
import org.apache.lucene.document.IntPoint
import org.apache.lucene.document.DoublePoint
import org.apache.lucene.document.BinaryPoint
import java.io.{BufferedReader, InputStreamReader, FileInputStream}
import scala.io.StdIn
import de.siegmar.fastcsv.reader.CsvReader

object LoadDataIntoLucene {
    def main(args: Array[String]): Unit = {
        // readFromBillboard()
        // readFromH5ExtractedData()
        // readWikiExtractedData()
        readGeniusSongCsv()
    }

    def extractValue(jvalue: JValue): Any = jvalue match {
        case JString(s) => s
        case JInt(num) => num
        case JDouble(num) => num
        case JBool(b) => b
        case JArray(arr) => arr.map(extractValue)
        case JObject(obj) => obj.map { case (k, v) => (k, extractValue(v)) }
        case JNull => null
        case JNothing => null
    }

    def getField(name: String, jvalue: JValue): Field = jvalue match {
        case JString(s) => new TextField(name, s, Field.Store.YES)
        case JInt(num) => new IntPoint(name, num.toInt)
        case JDouble(num) => new DoublePoint(name, num.toDouble)
        case JBool(b) => new TextField(name, b.toString(), Field.Store.YES)
        case JArray(arr) => new TextField(name, arr.map(extractValue).mkString(", "), Field.Store.YES) //TODO this could be improved
        case JObject(obj) => new TextField(name, obj.map { case (k, v) => (k, extractValue(v)) }.map { case (k, v) => s"$k: $v" }.mkString(", "), Field.Store.YES)
        case JNull => null
        case JNothing => null
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

        
        
        val indexPath = sys.env("LUCENE_LOCATION") + "/" + indexName
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
                        doc.add(getField(name, value))
                        
                    }
                    indexWriter.addDocument(doc)
                }
            }
        }

        indexWriter.close()
    }



    def readFromH5ExtractedData(): Unit = {
        val filePath = "/Users/duvalle/Documents/GitHub/dl4s/songs/src/main/python/h5/collected_data.json"
        val jsonContent = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8)
        val jsonArray = parse(jsonContent).asInstanceOf[JArray]

        val indexName = "millionsong"
        val mapping = """{
            "properties": {
                "artist": { "type": "keyword" },
                "song": { "type": "keyword" },
                "artist_id": { "type": "text" },
                "idx_similar_artists": { "type": "integer" },
                "release": { "type": "text" },
                "song_id": { "type": "text" },
                "song_hotttnesss": { "type": "float" },
                "artist_familiarity": { "type": "float" },
                "artist_hotttnesss": { "type": "float" },
                "genre": { "type": "text" }
            }
        }"""

        val indexPath = sys.env("LUCENE_LOCATION") + "/" + indexName
        val analyzer = new StandardAnalyzer()
        val indexConfig = new IndexWriterConfig(analyzer)
        val indexDir = FSDirectory.open(Paths.get(indexPath))
        val indexWriter = new IndexWriter(indexDir, indexConfig)

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
                doc.add(getField(name, value))
            }
            indexWriter.addDocument(doc)
        }

        indexWriter.close()
    }

    def readWikiExtractedData(): Unit = {
        val filePath = "/Users/duvalle/Documents/GitHub/dl4s/songs/src/main/python/wiki-scraper/songs.json"
        val jsonContent = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8)
        val jsonArray = parse(jsonContent).asInstanceOf[JArray]

        val indexName = "wikisongs"
        val mapping = """{
            "properties": {
                "artist": { "type": "keyword" },
                "album": { "type": "keyword" },
                "song": { "type": "keyword" },
                "year": { "type": "integer" }
            }
        }"""

        val indexPath = sys.env("LUCENE_LOCATION") + "/" + indexName
        val analyzer = new StandardAnalyzer()
        val indexConfig = new IndexWriterConfig(analyzer)
        val indexDir = FSDirectory.open(Paths.get(indexPath))
        val indexWriter = new IndexWriter(indexDir, indexConfig)

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
                doc.add(getField(name, value))
            }
            indexWriter.addDocument(doc)
        }

        indexWriter.close()
    }

    def readGeniusSongCsv(): Unit = {
        val filePath = "/Users/duvalle/Documents/GitHub/dl4s/data/Songs/Kaggle-GeniusSongLyrics/song_lyrics.csv"
        val file = Paths.get(filePath)

        val indexName = "geniusSongs"
        val indexPath = sys.env("LUCENE_LOCATION") + "/" + indexName
        val analyzer = new StandardAnalyzer()
        val indexConfig = new IndexWriterConfig(analyzer)
        val indexDir = FSDirectory.open(Paths.get(indexPath))
        val indexWriter = new IndexWriter(indexDir, indexConfig)

        CsvReader.builder().ofCsvRecord(file).iterator().forEachRemaining { record =>
            
            if (record.getField(record.getFieldCount() - 1) == "en"){
                val doc = new Document()
                doc.add(new TextField("title", record.getField(0), Field.Store.YES))
                doc.add(new TextField("tag", record.getField(1), Field.Store.YES))
                doc.add(new TextField("artist", record.getField(2), Field.Store.YES))
                doc.add(new IntPoint("year", record.getField(3).toInt))
                doc.add(new TextField("features", record.getField(5), Field.Store.YES))
                // doc.add(new TextField("lyrics", record.getField(6), Field.Store.YES))
                doc.add(new IntPoint("id", record.getField(7).toInt))
                doc.add(new TextField("language", record.getField(10), Field.Store.YES))
                indexWriter.addDocument(doc)
            }
        }

        indexWriter.close()
    }
}