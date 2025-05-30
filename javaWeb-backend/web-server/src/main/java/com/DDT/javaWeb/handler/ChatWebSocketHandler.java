package com.DDT.javaWeb.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {

    // 存储所有连接的会话
    private static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // 存储用户ID与会话ID的映射
    private static final ConcurrentHashMap<Long, String> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");

        if (userId != null) {
            sessions.put(session.getId(), session);
            userSessions.put(userId, session.getId());

            log.info("用户 {} (ID: {}) 连接成功，会话ID: {}", username, userId, session.getId());

            // 发送欢迎消息
            JSONObject welcomeMsg = new JSONObject();
            welcomeMsg.set("type", "welcome");
            welcomeMsg.set("message", "连接成功");
            welcomeMsg.set("timestamp", System.currentTimeMillis());
            sendMessage(session, welcomeMsg.toString());

            // 通知其他用户有人上线
            broadcastUserOnline(userId, username);

            // 发送在线用户列表
            sendOnlineUsersList(session);
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");

        if (userId == null) return;

        try {
            String payload = message.getPayload().toString();
            JSONObject json = JSONUtil.parseObj(payload);
            String type = json.getStr("type", "message");

            switch (type) {
                case "test":
                    handleTestMessage(session, json);
                    break;
                case "message":
                    handlePublicMessage(session, userId, username, json);
                    break;
                case "privateMessage":
                    handlePrivateMessage(session, userId, username, json);
                    break;
                case "joinRoom":
                    handleJoinRoom(session, userId, json);
                    break;
                default:
                    log.warn("未知的消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息时出错", e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误", exception);
        removeSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");

        if (userId != null) {
            log.info("用户 {} (ID: {}) 断开连接", username, userId);
            removeSession(session);
            broadcastUserOffline(userId, username);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    // 处理测试消息
    private void handleTestMessage(WebSocketSession session, JSONObject json) {
        JSONObject response = new JSONObject();
        response.set("type", "testResponse");
        response.set("originalMessage", json.getStr("message"));
        response.set("response", "测试消息收到！");
        response.set("timestamp", System.currentTimeMillis());

        sendMessage(session, response.toString());
    }

    // 处理公共消息
    private void handlePublicMessage(WebSocketSession session, Long userId, String username, JSONObject json) {
        String content = json.getStr("content");
        if (content == null || content.trim().isEmpty()) return;

        // 创建消息对象
        JSONObject messageObj = new JSONObject();
        messageObj.set("type", "newMessage");
        messageObj.set("id", System.currentTimeMillis());
        messageObj.set("senderId", userId);
        messageObj.set("senderName", username);
        messageObj.set("content", content);
        messageObj.set("timestamp", LocalDateTime.now().toString());
        messageObj.set("roomId", "global");

        // 广播给所有连接的用户
        broadcastMessage(messageObj.toString());

        log.info("用户 {} 发送公共消息: {}", username, content);
    }

    // 处理私聊消息
    private void handlePrivateMessage(WebSocketSession session, Long userId, String username, JSONObject json) {
        String content = json.getStr("content");
        Long targetUserId = json.getLong("targetUserId");

        if (content == null || content.trim().isEmpty() || targetUserId == null) return;

        // 创建私聊消息对象
        JSONObject messageObj = new JSONObject();
        messageObj.set("type", "privateMessage");
        messageObj.set("id", System.currentTimeMillis());
        messageObj.set("senderId", userId);
        messageObj.set("senderName", username);
        messageObj.set("receiverId", targetUserId);
        messageObj.set("content", content);
        messageObj.set("timestamp", LocalDateTime.now().toString());

        // 发送给目标用户
        sendMessageToUser(targetUserId, messageObj.toString());

        log.info("用户 {} 发送私聊消息给用户 {}: {}", username, targetUserId, content);
    }

    // 处理加入房间
    private void handleJoinRoom(WebSocketSession session, Long userId, JSONObject json) {
        String roomId = json.getStr("roomId");
        log.info("用户 {} 加入房间 {}", userId, roomId);
    }

    // 广播用户上线
    private void broadcastUserOnline(Long userId, String username) {
        JSONObject userOnlineMsg = new JSONObject();
        userOnlineMsg.set("type", "userOnline");
        userOnlineMsg.set("userId", userId);
        userOnlineMsg.set("username", username);
        userOnlineMsg.set("online", true);

        broadcastMessage(userOnlineMsg.toString());
    }

    // 广播用户下线
    private void broadcastUserOffline(Long userId, String username) {
        JSONObject userOfflineMsg = new JSONObject();
        userOfflineMsg.set("type", "userOffline");
        userOfflineMsg.set("userId", userId);
        userOfflineMsg.set("username", username);
        userOfflineMsg.set("online", false);

        broadcastMessage(userOfflineMsg.toString());
    }

    // 发送在线用户列表
    private void sendOnlineUsersList(WebSocketSession session) {
        JSONObject onlineUsersMsg = new JSONObject();
        onlineUsersMsg.set("type", "onlineUsers");

        // 获取当前在线用户
        JSONObject[] onlineUsers = userSessions.entrySet().stream()
                .map(entry -> {
                    Long userId = entry.getKey();
                    String sessionId = entry.getValue();
                    WebSocketSession userSession = sessions.get(sessionId);

                    if (userSession != null && userSession.isOpen()) {
                        String username = (String) userSession.getAttributes().get("username");
                        if (username != null) {
                            JSONObject userObj = new JSONObject();
                            userObj.set("userId", userId);
                            userObj.set("username", username);
                            return userObj;
                        }
                    }
                    return null;
                })
                .filter(user -> user != null)
                .toArray(JSONObject[]::new);

        onlineUsersMsg.set("users", onlineUsers);
        sendMessage(session, onlineUsersMsg.toString());
    }

    // 广播消息给所有连接的用户
    private void broadcastMessage(String message) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                sendMessage(session, message);
            }
        });
    }

    // 发送消息给指定用户
    private void sendMessageToUser(Long userId, String message) {
        String sessionId = userSessions.get(userId);
        if (sessionId != null) {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                sendMessage(session, message);
            }
        }
    }

    // 发送消息给指定会话
    private void sendMessage(WebSocketSession session, String message) {
        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            log.error("发送WebSocket消息失败", e);
        }
    }

    // 移除会话
    private void removeSession(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.remove(userId);
        }
        sessions.remove(session.getId());
    }

    // 获取当前在线用户数
    public static int getOnlineUserCount() {
        return sessions.size();
    }
}