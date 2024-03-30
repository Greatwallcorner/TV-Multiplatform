package com.corner.init

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.layout.TTLLLayout
import ch.qos.logback.classic.spi.Configurator
import ch.qos.logback.classic.spi.Configurator.ExecutionStatus
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.RollingPolicy
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.spi.ContextAwareBase
import ch.qos.logback.core.util.FileSize
import com.corner.catvodcore.util.Paths

/**
@author heatdesert
@date 2024-03-09 9:30
@description
 */
class TVLogConfigurator():ContextAwareBase(),Configurator {
    override fun configure(context: LoggerContext?): ExecutionStatus {
        addInfo("Setting up TV logback configuration.");
        println("log config")
        val ca = consoleAppender(context)
        val fa = fileAppender(context)
        val rootLogger = context?.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger?.level = Level.DEBUG
//        rootLogger?.level = Level.valueOf(getSettingItem("log") )
        rootLogger?.addAppender(ca)
        rootLogger?.addAppender(fa)
        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY
    }

    private fun fileAppender(context: LoggerContext?): RollingFileAppender<ILoggingEvent> {
        val rf = RollingFileAppender<ILoggingEvent>()
        rf.context = context
        rf.file = Paths.logPath() + "TV.log"
        rf.isAppend = true
        rf.isPrudent = true
        val triggeringPolicy = SizeBasedTriggeringPolicy<ILoggingEvent>()
        triggeringPolicy.context = context
        triggeringPolicy.maxFileSize = FileSize.valueOf("50MB")
        triggeringPolicy.start()
        rf.triggeringPolicy = triggeringPolicy

        val rollingPolicy = TimeBasedRollingPolicy<ILoggingEvent>()
        rollingPolicy.fileNamePattern = "${Paths.logPath()}/TV" + "_%d{yyyy-MM-dd}.log"
        rollingPolicy.context = context
        rollingPolicy.maxHistory = 5
        rollingPolicy.setParent(rf)
        rollingPolicy.setTotalSizeCap(FileSize.valueOf("50MB"))
        rollingPolicy.start()
        rf.rollingPolicy = rollingPolicy

        val encoder = PatternLayoutEncoder()
        encoder.context = context
        encoder.pattern = "%d %-5level [%thread] %logger{0}: %msg%n"
        encoder.charset = Charsets.UTF_8
        encoder.start()
        rf.encoder = encoder
        rf.start()
        return rf
    }

    private fun consoleAppender(context: LoggerContext?): ConsoleAppender<ILoggingEvent> {
        val ca = ConsoleAppender<ILoggingEvent>();
        ca.setContext(context);
        ca.setName("console");
        val encoder = LayoutWrappingEncoder<ILoggingEvent>()
        encoder.context = context;
        // same as
        // PatternLayout layout = new PatternLayout();
        // layout.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} -
        // %msg%n");
        val layout = TTLLLayout();
        layout.context = context;
        layout.start();
        encoder.setLayout(layout);
        ca.setEncoder(encoder);
        ca.start();
        return ca
    }
}