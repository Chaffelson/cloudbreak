package com.sequenceiq.cloudbreak.controller.mapper;

import static ch.qos.logback.classic.Level.DEBUG_INT;
import static ch.qos.logback.classic.Level.ERROR_INT;
import static ch.qos.logback.classic.Level.INFO;
import static ch.qos.logback.classic.Level.INFO_INT;
import static ch.qos.logback.classic.Level.WARN_INT;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult;

import ch.qos.logback.classic.Level;

abstract class BaseExceptionMapper<E extends Throwable> implements ExceptionMapper<E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseExceptionMapper.class);

    @Override
    public Response toResponse(E exception) {
        if (logException()) {
            String errorMessage = getErrorMessage(exception);
            switch (getLogLevel().levelInt) {
                case ERROR_INT:
                    LOGGER.error(errorMessage, exception);
                    break;
                case WARN_INT:
                    LOGGER.warn(errorMessage, exception);
                    break;
                case INFO_INT:
                    LOGGER.info(errorMessage, exception);
                    break;
                case DEBUG_INT:
                    LOGGER.debug(errorMessage, exception);
                    break;
                default:
                    LOGGER.info(errorMessage, exception);
                    break;
            }
        }
        return Response.status(getResponseStatus()).entity(getEntity(exception)).build();
    }

    protected String getErrorMessage(E exception) {
        return exception.getMessage();
    }

    protected Object getEntity(E exception) {
        return new ExceptionResult(getErrorMessage(exception));
    }

    protected boolean logException() {
        return true;
    }

    protected Level getLogLevel() {
        return INFO;
    }

    abstract Status getResponseStatus();

    abstract Class<E> getExceptionType();
}
