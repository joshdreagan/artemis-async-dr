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
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class CamelRouteConfiguration extends RouteBuilder {
  
  private static final Logger log = LoggerFactory.getLogger(CamelRouteConfiguration.class);

  private static final String NOTIFICATION_TYPE_HEADER = "NotificationType";
  
  @Autowired
  private ConsumerProperties consumerProperties;
  
  @Bean
  private ComponentCustomizer<AMQPComponent> amqpComponentCustomizer(ConnectionFactory jmsConnectionFactory) {
    return (component) -> {
      component.setConnectionFactory(jmsConnectionFactory);
    };
  }
  
  @Bean
  @Autowired
  private JdbcMessageIdRepository jdbcMessageIdRepository(DataSource dataSource) {
    JdbcMessageIdRepository jdbcMessageIdRepository = new JdbcMessageIdRepository(dataSource, consumerProperties.getNotification().getApplicationName());
    return jdbcMessageIdRepository;
  }
  
  @Override
  public void configure() {
    
    fromF("amqp:topic://%1$s::%1$s.%2$s", consumerProperties.getNotification().getTopicName(), consumerProperties.getNotification().getApplicationName())
      .choice()
        .when(simpleF("${headers.%s} =~ 'ADD'", NOTIFICATION_TYPE_HEADER))
          .to("direct:add")
        .when(simpleF("${headers.%s} =~ 'REMOVE'", NOTIFICATION_TYPE_HEADER))
          .to("direct:remove")
        .when(simpleF("${headers.%s} =~ 'CONFIRM'", NOTIFICATION_TYPE_HEADER))
          .to("direct:confirm")
        .otherwise()
          .log(LoggingLevel.INFO, log, String.format("Not sure how to handle type [${headers.%s}] for message [${body}]", NOTIFICATION_TYPE_HEADER))
      .end()
    ;
    
    from("direct:add")
      .setHeader("MessageID", body())
      .to("bean:jdbcMessageIdRepository?method=add(${body})")
      .choice()
        .when(body())
          .log(LoggingLevel.DEBUG, log, "Manually added id to repository: [${header.MessageID}]")
        .otherwise()
          .log(LoggingLevel.DEBUG, log, "Unable to manually add id to repository: [${header.MessageID}]")
      .end()
    ;
    
    from("direct:remove")
      .setHeader("MessageID", body())
      .to("bean:jdbcMessageIdRepository?method=remove(${body})")
      .choice()
        .when(body())
          .log(LoggingLevel.DEBUG, log, "Manually removed id from repository: [${header.MessageID}]")
        .otherwise()
          .log(LoggingLevel.DEBUG, log, "Unable to manually remove id from repository: [${header.MessageID}]")
      .end()
    ;
    
    from("direct:confirm")
      .setHeader("MessageID", body())
      .to("bean:jdbcMessageIdRepository?method=confirm(${body})")
      .choice()
        .when(body())
          .log(LoggingLevel.DEBUG, log, "Manually confirmed id in repository: [${header.MessageID}]")
        .otherwise()
          .log(LoggingLevel.DEBUG, log, "Unable to manually confirm id in repository: [${header.MessageID}]")
      .end()
    ;
  }
}
