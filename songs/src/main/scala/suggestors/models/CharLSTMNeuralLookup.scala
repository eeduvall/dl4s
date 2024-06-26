package suggestors.models

import java.io.FileOutputStream
import java.nio.charset.Charset;
import java.nio.file.{Files, Path}
import org.apache.commons.io.FileUtils
import java.util.{ArrayList, List, Map}
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.apache.lucene.util.BytesRef
import org.apache.lucene.store.DataInput
import org.apache.lucene.store.DataOutput
import org.apache.lucene.search.suggest.InputIterator
import org.apache.lucene.search.suggest.Lookup
import org.apache.lucene.search.suggest.Lookup.LookupResult
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.IUpdater

class CharLSTMNeuralLookup(lstmLayerSize: Int, miniBatchSize: Int, exampleLength: Int, tbpttLength: Int,
                     numEpochs: Int, noOfHiddenLayers: Int, learningRate: Float, weightInit: WeightInit,
                     updater: IUpdater, activation: Activation) extends Lookup {

    var network: Option[MultiLayerNetwork] = None
    var characterIterator: Option[CharacterIterator] = None

    def getCount(): Long = {
        return -1L
    }

    override def build(inputIterator: InputIterator): Unit = {
      val tempFile = Files.createTempFile("chars", ".txt")
      val outputStream = new FileOutputStream(tempFile.toFile())
      
      
      var input = inputIterator.next()
      while (input != null) {
          outputStream.write(input.bytes)
          input = inputIterator.next()
      }
      outputStream.flush()
      outputStream.close()
      characterIterator = Some(new CharacterIterator(tempFile.toAbsolutePath().toString(), Charset.defaultCharset(), miniBatchSize, exampleLength))
      if (characterIterator == null) {
        throw new IllegalArgumentException("Input iterator is null")
      }
      network = Some(NeuralNetworksUtils.trainLSTM(lstmLayerSize, tbpttLength, numEpochs, noOfHiddenLayers,
          characterIterator.get, weightInit, updater, activation, new ScoreIterationListener(1000)))
      FileUtils.forceDeleteOnExit(tempFile.toFile())
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