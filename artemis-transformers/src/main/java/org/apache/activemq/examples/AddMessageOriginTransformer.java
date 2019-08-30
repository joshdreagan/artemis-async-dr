/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.examples;

import java.util.Map;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.transformer.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddMessageOriginTransformer implements Transformer {
  
  private static final Logger log = LoggerFactory.getLogger(AddMessageOriginTransformer.class);

  private static final String MESSAGE_ORIGIN_HEADER = "ARTEMIS_MESSAGE_ORIGIN";

  private String messageOrigin;

  public String getMessageOrigin() {
    return messageOrigin;
  }

  public void setMessageOrigin(String messageOrigin) {
    this.messageOrigin = messageOrigin;
  }

  @Override
  public void init(Map<String, String> properties) {
    this.setMessageOrigin(properties.get("messageOrigin"));
  }
  
  @Override
  public Message transform(Message msg) {
    log.debug(String.format("Setting the %s header to value [%s].", MESSAGE_ORIGIN_HEADER, messageOrigin));
    msg.putStringProperty(MESSAGE_ORIGIN_HEADER, messageOrigin);
    return msg;
  }
}
