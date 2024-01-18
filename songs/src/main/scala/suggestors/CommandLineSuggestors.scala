import scala.io.StdIn

object CommandlineSuggestors {
    def main(args: Array[String]): Unit = {
        // println(BuildLookup.getIndexDirectory("billboard"))
        BuildLookup.buildLook()

        // while (true) {
            // print("Enter a word or phrase to see the search suggestors: ")
            // val input = StdIn.readLine()
            
            // val suggestors = GeneratorSuggestors.generateSuggestors(input)
            // print("Suggestions are: ")
            // synonyms.foreach(s => println(s))
            // println("\n")
        // }
    }
}
