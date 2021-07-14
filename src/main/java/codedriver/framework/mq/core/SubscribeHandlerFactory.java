/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.framework.mq.core;

import codedriver.framework.applicationlistener.core.ApplicationListenerBase;
import codedriver.framework.common.RootComponent;
import codedriver.framework.mq.dto.SubscribeHandlerVo;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RootComponent
public class SubscribeHandlerFactory extends ApplicationListenerBase implements BeanFactoryPostProcessor {
    private static final Map<String, ISubscribeHandler> componentMap = new HashMap<>();

    public static ISubscribeHandler getHandler(String handlerId) {
        return componentMap.get(handlerId);
    }

    private static final List<SubscribeHandlerVo> subscribeHandlerVoList = new ArrayList<>();

    public static List<SubscribeHandlerVo> getSubscribeHandlerList() {
        return subscribeHandlerVoList;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        Map<String, ISubscribeHandler> myMap = context.getBeansOfType(ISubscribeHandler.class);
        for (Map.Entry<String, ISubscribeHandler> entry : myMap.entrySet()) {
            ISubscribeHandler component = entry.getValue();
            componentMap.put(component.getClassName(), component);
            subscribeHandlerVoList.add(new SubscribeHandlerVo(component.getName(), component.getClassName()));
        }
    }

    @Override
    protected void myInit() {

    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }
}