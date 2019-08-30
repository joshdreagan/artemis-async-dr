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

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.transformer.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddMessageOriginTransformer implements Transformer {
  
  private static final Logger log = LoggerFactory.getLogger(AddMessageOriginTransformer.class);

  @Override
  public Message transform(Message msg) {
    String clusterName = System.getenv("ARTEMIS_CLUSTER_NAME");
    if (clusterName != null && !clusterName.isEmpty()) {
      log.debug(String.format("Setting the ARTEMIS_MESSAGE_ORIGIN header to value [%s].", clusterName));
      msg.putStringProperty("ARTEMIS_MESSAGE_ORIGIN", clusterName);
    } else {
      log.debug("Unable to set the ARTEMIS_MESSAGE_ORIGIN header. The ARTEMIS_CLUSTER_NAME environment variable is not set.");
    }
    return msg;
  }
}
