package com.travelsky.base.common.context;

public class Principal<AUTHENTICATION> {

    private String principal;
    private LoginSource source;
    private String ipAddress;
    private AUTHENTICATION authentication;

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public LoginSource getSource() {
        return source;
    }

    public void setSource(LoginSource source) {
        this.source = source;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public AUTHENTICATION getAuthentication() {
        return authentication;
    }

    public void setAuthentication(AUTHENTICATION authentication) {
        this.authentication = authentication;
    }

    public enum LoginSource {
        WEB, CLIENT,
    }
}
