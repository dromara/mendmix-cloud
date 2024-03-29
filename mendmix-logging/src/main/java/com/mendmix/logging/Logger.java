/**
 *    Copyright 2009-2015 the original author or authors.
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

/**
 * @author Clinton Begin
 */
public interface Logger {
	
  boolean isInfoEnabled();

  boolean isDebugEnabled();

  boolean isTraceEnabled();

  void error(String s);
  
  void error(String s, Throwable e);

  void debug(String s);
  
  void debug(String format, Object... arguments);

  void trace(String s);
  
  void trace(String format, Object... arguments);

  void warn(String s);
  
  void warn(String format, Object... arguments);
  
  void info(String s);
  
  void info(String format, Object... arguments);

}
