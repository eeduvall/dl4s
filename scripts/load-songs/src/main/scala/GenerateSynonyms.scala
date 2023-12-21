import collection.JavaConverters.iterableAsScalaIterableConverter
import org.deeplearning4j.models.word2vec.Word2Vec
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer


def generateSynonyms(inputString: String): Array[String] = {
    // Load pre-trained Word2Vec model
    val word2VecModel = WordVectorSerializer.readWord2VecModel("path/to/word2vec/model.bin")

    // Tokenize input string
    val tokens = inputString.split(" ")

    // Generate synonyms for each token
    val synonyms = tokens.flatMap { token =>
        word2VecModel.similarWordsInVocabTo(token, 0.5).asScala //0.5 is the accuracy
    }

    synonyms
}
