package com.chuangjie.module.iot.gateway.protocol.mqtt.handler.downstream;

import com.chuangjie.module.iot.core.messagebus.core.IotMessageBus;
import com.chuangjie.module.iot.core.mq.message.IotDeviceMessage;
import com.chuangjie.module.iot.gateway.protocol.AbstractIotProtocolDownstreamSubscriber;
import com.chuangjie.module.iot.gateway.protocol.mqtt.IotMqttProtocol;
import lombok.extern.slf4j.Slf4j;

/**
 * IoT 网关 MQTT 协议：接收下行给设备的消息
 *
 * @author hhy
 */
@Slf4j
public class IotMqttDownstreamSubscriber extends AbstractIotProtocolDownstreamSubscriber {

    private final IotMqttDownstreamHandler downstreamHandler;

    public IotMqttDownstreamSubscriber(IotMqttProtocol protocol,
                                       IotMqttDownstreamHandler downstreamHandler,
                                       IotMessageBus messageBus) {
        super(protocol, messageBus);
        this.downstreamHandler = downstreamHandler;
    }

    @Override
    protected void handleMessage(IotDeviceMessage message) {
        downstreamHandler.handle(message);
    }

}
