package xyz.luoxy.selenium;

/**
 * @author luoxy
 */

public enum BrowserType {
    Unknown("_"),
    Firefox("firefox"),
    Chrome("chrome");
    private final String browser;

    BrowserType(String browser) {
        this.browser = browser;
    }

    public static BrowserType from(String browser) {
        switch (browser) {
            case "firefox":
                return Firefox;
            case "chrome":
                return Chrome;
            default:
                return Unknown;
        }
    }

}
