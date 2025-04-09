package main.java.com.nightmare.code;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder;

public class UbuntuController {
    private Process process;
    private Context context;
    private double progress = 0.0;
    private final double step = 11.0;
    private File progressFile;
    private boolean vsCodeStarting = false;
    private boolean webViewHasOpen = false;
    private String lastLine = "";

    public UbuntuController(Context context) {
        this.context = context;
        this.progressFile = new File(context.getFilesDir(), "progress");
    }

    public void startUbuntu() {
        try {
            // 初始化环境
            initEnvironment();

            // 创建PTY终端
            createPtyTerminal();

            // 启动VS Code
            startVSCode();
        } catch (IOException e) {
            Log.e("UbuntuController", "Error starting Ubuntu", e);
        }
    }

    private void initEnvironment() throws IOException {
        // 创建必要目录
        createDirectories();

        // 设置环境变量
        Map<String, String> env = new HashMap<>();
        env.put("HOME", context.getFilesDir() + "/home");
        env.put("PATH", context.getFilesDir() + "/usr/bin");

        // 创建符号链接
        createSymbolicLinks();
    }

    private void createDirectories() throws IOException {
        new File(context.getFilesDir(), "tmp").mkdirs();
        new File(context.getFilesDir(), "home").mkdirs();
        new File(context.getFilesDir(), "usr/bin").mkdirs();
    }

    private void createSymbolicLinks() throws IOException {
        String libPath = context.getApplicationInfo().nativeLibraryDir;
        String[] androidFiles = {
                "libbash.so", "libbusybox.so", "liblibtalloc.so.2.so",
                "libloader.so", "libproot.so", "libsudo.so"
        };

        for (String file : androidFiles) {
            String source = libPath + "/" + file;
            String target = context.getFilesDir() + "/usr/bin/" +
                    file.replaceFirst("^lib|\\.so$", "");
            createLink(source, target);
        }
    }

    private void createLink(String source, String target) throws IOException {
        File targetFile = new File(target);
        if (targetFile.exists()) {
            targetFile.delete();
        }
        Runtime.getRuntime().exec("ln -s " + source + " " + target);
    }

    private void createPtyTerminal() throws IOException {
        List<String> command = new ArrayList<>();
        command.add(context.getFilesDir() + "/usr/bin/proot");
        command.add("-b");
        command.add(context.getFilesDir() + "/home:/root");
        command.add("-b");
        command.add(context.getFilesDir() + "/tmp:/tmp");
        command.add("/usr/bin/bash");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(context.getFilesDir()));
        pb.environment().putAll(getEnvironment());
        process = pb.start();

        // 处理进程输出
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lastLine = line;
                    Log.d("PTY Output", line);
                }
            } catch (IOException e) {
                Log.e("PTY", "Error reading process output", e);
            }
        }).start();
    }

    private Map<String, String> getEnvironment() {
        Map<String, String> env = new HashMap<>();
        env.put("HOME", context.getFilesDir() + "/home");
        env.put("PATH", context.getFilesDir() + "/usr/bin");
        return env;
    }

    private void startVSCode() throws IOException {
        vsCodeStarting = true;
        if (process != null) {
            process.getOutputStream().write("start_vs_code\n".getBytes());
            process.getOutputStream().flush();
        }
    }

    public void updateProgress(int value) {
        progress = value / step;
        try {
            progressFile.getParentFile().mkdirs();
            progressFile.createNewFile();
            java.nio.file.Files.write(progressFile.toPath(),
                    String.valueOf(value).getBytes());
        } catch (IOException e) {
            Log.e("Progress", "Error updating progress", e);
        }
    }

    public void syncProgress() {
        try {
            progressFile.getParentFile().mkdirs();
            progressFile.createNewFile();
            progressFile.toPath().getFileSystem().newWatchService();
        } catch (IOException e) {
            Log.e("Progress", "Error syncing progress", e);
        }
    }

    public void createBusyboxLinks() {
        String[] commands = {
                "xz", "realpath", "basename", "awk", "bzip2", "cat", "chmod",
                "cp", "curl", "cut", "du", "file", "find", "grep", "gzip",
                "head", "id", "lscpu", "mkdir", "rm", "sed", "tar", "xargs",
                "uname", "stat"
        };

        for (String cmd : commands) {
            try {
                createLink(context.getFilesDir() + "/usr/bin/busybox",
                        context.getFilesDir() + "/usr/bin/" + cmd);
            } catch (IOException e) {
                Log.e("Busybox", "Error creating link for " + cmd, e);
            }
        }
    }

    public String getLastLine() {
        return lastLine;
    }
}
