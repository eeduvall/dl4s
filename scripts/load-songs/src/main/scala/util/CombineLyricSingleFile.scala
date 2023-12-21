import java.io.{File, PrintWriter}

object CombineLyricSingleFile {
    def main(args: Array[String]): Unit = {
        val sourceFolder = new File("/Users/duvalle/Documents/GitHub/dl4s/data/Songs/lyrics-master/database")
        val outputFile = new File("/Users/duvalle/Documents/GitHub/dl4s/data/Songs/combined_lyrics.txt")

        val writer = new PrintWriter(outputFile)

        try {
            processFolder(sourceFolder, writer)
        } finally {
            writer.close()
        }

        println("Text files combined successfully!")
    }

    def processFolder(folder: File, writer: PrintWriter): Unit = {
        val files = folder.listFiles()

        if (files != null) {
            for (file <- files) {
                if (file.isDirectory) {
                    processFolder(file, writer)
                } else if (file.isFile && file.getName.endsWith(".txt")) {
                    val text = scala.io.Source.fromFile(file).mkString
                    writer.println(text)
                    writer.println("\n\n")
                }
            }
        }
    }
}
