package xyz.luoxy.selenium;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.html5.LocalStorage;
import org.openqa.selenium.html5.WebStorage;
import org.openqa.selenium.remote.Augmenter;

import java.util.Date;
import java.util.Set;

/**
 * @author luoxy
 */
public class SessionUtil {

    public static JSONObject getSession(WebDriver driver) {
        JSONObject session = new JSONObject();
        Set<Cookie> set = driver.manage().getCookies();
        session.put("cookie", set);

        JSONArray lc = new JSONArray();
        WebStorage webStorage = (WebStorage) new Augmenter().augment(driver);
        LocalStorage localStorage = webStorage.getLocalStorage();
        localStorage.keySet().forEach(c -> {
            try {
                JSONObject item = new JSONObject();
                item.put("key", c);
                item.put("value", localStorage.getItem(c));
                lc.add(item);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        session.put("lc", lc);
        return session;
    }

    public static void setSession(WebDriver driver, JSONObject session) {
        JSONArray cookies = session.getJSONArray("cookie");
        cookies.forEach(o -> {
            try {
                JSONObject m = (JSONObject) o;
                Cookie.Builder builder = new Cookie.Builder(m.getString("name"), m.getString("value"));
                String domain = m.getString("domain");
                String path = m.getString("path");
                Boolean secure = m.getBoolean("secure");
                Boolean httpOnly = m.getBoolean("httpOnly");
                builder.domain(domain).path(path).isSecure(secure).isHttpOnly(httpOnly);

                Long expiry = m.getLong("expiry");
                if (expiry != null) {
                    builder.expiresOn(new Date(expiry));
                }
                Cookie cookie = builder.build();
                driver.manage().addCookie(cookie);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        JSONArray lc = session.getJSONArray("lc");
        WebStorage webStorage = (WebStorage) new Augmenter().augment(driver);
        LocalStorage localStorage = webStorage.getLocalStorage();
        lc.forEach(o -> {
            try {
                JSONObject item = (JSONObject) o;
                String key = item.getString("key");
                String value = item.getString("value");
                localStorage.setItem(key, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }


    public static JSONObject getSessionV2(WebDriver driver) {
        JSONObject session = new JSONObject();
        Set<Cookie> set = driver.manage().getCookies();
        session.put("cookie", set);

        JSONObject lc = new JSONObject();
        WebStorage webStorage = (WebStorage) new Augmenter().augment(driver);
        LocalStorage localStorage = webStorage.getLocalStorage();
        localStorage.keySet().forEach(c -> {
            lc.put(c, localStorage.getItem(c));
        });
        session.put("lc", lc);
        return session;
    }

    public static void setSessionV2(WebDriver driver, JSONObject session) {
        JSONArray cookies = session.getJSONArray("cookie");
        cookies.forEach(o -> {
            try {
                JSONObject m = (JSONObject) o;
                Cookie.Builder builder = new Cookie.Builder(m.getString("name"), m.getString("value"));
                String domain = m.getString("domain");
                String path = m.getString("path");
                Boolean secure = m.getBoolean("secure");
                Boolean httpOnly = m.getBoolean("httpOnly");
                builder.domain(domain).path(path).isSecure(secure).isHttpOnly(httpOnly);

                Long expiry = m.getLong("expiry");
                if (expiry != null) {
                    builder.expiresOn(new Date(expiry));
                }
                Cookie cookie = builder.build();
                driver.manage().addCookie(cookie);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        JSONObject lc = session.getJSONObject("lc");
        WebStorage webStorage = (WebStorage) new Augmenter().augment(driver);
        LocalStorage localStorage = webStorage.getLocalStorage();
        lc.forEach((k, v) -> {
            localStorage.setItem(k, (String) v);
        });
    }
}