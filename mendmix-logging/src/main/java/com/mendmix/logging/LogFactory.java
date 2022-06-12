/**
 *    Copyright 2009-2020 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.mendmix.logging;

import java.lang.reflect.Constructor;

import com.mendmix.common.MendmixBaseException;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public final class LogFactory {

  /**
   * Marker to be used by logging implementations that support markers.
   */
  public static final String MARKER = "mendmix";

  private static Constructor<? extends Logger> logConstructor;

  static {
    tryImplementation(LogFactory::useSlf4jLogging);
    tryImplementation(LogFactory::useLog4J2Logging);
    tryImplementation(LogFactory::useLog4JLogging);
    tryImplementation(LogFactory::useJdkLogging);
  }

  private LogFactory() {
    // disable construction
  }

  public static Logger getLog(Class<?> clazz) {
    return getLog(clazz.getName());
  }

  public static Logger getLog(String logger) {
    try {
      return logConstructor.newInstance(logger);
    } catch (Throwable t) {
      throw new MendmixBaseException("Error creating logger for logger " + logger + ".  Cause: " + t);
    }
  }

  public static synchronized void useCustomLogging(Class<? extends Logger> clazz) {
    setImplementation(clazz);
  }

  public static synchronized void useSlf4jLogging() {
    setImplementation(com.mendmix.logging.adapter.slf4j.Slf4jImpl.class);
  }

  public static synchronized void useLog4JLogging() {
    setImplementation(com.mendmix.logging.adapter.log4j.Log4jImpl.class);
  }

  public static synchronized void useLog4J2Logging() {
    setImplementation(com.mendmix.logging.adapter.log4j2.Log4j2Impl.class);
  }

  public static synchronized void useJdkLogging() {
    setImplementation(com.mendmix.logging.adapter.jdk14.Jdk14LoggingImpl.class);
  }

  public static synchronized void useStdOutLogging() {
    setImplementation(com.mendmix.logging.adapter.stdout.StdOutImpl.class);
  }

  private static void tryImplementation(Runnable runnable) {
    if (logConstructor == null) {
      try {
        runnable.run();
      } catch (Throwable t) {
        // ignore
      }
    }
  }

  private static void setImplementation(Class<? extends Logger> implClass) {
    try {
      Constructor<? extends Logger> candidate = implClass.getConstructor(String.class);
      Logger log = candidate.newInstance(LogFactory.class.getName());
      if (log.isDebugEnabled()) {
        log.debug("MENDMIX-TRACE-LOGGGING-->> Logging initialized using '" + implClass + "' adapter.");
      }
      logConstructor = candidate;
    } catch (Throwable t) {
      throw new MendmixBaseException("Error setting Log implementation.  Cause: " + t);
    }
  }

}
