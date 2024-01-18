package suggestors.models

// import org.nd4j.linalg.api.ndarray.INDArray
// import org.nd4j.linalg.factory.Nd4j
// import org.nd4j.linalg.dataset.DataSet
// import org.nd4j.linalg.dataset.api.DataSetPreProcessor

import java.nio.charset.Charset
import java.io.File
import java.nio.file.Files
import java.util.Arrays
import java.util.Collections
import scala.util.Random
import scala.collection.mutable.ListBuffer
import scala.collection.mutable
import java.io.IOException
import org.slf4j.LoggerFactory
import scala.jdk.CollectionConverters._

class CharacterIterator(textFilePath: String, textFileEncoding: Charset, miniBatchSize: Int, exampleLength: Int, validCharacters: Array[Char], rng: Random) {

    //Variables
    //Maps each character to an index ind the input/output
    val charToIdxMap = mutable.Map[Char, Int]()
    //All characters of the input file (after filtering to only those that are valid
    var fileCharacters = Array.empty[Char]
    //Offsets for the process of each example
    var exampleStartOffsets = ListBuffer[Int]()
    //End Variables


    //Constructor
    if (!new File(textFilePath).exists()) {
      throw new IOException("Could not access file (does not exist): " + textFilePath)
    }
    if (miniBatchSize <= 0) {
      throw new IllegalArgumentException("Invalid miniBatchSize (must be >0)")
    }

    //Store valid characters is a map for later use in vectorization
    for (i <- 0 until validCharacters.length) {
       charToIdxMap += (validCharacters(i) -> i)
    }

    //Load file and convert contents to a char[]
    val newLineValid = charToIdxMap.contains('\n')
    val path = new File(textFilePath).toPath()
    val lines = Files.readAllLines(path, textFileEncoding)
    var maxSize = lines.size()    //add lines.size() to account for newline characters at end of each line
    for (line <- lines.asScala) {
      maxSize += line.length()
    }
    val characters = Array.ofDim[Char](maxSize)
    var currIdx = 0
    for (line <- lines.asScala) {
        for (charInLine <- line.toCharArray()) {
            if (charToIdxMap.contains(charInLine)) {
                characters(currIdx) = charInLine
                currIdx += 1
            }
        }
        if (newLineValid) {
            characters(currIdx) = '\n'
            currIdx += 1
        }
    }

    if (currIdx == characters.length) {
      fileCharacters = characters
    } else {
      fileCharacters = characters.slice(0, currIdx)
    }
    if (exampleLength >= fileCharacters.length) {
      throw new IllegalArgumentException("exampleLength=" + exampleLength
          + " cannot exceed number of valid characters in file (" + fileCharacters.length + ")")
    }

    val nRemoved = maxSize - fileCharacters.length
    val log = LoggerFactory.getLogger(getClass())
    log.info("Loaded and converted file: {} valid characters of {} total characters ({} removed)",
            fileCharacters.length, maxSize, nRemoved)

    initializeOffsets()
    //End Constructor

    def this(textFilePath: String, textFileEncoding: Charset, miniBatchSize: Int, exampleLength: Int) = {
        this(textFilePath, textFileEncoding, miniBatchSize, exampleLength, CharacterIterator.getMinimalCharacterSet, new Random())
    }

    def inputColumns(): Int = {
        validCharacters.length
    }

    def totalExamples(): Int = {
        (fileCharacters.length - 1) / miniBatchSize - 2
    }
    
    def initializeOffsets(): Unit = {
        //This defines the order in which parts of the file are fetched
        val nMinibatchesPerEpoch = (fileCharacters.length - 1) / exampleLength - 2   //-2: for end index, and for partial example
        for (i <- 0 until nMinibatchesPerEpoch) {
            exampleStartOffsets += (i * exampleLength)
        }
        exampleStartOffsets = rng.shuffle(exampleStartOffsets)
    }

    //TODO Not Implemented
    // char convertIndexToCharacter(int idx) {
    //     return validCharacters[idx]
    // }

    // int convertCharacterToIndex(char c) {
    //     return charToIdxMap.get(c)
    // }

    // public boolean hasNext() {
    //     return exampleStartOffsets.size() > 0
    // }

    // public DataSet next() {
    //     return next(miniBatchSize)
    // }

    // public DataSet next(int num) {
    //     if (exampleStartOffsets.size() == 0) {
    //     throw new NoSuchElementException()
    //     }

    //     int currMinibatchSize = Math.min(num, exampleStartOffsets.size())
    //     //Allocate space:
    //     //Note the order here:
    //     // dimension 0 = number of examples in minibatch
    //     // dimension 1 = size of each vector (i.e., number of characters)
    //     // dimension 2 = length of each time series/example
    //     //Why 'f' order here? See http://deeplearning4j.org/usingrnns.html#data section "Alternative: Implementing a custom DataSetIterator"
    //     INDArray input = Nd4j.create(new int[] {currMinibatchSize, validCharacters.length, exampleLength}, 'f')
    //     INDArray labels = Nd4j.create(new int[] {currMinibatchSize, validCharacters.length, exampleLength}, 'f')

    //     for (int i = 0 i < currMinibatchSize i++) {
    //     int startIdx = exampleStartOffsets.removeFirst()
    //     int endIdx = startIdx + exampleLength
    //     int currCharIdx = charToIdxMap.get(fileCharacters[startIdx])    //Current input
    //     int c = 0
    //     for (int j = startIdx + 1 j < endIdx j++, c++) {
    //         int nextCharIdx = charToIdxMap.get(fileCharacters[j])        //Next character to predict
    //         input.putScalar(new int[] {i, currCharIdx, c}, 1.0)
    //         labels.putScalar(new int[] {i, nextCharIdx, c}, 1.0)
    //         currCharIdx = nextCharIdx
    //     }
    //     }

    //     return new DataSet(input, labels)
    // }


    // public int totalOutcomes() {
    //     return validCharacters.length
    // }

    // public void reset() {
    //     exampleStartOffsets.clear()
    //     initializeOffsets()
    // }

    // public boolean resetSupported() {
    //     return true
    // }

    // @Override
    // public boolean asyncSupported() {
    //     return true
    // }

    // public int batch() {
    //     return miniBatchSize
    // }

    // public int cursor() {
    //     return totalExamples() - exampleStartOffsets.size()
    // }

    // public void setPreProcessor(DataSetPreProcessor preProcessor) {
    //     throw new UnsupportedOperationException("Not implemented")
    // }

    // @Override
    // public DataSetPreProcessor getPreProcessor() {
    //     throw new UnsupportedOperationException("Not implemented")
    // }

    // @Override
    // public List<String> getLabels() {
    //     throw new UnsupportedOperationException("Not implemented")
    // }

    // @Override
    // public void remove() {
    //     throw new UnsupportedOperationException()
    // }

    // public String getTextFilePath() {
    //     return textFilePath
    // }
}

object CharacterIterator {
    def getMinimalCharacterSet: Array[Char] = {
        ('a' to 'z').toArray ++ ('A' to 'Z').toArray ++ ('0' to '9').toArray ++ Array('!', '&', '(', ')', '?', '-', '\'', '"', ',', '.', ':', ';', ' ', '\n', '\t').toArray
    }
}