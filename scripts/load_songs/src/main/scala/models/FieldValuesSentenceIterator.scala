import org.deeplearning4j.text.sentenceiterator.SentenceIterator
import org.apache.lucene.index.IndexReader
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import java.util.Collections
import java.io.IOException

class FieldValuesSentenceIterator(reader: IndexReader, field: String) extends SentenceIterator {
    private var currentId: Int = 0

    @Override
    def reset(): Unit = {
        this.currentId = 0
    }

    @Override
    def nextSentence(): String = {
        if (!hasNext()) {
            return null
        }
        try {
            val document: Document = reader.document(currentId, Collections.singleton(field))
            document.getField(field).stringValue()
            // preProcessor != null ? preProcessor.preProcess(sentence) : sentence
        } catch {
            case e: IOException => throw new RuntimeException(e)
        } finally {
            currentId = currentId + 1
        }
    }

    @Override
    def hasNext(): Boolean = {
        return currentId < reader.numDocs()
    }

    //TODO implement these
    @Override
    def finish(): Unit = ???

    @Override
    def getPreProcessor(): org.deeplearning4j.text.sentenceiterator.SentencePreProcessor = ???

    @Override
    def setPreProcessor(x$0: org.deeplearning4j.text.sentenceiterator.SentencePreProcessor): Unit = ???
}
