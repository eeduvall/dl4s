package suggestors.models

import java.util.Base64
import java.io.File
// import java.io.{File, IOException, InputStream}
import java.io.InputStream
import java.util.LinkedList
import scala.util.Random
import scala.collection.mutable.{Map, ListBuffer}
import scala.collection.JavaConverters._
import scala.util.control.Breaks._
import org.slf4j.LoggerFactory

import org.apache.commons.io.IOUtils
import org.deeplearning4j.models.embeddings.WeightLookupTable
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.deeplearning4j.models.sequencevectors.sequence.SequenceElement
import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.{BackpropType, NeuralNetConfiguration}
import org.deeplearning4j.nn.conf.layers.{LSTM, RnnOutputLayer}
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.api.TrainingListener
import org.deeplearning4j.util.ModelSerializer
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.lossfunctions.LossFunctions
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.IUpdater

object NeuralNetworksUtils {
    val log = LoggerFactory.getLogger(getClass)

    val rng = new Random()

    var d: Double = 0.0

    def sampleFromNetwork(network: MultiLayerNetwork, characterIterator: CharacterIterator, initialization: String, numSamples: Int, eosChar: Character): Map[String, Double] = {
        var init = initialization
        if (initialization == null) {
            init = String.valueOf(characterIterator.convertIndexToCharacter(rng.nextInt(characterIterator.inputColumns())))
        }

        val sb = Array.fill(numSamples)(new StringBuilder)
        for (i <- 0 until numSamples) {
            sb(i) = new StringBuilder(init)
        }

        val initializationInput = encodeInput(characterIterator, init, numSamples)

        network.rnnClearPreviousState()
        var output = network.rnnTimeStep(initializationInput);
        output = output.tensorAlongDimension(output.size(2).toInt - 1, 1, 0)


        val charactersToSample = 40
        val probs = ListBuffer.fill(numSamples)(0.0)
        for (i <- 0 until charactersToSample) {
            val nextInput = Nd4j.zeros(numSamples, characterIterator.inputColumns())
            breakable {
                for (s <- 0 until numSamples) {
                    val outputProbDistribution = Array.ofDim[Double](characterIterator.totalOutcomes())
                    for (j <- outputProbDistribution.indices) {
                        outputProbDistribution(j) = output.getDouble(Integer(s), Integer(j))
                    }
                    val sampledCharacterIdx = sampleFromDistribution(outputProbDistribution)
                    probs += d
                    nextInput.putScalar(Array(s, sampledCharacterIdx), 1.0f)
                    val c = characterIterator.convertIndexToCharacter(sampledCharacterIdx)
                    if (eosChar != null && eosChar == c) {
                        break
                    }
                    sb(s).append(c)
                }
            }

            output = network.rnnTimeStep(nextInput);
        }

        val out = Map[String, Double]()
        for (i <- 0 until numSamples) {
        out.put(sb(i).toString(), probs(i))
        }
        return out
  }


    def encodeInput(characterIterator: CharacterIterator, initialization: String, numSamples: Int): INDArray = {
        val initializationInput = Nd4j.zeros(numSamples, characterIterator.inputColumns(), initialization.length())
        val init = initialization.toCharArray()
        for (i <- 0 until init.length) {
            val idx = characterIterator.convertCharacterToIndex(init(i))
            for (j <- 0 until numSamples) {
                initializationInput.putScalar(Array(j, idx, i), 1.0f)
            }
        }
        initializationInput
    }

    def sampleFromDistribution(distribution: Array[Double]): Int = {
        d = 0.0
        var sum = 0.0
        var result = -1
        
        breakable {
            for (t <- 0 until 10) {
                d = rng.nextDouble()
                sum = 0.0
                for (i <- 0 until distribution.length) {
                    sum += distribution(i)
                    if (d <= sum) {
                        result = i
                        break()
                    }
                }
            }
        }
        
        if (result == -1) {
            throw new IllegalArgumentException("Distribution is invalid? d=" + d + ", sum=" + sum)
        }

        result
    }

    def buildLSTM(noOfHiddenLayers: Int, lstmLayerSize: Int, tbpttLength: Int, ioSize: Int, weightInit: WeightInit, updater: IUpdater, activation: Activation): MultiLayerConfiguration = {
        var builder = new NeuralNetConfiguration.Builder()
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .seed(12345)
            .l2(0.001)
            .weightInit(weightInit)
            .updater(updater)
            .list()
            .layer(0, new LSTM.Builder().nIn(ioSize).nOut(lstmLayerSize)
                .activation(activation).build())

        for (i <- 1 until noOfHiddenLayers) {
            builder = builder.layer(i, new LSTM.Builder().nIn(lstmLayerSize).nOut(lstmLayerSize)
                .activation(activation).build())
        }

        builder.layer(noOfHiddenLayers, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT).activation(Activation.SOFTMAX)
            .nIn(lstmLayerSize).nOut(ioSize).build())
            .backpropType(BackpropType.TruncatedBPTT).tBPTTForwardLength(tbpttLength).tBPTTBackwardLength(tbpttLength)//.pretrain(false).backprop(true)
            //Despite being referenced in 2.1 docs, neither method exists https://deeplearning4j.konduit.ai/deeplearning4j/reference/multi-layer-network
            //Explanation of what these are here: https://www.baeldung.com/deeplearning4j
            .build()

        builder.build()
    }

    def trainLSTM(lstmLayerSize: Int, tbpttLength: Int, numEpochs: Int, noOfHiddenLayers: Int, iter: CharacterIterator, weightInit: WeightInit, updater: IUpdater, activation: Activation, listeners: TrainingListener*): MultiLayerNetwork = {
        val conf = buildLSTM(noOfHiddenLayers, lstmLayerSize, tbpttLength, iter.inputColumns(), weightInit, updater, activation)

        val name = s"$lstmLayerSize-$tbpttLength-$numEpochs-$noOfHiddenLayers-$weightInit-$updater-$activation"

        val net = new MultiLayerNetwork(conf)
        net.init()
        net.setListeners(listeners: _*)

        log.info(s"params : ${net.numParams()}, examples: ${iter.totalExamples()}")

        trainAndSave(numEpochs, iter, name, net)
        net
    }

    private def trainAndSave(numEpochs: Int, iter: CharacterIterator, name: String, net: MultiLayerNetwork): Unit = {
        var miniBatchNumber = 0
        val generateSamplesEveryNMinibatches = 300
        for (_ <- 0 until numEpochs) {
            while (iter.hasNext()) {
                val next = iter.next()
                net.fit(next)
                if ({ miniBatchNumber += 1; miniBatchNumber } % generateSamplesEveryNMinibatches == 0) {
                    val samples = sampleFromNetwork(net, iter, "latest trends\n", 3, '\n').keys.toArray
                    for (j <- samples.indices) {
                        log.info(s"----- Sample $j -----")
                        log.info(samples(j))
                    }
                }
            }
        }
        val locationToSave = new File(s"target/charLSTM-$name-${net.numParams()}-${iter.totalExamples()}.zip")
        assert(locationToSave.createNewFile())
        ModelSerializer.writeModel(net, locationToSave, true)
   }

    def generateInputs(input: String): List[String] = {
        val inputs = new LinkedList[String]()
        for (i <- 1 until input.length()) {
            inputs.add(input.substring(0, i))
        }
        inputs.add(input)
        inputs.asScala.toList
    }

    def readLookupTable[T <: SequenceElement](stream: InputStream): WeightLookupTable[T] = {
        val weightLookupTable = new InMemoryLookupTable[T]()
        var headerRead = false
        for (line <- IOUtils.readLines(stream, "UTF-8").asScala) {
            val tokens = line.split(" ")
            if (!headerRead) {
                // reading header as "NUM_WORDS VECTOR_SIZE NUM_DOCS"
                val numWords = tokens(0).toInt
                val layerSize = tokens(1).toInt
                val totalNumberOfDocs = tokens(2).toInt
                log.debug(s"Reading header - words: $numWords, layerSize: $layerSize, totalNumberOfDocs: $totalNumberOfDocs")
                headerRead = true
            }

            // val label = WordVectorSerializer.decodeB64(tokens(0))
            val label = new String(Base64.getDecoder.decode(tokens(0)), "UTF-8")
            val vector = Nd4j.create(tokens.length - 1)
            if (label != null && vector != null) {
                for (i <- 1 until tokens.length) {
                    vector.putScalar(i - 1, tokens(i).toDouble)
                }
                weightLookupTable.putVector(label, vector)
            }
        }
        stream.close()
        weightLookupTable
    }
}