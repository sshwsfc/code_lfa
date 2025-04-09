package main.java.com.nightmare.code;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class JavaUbuntuActivity extends AppCompatActivity {
    private WebView webView;
    private Process process;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_java_ubuntu);

        // 初始化WebView
        webView = findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        // 启动Ubuntu环境
        startUbuntu();
    }

    private UbuntuController ubuntuController;

    private void startUbuntu() {
        ubuntuController = new UbuntuController(this);
        ubuntuController.startUbuntu();

        // 监听输出并更新UI
        new Thread(() -> {
            while (true) {
                String output = ubuntuController.getLastLine();
                if (output != null && !output.isEmpty()) {
                    String finalOutput = output;
                    runOnUiThread(() -> webView.loadUrl(
                            "javascript:terminal.write('" + finalOutput + "\\n')"));
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (process != null) {
            process.destroy();
        }
    }
}
