package suggestors.models

import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.dataset.api.DataSetPreProcessor
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator
import java.nio.charset.Charset
import java.io.File
import java.nio.file.Files
import java.util.Arrays
import java.util.Collections
import java.util.{List => JList}
import scala.util.Random
import scala.collection.mutable.ListBuffer
import scala.collection.mutable
import java.io.IOException
import org.slf4j.LoggerFactory
import scala.jdk.CollectionConverters._

class CharacterIterator(textFilePath: String, textFileEncoding: Charset, miniBatchSize: Int, exampleLength: Int, validCharacters: Array[Char], rng: Random) extends DataSetIterator {

    //Variables
    //Maps each character to an index in the input/output
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

    //Store valid characters in a map for later use in vectorization
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
    //TODO this throws an OOM when the content is too large
    //Fix by writing to a temp file and reading in chunks
    //See: File Characters as important variable set at end
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

    def totalOutcomes(): Int = {
        validCharacters.length
    }

    def batch(): Int = {
        miniBatchSize
    }

    def hasNext(): Boolean = {
        exampleStartOffsets.length > 0
    }

    def next(): DataSet = {
        next(miniBatchSize)
    }

    def next(num: Int): DataSet = {
        if (exampleStartOffsets.length == 0) {
            throw new NoSuchElementException()
        }

        val currMinibatchSize = Math.min(num, exampleStartOffsets.length)
        //Allocate space:
        //Note the order here:
        // dimension 0 = number of examples in minibatch
        // dimension 1 = size of each vector (i.e., number of characters)
        // dimension 2 = length of each time series/example
        //Why 'f' order here? See http://deeplearning4j.org/usingrnns.html#data section "Alternative: Implementing a custom DataSetIterator"
        val input = Nd4j.create(Array(currMinibatchSize, validCharacters.length, exampleLength), 'f')
        val labels = Nd4j.create(Array(currMinibatchSize, validCharacters.length, exampleLength), 'f')

        for (i <- 0 until currMinibatchSize) {
            var startIdx = exampleStartOffsets.remove(0)
            var endIdx = startIdx + exampleLength
            println(s"ASCII Decimal Encoding of '${fileCharacters(startIdx)}': ${fileCharacters(startIdx).toInt}")
            var currCharIdx = charToIdxMap.get(fileCharacters(startIdx))
                .getOrElse(throw new NoSuchElementException(s"No value found for character '${fileCharacters(startIdx)}'"))    //Current input
            var c = 0
            for (j <- startIdx + 1 until endIdx) {
                c += 1
                // println("fileCharacters(j): " + fileCharacters(j) + " charToIdxMap: " + charToIdxMap.get(fileCharacters(j)))
                println(s"ASCII Decimal Encoding of '${fileCharacters(j)}': ${fileCharacters(j).toInt}")
                val nextCharIdx = charToIdxMap.get(fileCharacters(j))
                    .getOrElse(throw new NoSuchElementException(s"No value found for character '${fileCharacters(j)}'"))        //Next character to predict
                input.putScalar(Array(i, currCharIdx, c), 1.0)
                labels.putScalar(Array(i, nextCharIdx, c), 1.0)
                currCharIdx = nextCharIdx
            }
        }

        new DataSet(input, labels)
    }

    def reset(): Unit = {
        exampleStartOffsets.clear()
        initializeOffsets()
    }

    def resetSupported(): Boolean = {
        true
    }

    def asyncSupported(): Boolean = {
        true
    }
    

    def convertIndexToCharacter(idx: Int): Char = {
        return validCharacters(idx)
    }

    def convertCharacterToIndex(c: Char): Int = {
        println(s"ASCII Decimal Encoding of '${c}': ${c.toInt}")
        charToIdxMap.get(c) match {
            case Some(value) => value
            case None => throw new NoSuchElementException(s"No value found for character $c")
        }
    }

    def cursor(): Int = {
        return totalExamples() - exampleStartOffsets.length
    }

    def getTextFilePath(): String = {
        return textFilePath
    }

    def initializeOffsets(): Unit = {
        //This defines the order in which parts of the file are fetched
        val nMinibatchesPerEpoch = (fileCharacters.length - 1) / exampleLength - 2   //-2: for end index, and for partial example
        for (i <- 0 until nMinibatchesPerEpoch) {
            exampleStartOffsets += (i * exampleLength)
        }
        exampleStartOffsets = rng.shuffle(exampleStartOffsets)
    }

    override def getLabels(): JList[String] = {
        throw new UnsupportedOperationException("Not implemented")
    }

    override def setPreProcessor(preProcessor: DataSetPreProcessor): Unit = {
        throw new UnsupportedOperationException("Not implemented")
    }

    override def getPreProcessor(): DataSetPreProcessor = {
        throw new UnsupportedOperationException("Not implemented")
    }

    override def remove(): Unit = {
        throw new UnsupportedOperationException("Not implemented")
    }
}

object CharacterIterator {
    def getMinimalCharacterSet: Array[Char] = {
        // ('a' to 'z').toArray ++ ('A' to 'Z').toArray ++ ('0' to '9').toArray ++ Array('!', '&', '(', ')', '?', '-', '\'', '"', ',', '.', ':', ';', ' ', '\n', '\t').toArray
        ('a' to 'z').toArray ++ ('0' to '9').toArray ++ Array(' ').toArray
    }
}