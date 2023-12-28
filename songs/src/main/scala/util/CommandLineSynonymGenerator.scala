import scala.io.StdIn

object CommandlineSynonymGenerator {
    def main(args: Array[String]): Unit = {
        while (true) {
            print("Enter a word to generate synonyms: ")
            val input = StdIn.readLine()
            
            if (input.trim.split("\\s+").length > 1) {
                println("Please enter only one word.")
            } else {
                val synonyms = GenerateSynonyms.generateSynonyms(input)
                print("Synonyms are: ")
                synonyms.foreach(s => print(s + ", "))
                println("\n")
            }
        }
    }
}
