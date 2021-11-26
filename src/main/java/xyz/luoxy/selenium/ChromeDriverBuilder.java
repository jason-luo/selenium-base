package xyz.luoxy.selenium;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ChromeDriverBuilder {
    public static final Logger log = LoggerFactory.getLogger(ChromeDriverBuilder.class);

    private boolean headless = false;

    /**
     * 下面三个配置项是相关的
     * userProfile: 只有此值为true，下面两个配置项才生效
     */
    private boolean userProfile = false;
    private String userDataDir = null;
    private String profileName = null;

    private String downloadDir = null;
    private Proxy proxy = null;

    private ChromeDriverBuilder() {

    }

    public static ChromeDriverBuilder create() {
        return new ChromeDriverBuilder();
    }

    public ChromeDriverBuilder headless(boolean headless) {
        this.headless = headless;
        return this;
    }

    public ChromeDriverBuilder userProfile(boolean use) {
        this.userProfile = use;
        return this;
    }

    public ChromeDriverBuilder proxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    public ChromeDriverBuilder userDataDir(String userDataDir) {
        this.userDataDir = userDataDir;
        return this;
    }

    public ChromeDriverBuilder profileName(String profileName) {
        this.profileName = profileName;
        return this;
    }

    public ChromeDriverBuilder downloadDir(String downloadDir) {
        this.downloadDir = downloadDir;
        return this;
    }

    String readFileFromJar(String path) throws Exception {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            throw new NullPointerException("open file fail:" + path);
        }
        return readFileContent(is);
    }

    String readFile(String path) throws Exception {
        InputStream is = new FileInputStream(path);
        return readFileContent(is);
    }

    void writeFile(String path, String content) throws Exception {
        OutputStream os = new FileOutputStream(path);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            writer.write(content);
        }
    }

    String readFileContent(InputStream is) throws Exception {
        if (is == null) {
            throw new NullPointerException("open file fail");
        }
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            while (true) {
                String s = bufferedReader.readLine();
                if (s == null) {
                    break;
                }
                sb.append(s);
            }
            return sb.toString();
        }
    }

    private String buildProxyPlugin() throws Exception {
        String authPluginZip = "proxy_auth_plugin.zip";
        String manifestJson = readFileFromJar("/proxy_plugin/manifest.json");
        String backgroundJs = readFileFromJar("/proxy_plugin/background.js");
        backgroundJs = String.format(backgroundJs, proxy.getIp(), proxy.getPort(), proxy.getUser(), proxy.getPassword());

        try {
            Files.deleteIfExists(new File(authPluginZip).toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(authPluginZip))) {
            ZipEntry zipEntry = new ZipEntry("manifest.json");
            zipOut.putNextEntry(zipEntry);
            zipOut.write(manifestJson.getBytes());
            zipEntry = new ZipEntry("background.js");
            zipOut.putNextEntry(zipEntry);
            zipOut.write(backgroundJs.getBytes());
        } catch (Exception e) {
            log.error("压缩插件异常");
        }
        return authPluginZip;
    }


    public RemoteWebDriver build() throws Exception {
        if (profileName != null || userDataDir != null) {
            if (!userProfile) {
                throw new Exception("使用了profileName和userDataDir，但是没有开启profile能力");
            }
        }

        RemoteWebDriver driver;
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("disable-infobars");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--ignore-certificate-errors");

        if (headless) {
            log.info("use chrome headless");
            chromeOptions.addArguments("--headless");
        }

        Map<String, Object> chromePrefs = new HashMap<>();
        if (userProfile) {
            // 不指定用户数据目录则使用默认的路径
            if (userDataDir == null) {
                // 使用默认Profile的目录
                String localAppData = System.getenv("LOCALAPPDATA");
                userDataDir = localAppData + "\\Google\\Chrome\\User Data";
            }
            chromeOptions.addArguments("user-data-dir=" + userDataDir);

            // 可以为不同使用者设置不同的Profile，这些profile都在user-data-dir的目录下，命名规则一般为Profile+数字
            String currentProfileDataDir = userDataDir;
            if (profileName != null) {
                chromeOptions.addArguments(String.format("--profile-directory=%s", profileName));
                currentProfileDataDir = userDataDir + "\\" + profileName;
            }

            // 设置下载目录，对于使用指定Profile的情况，直接修改对应Preferences文件
            if (downloadDir != null) {
                String profilePath = currentProfileDataDir + "\\" + "Preferences";

                try {
                    String json = readFile(profilePath);
                    JSONObject jo = JSON.parseObject(json);
                    JSONObject download = jo.getJSONObject("download");
                    if (download == null) {
                        download = new JSONObject();
                    }
                    download.put("default_directory", downloadDir);
                    download.put("directory_upgrade", true);
                    jo.put("download", download);

                    JSONObject savefile = jo.getJSONObject("savefile");
                    if (savefile == null) {
                        savefile = new JSONObject();
                    }
                    savefile.put("default_directory", downloadDir);
                    savefile.put("directory_upgrade", true);
                    jo.put("savefile", savefile);
                    writeFile(profilePath, jo.toJSONString());
                } catch (Exception e) {
                    chromePrefs.put("download.default_directory", downloadDir);
                    chromePrefs.put("savefile.directory_upgrade", true);
                    chromePrefs.put("download.directory_upgrade", true);
                }
            }
        } else {
            if (downloadDir != null) {
                chromePrefs.put("download.default_directory", downloadDir);
                chromePrefs.put("savefile.directory_upgrade", true);
                chromePrefs.put("download.directory_upgrade", true);
            }
        }

        if (proxy != null) {
            if (proxy.getUser() != null) {
                String proxyPluginPath = buildProxyPlugin();
                chromeOptions.addExtensions(new File(proxyPluginPath));
            } else {
                chromeOptions.addArguments("--proxy-server=" + proxy.getIp() + ":" + proxy.getPort());
            }
        }

        // 关闭提示保存密码
        // https://stackoverflow.com/questions/56311000/how-can-i-disable-save-password-popup-in-selenium
        chromePrefs.put("credentials_enable_service", false);
        chromePrefs.put("profile.password_manager_enabled", false);
        chromePrefs.put("profile.default_content_setting_values.notifications", 2);
        chromeOptions.setExperimentalOption("prefs", chromePrefs);
        chromeOptions.addArguments("--disable-web-security");


        chromeOptions.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        // 据说可以解决webdriver=true的问题
        chromeOptions.addArguments("--disable-blink-features");
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");

        ChromeDriver cDriver = new ChromeDriver(chromeOptions);
        // 通过参数解决了，以下作为一个hack方法留在这里
        // 移除navigator下的webdriver属性
        //        Map<String, Object> args = new HashMap<>();
        //        args.put("source", "Object.defineProperty(navigator, 'webdriver', {\n" +
        //            "      get: () => false\n" +
        //            "    })");
        //
        //        cDriver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", args);
        driver = cDriver;
        return driver;
    }
}
