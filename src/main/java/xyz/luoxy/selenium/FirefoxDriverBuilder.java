package xyz.luoxy.selenium;

import org.openqa.selenium.firefox.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author luoxy
 */
public class FirefoxDriverBuilder {
    public static final Logger log = LoggerFactory.getLogger(FirefoxDriverBuilder.class);

    private boolean headless = false;

    /**
     * 下面三个配置项是相关的
     * userProfile: 只有此值为true，下面两个配置项才生效
     */
    private boolean userProfile = false;

    private String downloadDir = null;
    private Proxy proxy = null;

    public static FirefoxDriverBuilder create() {
        return new FirefoxDriverBuilder();
    }

    public FirefoxDriverBuilder headless(boolean headless) {
        this.headless = headless;
        return this;
    }

    public FirefoxDriverBuilder userProfile(boolean use) {
        this.userProfile = use;
        return this;
    }

    public FirefoxDriverBuilder proxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    public FirefoxDriverBuilder downloadDir(String downloadDir) {
        this.downloadDir = downloadDir;
        return this;
    }

    public RemoteWebDriver build() {
        RemoteWebDriver driver;
        FirefoxOptions options = new FirefoxOptions();
        if (downloadDir != null) {
            ProfilesIni init = new ProfilesIni();
            FirefoxProfile profile = init.getProfile("default-release");

            // https://www.toolsqa.com/selenium-webdriver/how-to-download-files-using-selenium/
            profile.setPreference("browser.download.dir", downloadDir);
            profile.setPreference("browser.download.folderList", 2);
            profile.setPreference("browser.download.useDownloadDir", true);

            // excel自动保存，不再提示保存类型
            profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/msexcel;");
            profile.setPreference("pdfjs.disabled", true);
            profile.setPreference("browser.helperApps.alwaysAsk.force", false);
            profile.setPreference("browser.download.manager.showWhenStarting", false);
            profile.setPreference("browser.download.manager.alertOnEXEOpen", false);
            profile.setPreference("browser.download.manager.focusWhenStarting", false);
            profile.setPreference("browser.download.manager.useWindow", false);
            profile.setPreference("browser.download.manager.showAlertOnComplete", false);
            profile.setPreference("browser.download.manager.closeWhenDone", false);

            options.setProfile(profile);
        }

        if (headless) {
            log.info("use firefox headless");
            FirefoxBinary firefoxBinary = new FirefoxBinary();
            firefoxBinary.addCommandLineOptions("--headless");
            options.setBinary(firefoxBinary);
        }

        if (proxy != null) {
            String hp = String.format("%s:%d", proxy.getIp(), proxy.getPort());
            options.setCapability("proxy", new org.openqa.selenium.Proxy().setHttpProxy(hp).setSslProxy(hp));
        }

        driver = new FirefoxDriver(options);
        return driver;
    }

    public static RemoteWebDriver build(boolean headless, String downloadDir) {
        return FirefoxDriverBuilder.create().headless(headless).downloadDir(downloadDir).build();
    }
}
