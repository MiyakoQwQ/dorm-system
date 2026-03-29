package javawork1.dorm.client;

import javafx.application.Application;

// 极其重要：这个类绝对不能继承 Application！这就是用来欺骗 JVM 的伪装外壳。
public class MainLauncher {
    public static void main(String[] args) {
        // 在这里，手动调用你真正的前端界面类
        Application.launch(DormClientApp.class, args);
    }
}
