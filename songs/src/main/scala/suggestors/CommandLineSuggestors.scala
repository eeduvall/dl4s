import scala.io.StdIn
import suggestors.util.BuildLookup

object CommandlineSuggestors {
    def main(args: Array[String]): Unit = {
        // println(BuildLookup.getIndexDirectory("billboard"))
        val lookupUtil = new BuildLookup()
        lookupUtil.buildLook()

        while (true) {
            print("Enter a word or phrase to see the search suggestors: ")
            val input = StdIn.readLine()
            
            // val suggestors = GeneratorSuggestors.generateSuggestors(input)
            val suggestors = lookupUtil.getSuggestors(input)
            print("Suggestions are: ")
            suggestors.foreach(s => println(s))
            println("\n")
        }
    }
}
