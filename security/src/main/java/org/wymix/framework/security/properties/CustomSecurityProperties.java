package org.wymix.framework.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("security")
public class CustomSecurityProperties {

    private boolean formLoginEnable = true;

    private String loginPage;

    private String loginProcessingUrl = "/login";

    private String ignoreUrls;

    private String logoutUrl = "/logout";

    private JwtProperties jwt = new JwtProperties();

    public JwtProperties getJwt() {
        return jwt;
    }

    public void setJwt(JwtProperties jwt) {
        this.jwt = jwt;
    }

    public boolean isFormLoginEnable() {
        return formLoginEnable;
    }

    public void setFormLoginEnable(boolean formLoginEnable) {
        this.formLoginEnable = formLoginEnable;
    }

    public String getLoginPage() {
        return loginPage;
    }

    public void setLoginPage(String loginPage) {
        this.loginPage = loginPage;
    }

    public String getLoginProcessingUrl() {
        return loginProcessingUrl;
    }

    public void setLoginProcessingUrl(String loginProcessingUrl) {
        this.loginProcessingUrl = loginProcessingUrl;
    }

    public String getIgnoreUrls() {
        return ignoreUrls;
    }

    public void setIgnoreUrls(String ignoreUrls) {
        this.ignoreUrls = ignoreUrls;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public void setLogoutUrl(String logoutUrl) {
        this.logoutUrl = logoutUrl;
    }

    public static class JwtProperties {
        private boolean randomSecret = false;
        private String secret;
        private long expiredTime = 3600;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpiredTime() {
            return expiredTime;
        }

        public void setExpiredTime(long expiredTime) {
            this.expiredTime = expiredTime;
        }

        public boolean isRandomSecret() {
            return randomSecret;
        }

        public void setRandomSecret(boolean randomSecret) {
            this.randomSecret = randomSecret;
        }
    }
}
