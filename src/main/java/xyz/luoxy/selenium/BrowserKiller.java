package xyz.luoxy.selenium;

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author luoxy
 */
public class BrowserKiller {
    private static final Logger logger = LoggerFactory.getLogger(BrowserKiller.class);
    public static boolean needKill = true;

    public static void run(BrowserType browserType) {
        if (!needKill) {
            return;
        }

        try {
            innerRun(browserType);
        } catch (IOException e) {
            logger.error("关闭浏览器异常: {}", e.getMessage());
        }
    }

    private static void innerRun(BrowserType browserType) throws IOException {
        logger.info("kill Browser");

        List<String> commands = new ArrayList<>();
        commands.add("taskkill");
        commands.add("/F");
        String username = System.getProperty("user.name");
        if (username != null) {
            String filter = "/FI \"USERNAME eq " + username + "\"";
            commands.add(filter);
        }

        if (browserType == BrowserType.Chrome) {
            commands.add("/IM chromedriver.exe");
            commands.add("/IM chrome.exe");
        } else if (browserType == BrowserType.Firefox) {
            commands.add("/IM geckodriver.exe");
            commands.add("/IM firefox.exe");

        } else {
            logger.error("不支持的浏览器类型");
            return;
        }


        String commondStr = Joiner.on(" ").join(commands);

        // 写到脚本文件，直接执行会提示/FI 这个参数有问题
        String tomcatHome = System.getProperty("catalina.home");
        if (tomcatHome == null) {
            tomcatHome = ".";
        }
        String path = tomcatHome + File.separator + "kill.bat";
        FileWriter fileWriter = new FileWriter(path);
        fileWriter.append(commondStr);
        fileWriter.flush();
        fileWriter.close();

        commands.clear();
        commands.add(path);

        ProcessBuilder processBuilder = new ProcessBuilder();

        processBuilder.command(commands);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(processBuilder.command().toString() + " --->: " + line);
            }
        }

        try {
            process.waitFor();
            int exit = process.exitValue();
            if (exit != 0) {
                logger.info("执行异常");
            } else {
                logger.info("执行完毕");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
