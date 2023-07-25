package org.vividus.plugin.jira.exception;

import org.apache.commons.lang3.StringUtils;

public final class NotSingleUniqueValueException extends Exception {

  private static final long serialVersionUID = -5714259148779518041L;

  public NotSingleUniqueValueException(Iterable<String> values) {
    super("Expected one unique element but was: " + StringUtils.join(values, ", "));
  }
}
