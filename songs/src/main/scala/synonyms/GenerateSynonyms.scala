import collection.JavaConverters.iterableAsScalaIterableConverter
import org.deeplearning4j.models.word2vec.Word2Vec
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer

object GenerateSynonyms {
    // Load pre-trained Word2Vec model
    val word2VecModel = WordVectorSerializer.readWord2VecModel("/Users/duvalle/Documents/GitHub/dl4s/data/Songs/billboard/data/billboard-top100-model.bin")

    def generateSynonyms(inputString: String): Array[String] = {
        // Tokenize input string
        val tokens = inputString.split(" ")

        // Generate synonyms for each token
        val synonyms = tokens.flatMap { token =>
            word2VecModel.similarWordsInVocabTo(token, 0.85).asScala //0.85 is the accuracy
        }

        synonyms
    }
}
