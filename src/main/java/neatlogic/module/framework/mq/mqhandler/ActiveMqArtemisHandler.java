/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.framework.mq.mqhandler;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.exception.mq.SubscribeTopicException;
import neatlogic.framework.mq.core.IMqHandler;
import neatlogic.framework.mq.core.ISubscribeHandler;
import neatlogic.framework.mq.dto.SubscribeVo;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.jms.*;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActiveMqArtemisHandler implements IMqHandler {
    private static final Logger logger = LoggerFactory.getLogger(ActiveMqArtemisHandler.class);
    private static ConnectionFactory connectionFactory;
    private static final Map<String, SimpleMessageListenerContainer> containerMap = new ConcurrentHashMap<>();
    public static final String SEPARATOR = "#";
    protected static JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate _jmsTemplate) {
        jmsTemplate = _jmsTemplate;
    }

    @Autowired
    public void setConnectionFactory(ActiveMQConnectionFactory _connectionFactory) {
        connectionFactory = _connectionFactory;
    }

    @Override
    public String getName() {
        return "artemis";
    }

    @Override
    public boolean create(SubscribeVo subVo, ISubscribeHandler subscribeHandler) throws SubscribeTopicException {
        String topicName = subVo.getTopicName();
        String clientName = subVo.getName();
        boolean isDurable = subVo.getIsDurable().equals(1);
        topicName = topicName.toLowerCase(Locale.ROOT);
        clientName = clientName.toLowerCase(Locale.ROOT);
        if (!containerMap.containsKey(TenantContext.get().getTenantUuid() + SEPARATOR + topicName + SEPARATOR + clientName)) {
            String finalClientName = clientName;
            String tenantUuid = TenantContext.get().getTenantUuid();
            String finalTopicName = topicName;
            MessageListenerAdapter messageAdapter = new MessageListenerAdapter() {
                @Override
                public void onMessage(Message message, @Nullable Session session) throws JMSException {
                    try {
                        subscribeHandler.onMessage((TextMessage) message, session, finalTopicName, finalClientName, tenantUuid);
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
            };

            SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
            container.setConnectionFactory(connectionFactory);
            container.setPubSubDomain(true);
            container.setDestinationName(TenantContext.get().getTenantUuid() + "/" + topicName);
            container.setDurableSubscriptionName(TenantContext.get().getTenantUuid() + "/" + clientName + "/" + Config.SCHEDULE_SERVER_ID);
            container.setSubscriptionDurable(isDurable);
            container.setClientId(TenantContext.get().getTenantUuid() + "/" + clientName + "/" + Config.SCHEDULE_SERVER_ID);
            container.setMessageListener(messageAdapter);
            //container.setAutoStartup(true);
            containerMap.put(TenantContext.get().getTenantUuid() + SEPARATOR + topicName + SEPARATOR + clientName, container);
            try {
                container.start();
            } catch (Exception ex) {
                throw new SubscribeTopicException(topicName, clientName, ex.getMessage());
            }
        }
        return true;
    }

    @Override
    public void reconnect(SubscribeVo subscribeVo) {
        String topicName = subscribeVo.getTopicName();
        String clientName = subscribeVo.getName();
        topicName = topicName.toLowerCase(Locale.ROOT);
        clientName = clientName.toLowerCase(Locale.ROOT);
        SimpleMessageListenerContainer container = containerMap.get(TenantContext.get().getTenantUuid() + SEPARATOR + topicName + SEPARATOR + clientName);
        if (container != null && !container.isRunning()) {
            container.start();
        }
    }

    @Override
    public void destroy(SubscribeVo subscribeVo) {
        SimpleMessageListenerContainer container = containerMap.get(TenantContext.get().getTenantUuid()
                + SEPARATOR
                + subscribeVo.getTopicName().toLowerCase()
                + SEPARATOR
                + subscribeVo.getName().toLowerCase());
        if (container.isRunning()) {
            container.stop();
        }
        container.shutdown();
        container.destroy();
        containerMap.remove(TenantContext.get().getTenantUuid()
                + SEPARATOR
                + subscribeVo.getTopicName().toLowerCase()
                + SEPARATOR
                + subscribeVo.getName().toLowerCase());
    }

    @Override
    public void send(String topicName, String content) {
        try {
            jmsTemplate.convertAndSend(TenantContext.get().getTenantUuid() + "/" + topicName, content);
        } catch (Exception ex) {
            logger.error("发送消息到{}/{}失败，异常：{}", TenantContext.get().getTenantUuid(), topicName, ex.getMessage());
        }
    }


}
