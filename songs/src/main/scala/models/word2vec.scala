import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.deeplearning4j.models.word2vec.Word2Vec
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor._

object Word2VecGenerator {
    def main(args: Array[String]): Unit = {
        // Define the path to your text corpus
        val corpusPath = "/Users/duvalle/Documents/GitHub/dl4s/data/Songs/billboard/data/combined_lyrics.txt"
        // val path = Paths.get("/path/to/index")
        // val directory = FSDirectory.open(path)
        // val reader = DirectoryReader.open(directory)

        // Set up the sentence iterator
        val sentenceIterator = new BasicLineIterator(corpusPath)
        // val sentenceIterator = new FieldValuesSentenceIterator(reader, "text") //text indicates field where lyrics are stored

        // Set up the tokenizer
        val tokenizerFactory = new DefaultTokenizerFactory()
        // val tokenizerFactory = new BertWordPieceTokenizer("")
        tokenizerFactory.setTokenPreProcessor(new CommonPreprocessor())//new BertWordPiecePreProcessor()

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

        word2Vec.fit()

        // Save the Word2Vec model
        val modelPath = "/Users/duvalle/Documents/GitHub/dl4s/data/Songs/billboard/data/billboard-top100-model.bin"
        WordVectorSerializer.writeWord2VecModel(word2Vec, modelPath)
    }
}
