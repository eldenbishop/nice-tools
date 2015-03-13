package org.jauntsy.text;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by ebishop on 3/12/15.
 */
public class SimpleFormattedMessage implements FormattedMessage {

    private final String messagePattern;
    private final Map messageProperties;
    private final String message;

    public SimpleFormattedMessage(String messagePattern, Map messageProperties) {
        this(messagePattern, messageProperties, NamedMessageFormat.DEFAULT_SERIALIZER);
    }

    public SimpleFormattedMessage(String messagePattern, Map messageProperties, Function<Object, String> serializer) {
        this.messagePattern = messagePattern;
        this.messageProperties = messageProperties;
        this.message = NamedMessageFormat.format(messagePattern, messageProperties, serializer);
    }

    public SimpleFormattedMessage(String message) {
        this(NamedMessageFormat.escapeSimpleMessage(message), new LinkedHashMap<>());
    }

    @Override
    public String getMessagePattern() {
        return messagePattern;
    }

    @Override
    public Map getMessageProperties() {
        return messageProperties;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
