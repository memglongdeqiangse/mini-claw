package com.miniclaw.channel.feishu;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.miniclaw.bus.AgentMessageQueue;
import com.miniclaw.channel.BaseChannel;
import com.miniclaw.channel.ChannelType;
import com.miniclaw.channel.config.FeiShuChannelConfig;
import com.lark.oapi.Client;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.enums.MsgTypeEnum;
import com.lark.oapi.service.im.v1.enums.ReceiveIdTypeEnum;
import com.lark.oapi.service.im.v1.model.*;
import com.lark.oapi.service.im.v1.model.ext.MessageText;
import com.miniclaw.channel.schema.AgentResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 飞书渠道实现
 * 支持私聊和群聊消息接收与回复
 *
 * @author lei
 */
@Slf4j
public class FeishuChannel extends BaseChannel {
    /**
     * 飞书渠道配置
     */
    private final FeiShuChannelConfig feiShuChannelConfig;
    /**
     * 飞书客户端
     */
    private final Client client;
    /**
     * 飞书事件处理器
     */
    private final EventDispatcher eventHandler;
    /**
     * 飞书WebSocket客户端
     */
    private com.lark.oapi.ws.Client wsClient;
    /**
     * 机器人openId
     */
    private final String botOpenId;

    public FeishuChannel(FeiShuChannelConfig config, AgentMessageQueue agentMessageQueue) {
        super(ChannelType.FEISHU, config, agentMessageQueue);
        this.feiShuChannelConfig = config;
        this.botOpenId = feiShuChannelConfig.getBotOpenId();
        // 初始化飞书客户端
        this.client = new Client.Builder(config.getAppId(), config.getAppSecret()).build();
        // 初始化事件处理器
        this.eventHandler = EventDispatcher.newBuilder("", "")
                .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                    @Override
                    public void handle(P2MessageReceiveV1 event) throws Exception {
                        handleMessageReceive(event);
                    }
                })
                .build();
    }

    /**
     * 处理接收到的消息
     */
    private void handleMessageReceive(P2MessageReceiveV1 event) throws Exception {
        String content = event.getEvent().getMessage().getContent();
        String userId = event.getEvent().getSender().getSenderId().getOpenId();
        String chatId = event.getEvent().getMessage().getChatId();
        // 获取会话类型，p2p或group
        String chatType = event.getEvent().getMessage().getChatType();
        //单聊：chat_type = "p2p"
        //群聊：chat_type = "group"
        //单聊：调用发送消息接口
        // 检查是否允许该用户
        if (!isAllowed(userId)) {
            sendToUser(chatType, chatId, "抱歉，您没有权限使用此机器人。");
            return;
        }

        // 解析消息内容
        Map<String, String> respContent;
        try {
            respContent = new Gson().fromJson(content, new TypeToken<Map<String, String>>() {
            }.getType());
        } catch (JsonSyntaxException e) {
            sendToUser(chatType, chatId, "解析消息失败，请发送文本消息");
            return;
        }
        String text = respContent.get("text");
        //如果是群聊，判断是否@了自己 如果没有@自己，则不处理，mentions数组中会包含自己的openId
        if (botOpenId != null && "group".equals(chatType)) {
            MentionEvent[] mentions = event.getEvent().getMessage().getMentions();
            if (mentions == null) {
                log.info("群聊消息，但未@机器人，不处理");
                return;
            }
            for (MentionEvent mention : mentions) {
                UserId mentionUserId = mention.getId();
                if (mentionUserId != null) {
                    if (mentionUserId.getOpenId().equals(botOpenId)) {
                        log.info("群聊消息，@了机器人");
                        process(chatId, chatId, text, Map.of("chatType", chatType));
                        return;
                    }
                }
            }
            return;
        }
        // 检查消息类型是否为文本
        if (!"text".equals(event.getEvent().getMessage().getMessageType())) {
            sendToUser(chatType, chatId, "暂不支持此消息类型，请发送文本消息");
            return;
        }

        // 构建会话ID（使用chatId作为sessionId）
        String sessionId = chatId;
        // 存储会话上下文，用于回复
        // 处理消息，发送到Agent
        process(userId, sessionId, text, Map.of("chatType", chatType));
    }

    @Override
    public boolean isAllowed(String userId) {
        // 如果白名单为空，允许所有用户
        if (allowFrom == null || allowFrom.isEmpty()) {
            return true;
        }
        return allowFrom.contains(userId);
    }

    @Override
    public CompletableFuture<Void> send(AgentResponse response) {
        return CompletableFuture.runAsync(() -> {
            // 默认发送给最后活跃的会话（简单实现）
            // 实际使用时可能需要更复杂的逻辑来管理多会话
            String chatType = (String) response.channelMeta().get("chatType");
            if (chatType != null && "p2p".equals(chatType)) {
                sendToUser(ReceiveIdTypeEnum.OPEN_ID.getValue(), response.userId(), response.output().getTextContent());
            } else {
                sendToUser(ReceiveIdTypeEnum.CHAT_ID.getValue(), response.sessionId(), response.output().getTextContent());
            }
        });
    }


    /**
     * 主动给用户发送消息
     *
     * @param chatType  会话类型：p2p、group
     * @param receiveId 接收者ID：open_id、user_id、union_id
     * @param text      消息内容
     * @return 发送结果，成功返回true，失败返回false
     */
    public boolean sendToUser(String chatType, String receiveId, String text) {
        try {
            MessageText messageText = new MessageText();
            messageText.setText(text);
            String content = JSON.toJSONString(messageText);
            CreateMessageReq req = CreateMessageReq.newBuilder()
                    .receiveIdType(chatType)
                    .createMessageReqBody(CreateMessageReqBody.newBuilder()
                            .receiveId(receiveId)
                            .msgType(MsgTypeEnum.MSG_TYPE_TEXT.getValue())
                            .content(content)
                            .build())
                    .build();

            CreateMessageResp resp = client.im().v1().message().create(req);
            if (resp.getCode() != 0) {
                log.error("发送消息给用户失败, userId: {}, req: {}",
                        receiveId, resp);
                return false;
            }
            log.info("消息发送成功, userId: {}", receiveId);
            return true;
        } catch (Exception e) {
            log.error("发送消息给用户异常, userId: {}, error: {}", receiveId, e.getMessage(), e);
            return false;
        }
    }


    @Override
    public void start() {
        log.info("Starting FeishuChannel...");
        wsClient = new com.lark.oapi.ws.Client.Builder(
                feiShuChannelConfig.getAppId(),
                feiShuChannelConfig.getAppSecret()
        ).eventHandler(eventHandler).build();

        wsClient.start();
        log.info("FeishuChannel started successfully.");
    }

    @Override
    public void stop() {
        if (wsClient != null) {
//            wsClient.disconnect();
            log.info("FeishuChannel stopped.");
        }
    }

}