package suggestors.models

import java.io.FileOutputStream
import java.nio.charset.Charset;
import java.nio.file.{Files, Path}
import org.apache.commons.io.FileUtils
import java.util.{ArrayList, List, Map}
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.deeplearning4j.ui.api.UIServer
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage
import org.deeplearning4j.ui.model.stats.StatsListener
import org.apache.lucene.util.BytesRef
import org.apache.lucene.store.DataInput
import org.apache.lucene.store.DataOutput
import org.apache.lucene.search.suggest.InputIterator
import org.apache.lucene.search.suggest.Lookup
import org.apache.lucene.search.suggest.Lookup.LookupResult
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.IUpdater
import org.apache.tika.langdetect.OptimaizeLangDetector
import org.apache.tika.language.detect.LanguageResult
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.analysis.{TokenStream, Analyzer, CharArraySet}
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.core.StopFilter
import java.io.StringReader
import scala.collection.mutable.ArrayBuffer

class CharLSTMNeuralLookup(lstmLayerSize: Int, miniBatchSize: Int, exampleLength: Int, tbpttLength: Int,
                     numEpochs: Int, noOfHiddenLayers: Int, learningRate: Float, weightInit: WeightInit,
                     updater: IUpdater, activation: Activation) extends Lookup {

    var network: Option[MultiLayerNetwork] = None
    var characterIterator: Option[CharacterIterator] = None

    def getCount(): Long = {
        return -1L
    }

    override def build(inputIterator: InputIterator): Unit = {

      var tempFile = Files.createTempFile("chars", ".txt")
      println("Temp file created " + tempFile.toAbsolutePath())
      //Check if a temp file exists, if so, use it
      var newTempFile = true
      val tempFiles = tempFile.getParent.toFile.listFiles((dir, name) => name.startsWith("chars") && name.endsWith(".txt"))


      if (tempFiles != null && tempFiles.length > 1) {
        var reusedFile = tempFiles(0)
        for (file <- tempFiles) {
          if (file.getAbsolutePath() != tempFile.toFile().getAbsolutePath()) {
            reusedFile = file
            newTempFile = false
          }
        }
        FileUtils.forceDelete(tempFile.toFile())
        tempFile = reusedFile.toPath()
      }
      //END
      println("Temp file location is:  " + tempFile.toAbsolutePath().toString() + " newTempFile is: " + newTempFile)
      if (newTempFile) {
        val outputStream = new FileOutputStream(tempFile.toFile())
        
        val detector = new OptimaizeLangDetector()
        detector.loadModels()
        var songCount = 0
        var input = inputIterator.next()
        while (input != null && songCount < 180000) {
            //convert BytesRef to string, remove any parentheses info, trim, and add newline character
            val inputString = new String(input.bytes, Charset.defaultCharset())
            val result: LanguageResult = detector.detect(inputString)
            if (result.getLanguage == "en" && result.getRawScore >= 0.99) {
              if (!inputString.toLowerCase().contains("chapter")) {
                val cleanedString = if (inputString.toLowerCase().contains("feat.")) {
                  inputString.toLowerCase().split("feat.")(0).replaceAll("\\(.*?\\)", "").trim() + "\n"
                } else {
                  inputString.toLowerCase().replaceAll("\\(.*?\\)", "").trim() + "\n"
                }
                // val cleanedString = inputString.replaceAll("\\(.*?\\)", "").trim().toLowerCase()// + "\n"
                // ...

                // val analyzer: Analyzer = new EnglishAnalyzer()
                // val tokenStream = analyzer.tokenStream(null, cleanedString)

                //variant - just remove stop words
                // val stopWords: CharArraySet = EnglishAnalyzer.getDefaultStopSet
                // val result: ArrayBuffer[String] = ArrayBuffer()
                // cleanedString.split(" ").foreach({ word =>
                //   if (!stopWords.contains(word)) {
                //     result += word
                //   }})


                // val filteredStream: TokenStream = new StopFilter(tokenStream, stopWords)
                // val term: CharTermAttribute = filteredStream.addAttribute(classOf[CharTermAttribute])
                // filteredStream.reset()

                // var result: scala.collection.immutable.List[String] = scala.collection.immutable.List()
                // while (filteredStream.incrementToken()) {
                //   result = result :+ term.toString
                // }

                // filteredStream.end()
                // filteredStream.close()
                // analyzer.close()

                // Now you can use the 'tokens' list containing the extracted tokens
                // val combinedString = result.mkString(" ") + "\n"
                val cleanedBytes = cleanedString.getBytes(Charset.defaultCharset())
                outputStream.write(cleanedBytes)
                songCount += 1
              }
            }
            //
            // outputStream.write(input.bytes)
            input = inputIterator.next()
        }
        outputStream.flush()
        outputStream.close()
      }
      characterIterator = Some(new CharacterIterator(tempFile.toAbsolutePath().toString(), Charset.defaultCharset(), miniBatchSize, exampleLength))
      if (characterIterator == null) {
        throw new IllegalArgumentException("Input iterator is null")
      }

      var uiServer = UIServer.getInstance()
      var statsStorage = new InMemoryStatsStorage()
      uiServer.attach(statsStorage)
      network = Some(NeuralNetworksUtils.trainLSTM(lstmLayerSize, tbpttLength, numEpochs, noOfHiddenLayers,
          characterIterator.get, weightInit, updater, activation, new ScoreIterationListener(1000), new StatsListener(statsStorage)))
      // FileUtils.forceDeleteOnExit(tempFile.toFile())
    }

    override def lookup(key: CharSequence, contexts: java.util.Set[BytesRef], onlyMorePopular: Boolean, num: Int): List[LookupResult] = {
        val results = new ArrayList[LookupResult]()
        if (characterIterator == null) {
        throw new IllegalArgumentException("Input iterator is null")
      }
        val output = NeuralNetworksUtils.sampleFromNetwork(network.get,
            characterIterator.get, key.toString(), num, null)
        for ((key, value) <- output) {
          results.add(new LookupResult(key, value.toLong))
        }
        return results
    }


    def store(x$0: DataOutput): Boolean = {
      false
    }

    def load(x$0: DataInput): Boolean = {
      false
    }
    
    def ramBytesUsed(): Long = {
        0
    }
}