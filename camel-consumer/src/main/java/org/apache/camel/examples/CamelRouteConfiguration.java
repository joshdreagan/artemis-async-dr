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

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.amqp.AMQPComponent;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.apache.camel.spi.ComponentCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class CamelRouteConfiguration extends RouteBuilder {
  
  private static final Logger log = LoggerFactory.getLogger(CamelRouteConfiguration.class);
  
  private static final String MESSAGE_UUID_HEADER = "ARTEMIS_MESSAGE_UUID";
  
  @Bean
  private ComponentCustomizer<AMQPComponent> amqpComponentCustomizer(ConnectionFactory jmsConnectionFactory) {
    return (component) -> {
      component.setConnectionFactory(jmsConnectionFactory);
    };
  }
  
  @Bean
  @Autowired
  private JdbcMessageIdRepository jdbcMessageIdRepository(DataSource dataSource, @Value("${camel.springboot.name}") String processorName) {
    JdbcMessageIdRepository jdbcMessageIdRepository = new JdbcMessageIdRepository(dataSource, processorName);
    return jdbcMessageIdRepository;
  }
  
  @Bean
  @Autowired
  private NotifyingMessageIdRepository<String> notifyingMessageIdRepository(ConnectionFactory jmsConnectionFactory, @Value("${consumer.notification.topic-name}") String notificationTopicName, JdbcMessageIdRepository jdbcMessageIdRepository) {
    return new NotifyingMessageIdRepository<>(jmsConnectionFactory, notificationTopicName, jdbcMessageIdRepository);
  }
  
  @Override
  public void configure() {
    
    from("amqp:{{amqp.destination.type:queue}}://{{amqp.destination.name}}?disableReplyTo=true")
      .log(LoggingLevel.DEBUG, log, String.format("Picked up message [${header.%s}]", MESSAGE_UUID_HEADER))
      .idempotentConsumer(header(MESSAGE_UUID_HEADER))
        .messageIdRepositoryRef("notifyingMessageIdRepository")
        .log(LoggingLevel.DEBUG, log, String.format("Processed message [${header.%s}]: ${body}", MESSAGE_UUID_HEADER))
        .to("stream:out")
      .end()
    ;
  }
}
