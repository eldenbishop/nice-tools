package org.jauntsy.text;

import java.util.Map;

/**
 * Created by ebishop on 3/12/15.
 */
public interface FormattedMessage {
    String getMessage();
    String getMessagePattern();
    Map getMessageProperties();
}
