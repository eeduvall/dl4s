package suggestors.models

import java.util.{Map, HashMap}
import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import suggestors.models.CharacterIterator
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.{BackpropType, NeuralNetConfiguration}
import org.deeplearning4j.nn.conf.layers.{LSTM, RnnOutputLayer}
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.api.TrainingListener
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.lossfunctions.LossFunctions
// import org.nd4j.linalg.api.ndarray.INDArray
// import org.nd4j.linalg.factory.Nd4j
// import org.nd4j.linalg.lookup.{InMemoryLookupTable, WeightLookupTable}
// import org.nd4j.linalg.util.{IOUtils, WordVectorSerializer}
import org.slf4j.LoggerFactory
// import java.io.{File, IOException, InputStream}
// import java.util.{LinkedList, List}
// import scala.collection.JavaConverters._

import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.IUpdater

object NeuralNetworksUtils {
    def sampleFromNetwork(network: MultiLayerNetwork, characterIterator: CharacterIterator, initialization: String, numSamples: Int, eosChar: Character): Map[String, Double] = {
        //TODO convert to Scala
        return new HashMap[String, Double]()
    // if (initialization == null) {
    //   initialization = String.valueOf(characterIterator.convertIndexToCharacter(rng.nextInt(characterIterator.inputColumns())));
    // }

    // StringBuilder[] sb = new StringBuilder[numSamples];
    // for (int i = 0; i < numSamples; i++) {
    //   sb[i] = new StringBuilder(initialization);
    // }

    // INDArray initializationInput = encodeInput(characterIterator, initialization, numSamples);

    // network.rnnClearPreviousState();
    // INDArray output = network.rnnTimeStep(initializationInput);
    // output = output.tensorAlongDimension((int)output.size(2) - 1, 1, 0);

    // int charactersToSample = 40;
    // List<Double> probs = new ArrayList<>(numSamples);
    // for (int i = 0; i < charactersToSample; i++) {
    //   INDArray nextInput = Nd4j.zeros(numSamples, characterIterator.inputColumns());
    //   for (int s = 0; s < numSamples; s++) {
    //     double[] outputProbDistribution = new double[characterIterator.totalOutcomes()];
    //     for (int j = 0; j < outputProbDistribution.length; j++) {
    //       outputProbDistribution[j] = output.getDouble(s, j);
    //     }
    //     int sampledCharacterIdx = sampleFromDistribution(outputProbDistribution);
    //     probs.add(d);
    //     nextInput.putScalar(new int[] {s, sampledCharacterIdx}, 1.0f);
    //     char c = characterIterator.convertIndexToCharacter(sampledCharacterIdx);
    //     if (eosChar != null && eosChar == c) {
    //       break;
    //     }
    //     sb[s].append(c);
    //   }

    //   output = network.rnnTimeStep(nextInput);
    // }

    // Map<String, Double> out = new HashMap<>();
    // for (int i = 0; i < numSamples; i++) {
    //   out.put(sb[i].toString(), probs.get(i));
    // }
    // return out;
  }

    private val log = LoggerFactory.getLogger(getClass)

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

    //TODO Convert to scala
    private def trainAndSave(numEpochs: Int, iter: CharacterIterator, name: String, net: MultiLayerNetwork): Unit = {
//         var miniBatchNumber = 0
//         val generateSamplesEveryNMinibatches = 300
//         for (_ <- 0 until numEpochs) {
//             while (iter.hasNext()) {
//                 val next = iter.next()
//                 net.fit(next)
//                 if ({ miniBatchNumber += 1; miniBatchNumber } % generateSamplesEveryNMinibatches == 0) {
//                     val samples = sampleFromNetwork(net, iter, "latest trends\n", 3, '\n').keySet().toArray(new Array[String](3))
//                     for (j <- samples.indices) {
//                         log.info(s"----- Sample $j -----")
//                         log.info(samples(j))
//                     }
//                 }
//             }
//         }
//         val locationToSave = new File(s"target/charLSTM-$name-${net.numParams()}-${iter.numExamples()}.zip")
//         assert(locationToSave.createNewFile())
//         ModelSerializer.writeModel(net, locationToSave, true)
   }

//     def generateInputs(input: String): List[String] = {
//         val inputs = new LinkedList[String]()
//         for (i <- 1 until input.length()) {
//             inputs.add(input.substring(0, i))
//         }
//         inputs.add(input)
//         inputs.asScala.toList
//     }

//     def readLookupTable[T <: SequenceElement](stream: InputStream): WeightLookupTable[T] = {
//         val weightLookupTable = new InMemoryLookupTable[T]()
//         var headerRead = false
//         for (line <- IOUtils.readLines(stream, "UTF-8").asScala) {
//             val tokens = line.split(" ")
//             if (!headerRead) {
//                 val numWords = tokens(0).toInt
//                 val layerSize = tokens(1).toInt
//                 val totalNumberOfDocs = tokens(2).toInt
//                 log.debug(s"Reading header - words: $numWords, layerSize: $layerSize, totalNumberOfDocs: $totalNumberOfDocs")
//                 headerRead = true
//             }

//             val label = WordVectorSerializer.decodeB64(tokens(0))
//             val vector = Nd4j.create(tokens.length - 1)
//             if (label != null && vector != null) {
//                 for (i <- 1 until tokens.length) {
//                     vector.putScalar(i - 1, tokens(i).toDouble)
//                 }
//                 weightLookupTable.putVector(label, vector)
//             }
//         }
//         stream.close()
//         weightLookupTable
//     }
}