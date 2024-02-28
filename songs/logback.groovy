import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory

def logFile = "/songs.log"

LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory()

// Define file appender
FileAppender fileAppender = new FileAppender()
fileAppender.setContext(loggerContext)
fileAppender.setFile(logFile)

PatternLayoutEncoder encoder = new PatternLayoutEncoder()
encoder.setContext(loggerContext)
encoder.setPattern("%date [%thread] %-5level %logger{36} - %msg%n")
encoder.start()

fileAppender.setEncoder(encoder)
fileAppender.start()

// Define console appender
ConsoleAppender consoleAppender = new ConsoleAppender()
consoleAppender.setContext(loggerContext)
consoleAppender.setEncoder(encoder)
consoleAppender.start()

// Get root logger and add appenders
Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
rootLogger.setLevel(Level.INFO)
rootLogger.addAppender(fileAppender)
rootLogger.addAppender(consoleAppender)
