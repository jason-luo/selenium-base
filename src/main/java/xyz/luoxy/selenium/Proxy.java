package xyz.luoxy.selenium;


/**
 * @author luoxy
 */
public class Proxy {
    private String ip;
    private Integer port;
    private String user;
    private String password;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Proxy(){

    }
    public Proxy(String proxyStr){
        // user/pwd@ip:port
        String ipPart;
        String authPart = null;
        String[] split = proxyStr.split("@");
        if (split.length == 1) {
            ipPart = split[0];
        } else {
            authPart = split[0];
            ipPart = split[1];
        }

        String[] addrs = ipPart.split(":");
        setIp(addrs[0]);
        setPort(Integer.parseInt(addrs[1]));
        if (authPart != null) {
            String[] authInfo = authPart.split("/");
            setUser(authInfo[0]);
            setPassword(authInfo[1]);
        }
    }
}
