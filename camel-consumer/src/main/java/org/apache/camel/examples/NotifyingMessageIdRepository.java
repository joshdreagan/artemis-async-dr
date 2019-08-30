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
package org.apache.camel.examples;

import java.util.HashMap;
import java.util.Map;
import javax.jms.ConnectionFactory;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spi.IdempotentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotifyingMessageIdRepository<T> implements IdempotentRepository<T> {
  
  private static final Logger log = LoggerFactory.getLogger(NotifyingMessageIdRepository.class);
  
  private static final String NOTIFICATION_DESTINATION_HEADER = "CamelJmsDestinationName";
  private static final String NOTIFICATION_TYPE_HEADER = "NotificationType";
  private static enum NotificationType {
    ADD,
    REMOVE,
    CONFIRM
  }
  
  private final ConnectionFactory connectionFactory;
  private final String topicName;
  
  private final IdempotentRepository<T> delegate;

  @EndpointInject(uri = "amqp:topic:dummy")
  private ProducerTemplate producer;

  public NotifyingMessageIdRepository(ConnectionFactory connectionFactory, String topicName, IdempotentRepository<T> delegate) {
    this.connectionFactory = connectionFactory;
    this.topicName = topicName;
    
    this.delegate = delegate;
  }

  public ConnectionFactory getConnectionFactory() {
    return connectionFactory;
  }

  public String getTopicName() {
    return topicName;
  }

  public IdempotentRepository<T> getDelegate() {
    return delegate;
  }

  @Override
  public boolean add(T key) {
    Map<String, Object> headers = new HashMap<>();
    headers.put(NOTIFICATION_TYPE_HEADER, NotificationType.ADD.toString());
    headers.put(NOTIFICATION_DESTINATION_HEADER, topicName);
    producer.sendBodyAndHeaders(key, headers);
    return delegate.add(key);
  }

  @Override
  public boolean contains(T key) {
    return delegate.contains(key);
  }

  @Override
  public boolean remove(T key) {
    Map<String, Object> headers = new HashMap<>();
    headers.put(NOTIFICATION_TYPE_HEADER, NotificationType.REMOVE.toString());
    headers.put(NOTIFICATION_DESTINATION_HEADER, topicName);
    producer.sendBodyAndHeaders(key, headers);
    return delegate.remove(key);
  }

  @Override
  public boolean confirm(T key) {
    Map<String, Object> headers = new HashMap<>();
    headers.put(NOTIFICATION_TYPE_HEADER, NotificationType.CONFIRM.toString());
    headers.put(NOTIFICATION_DESTINATION_HEADER, topicName);
    producer.sendBodyAndHeaders(key, headers);
    return delegate.confirm(key);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public void start() throws Exception {
    delegate.start();
  }

  @Override
  public void stop() throws Exception {
    delegate.stop();
  }
}
