
    package javawork1.dorm.controller; // 注意：这里的包名必须和你实际的一致

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

    // @RestController 告诉 Spring Boot：这是一个接待员，它返回的数据直接塞进网络传输流里。
    @RestController
    public class TestController {

        // @GetMapping 规定了暗号：只有外部访问 "/test" 这个路径时，才执行这个方法。
        @GetMapping("/test")
        public String testConnection() {
            System.out.println("【后端控制台】收到外部网络请求！");
            return "【后端中枢回应】：宿舍报修系统 API 握手成功！前后端分离通道已打通。";
        }
    }

