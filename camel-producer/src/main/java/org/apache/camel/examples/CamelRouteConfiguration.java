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

import java.util.UUID;
import javax.jms.ConnectionFactory;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.amqp.AMQPComponent;
import org.apache.camel.spi.ComponentCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
public class CamelRouteConfiguration extends RouteBuilder {
  
  private static final String MESSAGE_UUID_HEADER = "ARTEMIS_MESSAGE_UUID";
  
  @Bean
  private ComponentCustomizer<AMQPComponent> amqpComponentCustomizer(ConnectionFactory jmsConnectionFactory) {
    return (component) -> {
      component.setConnectionFactory(jmsConnectionFactory);
    };
  }
  
  @Bean
  @Scope("prototype")
  private String uuid() {
    return UUID.randomUUID().toString();
  }
  
  @Override
  public void configure() {
    
    from("stream:in?promptMessage=RAW(> )&initialPromptDelay=0")
      .setHeader(MESSAGE_UUID_HEADER, simple("ref:uuid"))
      .log(LoggingLevel.DEBUG, log, String.format("Sending message [${header.%s}]: ${body}", MESSAGE_UUID_HEADER))
      .to(ExchangePattern.InOnly, "amqp:{{amqp.destination.type:queue}}://{{amqp.destination.name}}")
    ;
  }
}
