import java.io.{File, PrintWriter}
import scala.io.Source
import org.json4s._
import org.json4s.DefaultFormats._
import org.json4s.jackson.JsonMethods._

object CombineLyricSingleFile {
    def main(args: Array[String]): Unit = {
        val sourceFolder = new File("/Users/duvalle/Documents/GitHub/dl4s/data/Songs/billboard/data/years")
        val outputFile = new File("/Users/duvalle/Documents/GitHub/dl4s/data/Songs/billboard/data/combined_lyrics.txt")

        val writer = new PrintWriter(outputFile)

        try {
            processFolder(sourceFolder, writer)
        } finally {
            writer.close()
        }

        println("Text files combined successfully!")
    }

    def processFolder(folder: File, writer: PrintWriter): Unit = {
        implicit val formats = DefaultFormats
        val files = folder.listFiles()

        if (files != null) {
            for (file <- files) {
                println(s"Processing ${file.getName}...")
                if (file.isDirectory) {
                    processFolder(file, writer)
                } else if (file.isFile && file.getName.endsWith(".json")) {
                    val jsonString = Source.fromFile(file).mkString
                    val json = parse(jsonString)
                    val lyricsList = (json \ "lyrics").children.map(_.extract[String])
                    lyricsList.foreach { lyrics =>
                        writer.println(lyrics)
                        writer.println("\n\n")
                    }
                }
            }
        }
    }
}
