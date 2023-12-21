import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.deeplearning4j.models.word2vec.Word2Vec
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory

object Word2VecGenerator {
    def main(args: Array[String]): Unit = {
        // Define the path to your text corpus
        val corpusPath = "/path/to/your/text/corpus.txt"
        // val path = Paths.get("/path/to/index")
        // val directory = FSDirectory.open(path)
        // val reader = DirectoryReader.open(directory)

        // Set up the sentence iterator
        val sentenceIterator = new BasicLineIterator(corpusPath)
        // val sentenceIterator = new FieldValuesSentenceIterator(reader, "text") //text indicates field where lyrics are stored

        // Set up the tokenizer
        val tokenizerFactory = new DefaultTokenizerFactory()

        // Train the Word2Vec model
        val word2Vec = new Word2Vec.Builder()
            .minWordFrequency(5)
            .iterations(5)
            .layerSize(100)
            .seed(42)
            .windowSize(5)
            .iterate(sentenceIterator)
            .tokenizerFactory(tokenizerFactory)
            .build()
        // val vec = new Word2Vec.Builder()
        //     .layerSize(100)
        //     .windowSize(5)
        //     .iterate(sentenceIterator)
        //     .build()

        word2Vec.fit()

        // Save the Word2Vec model
        val modelPath = "/path/to/save/word2vec/model.bin"
        WordVectorSerializer.writeWord2VecModel(word2Vec, modelPath)
    }
}
