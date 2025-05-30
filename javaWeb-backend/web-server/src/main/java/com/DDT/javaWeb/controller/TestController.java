package com.DDT.javaWeb.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test/websocket")
    public String testWebSocket() {
        return "WebSocket服务正在运行";
    }
}