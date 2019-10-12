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

import java.util.UUID;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerPlugin;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddMessageUuidPlugin implements ActiveMQServerPlugin {
  
  private static final Logger log = LoggerFactory.getLogger(AddMessageUuidPlugin.class);

  private static final String MESSAGE_UUID_HEADER = "ARTEMIS_MESSAGE_UUID";

  @Override
  public void beforeSend(ServerSession session, Transaction tx, Message message, boolean direct, boolean noAutoCreateQueue) throws ActiveMQException {
    if (!message.containsProperty(MESSAGE_UUID_HEADER)) {
      String uuid = UUID.randomUUID().toString();
      log.debug(String.format("Setting the %s header to value [%s].", MESSAGE_UUID_HEADER, uuid));
      message.putStringProperty(MESSAGE_UUID_HEADER, uuid);
      message.reencode();
    }
  }
}
