package org.wymix.framework.core.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public class Message implements Serializable {

    private String id;
    private SendType[] sendTypes;
    private String subject;
    private Set<String> to;
    private Set<String> toUserAccounts;
    private Set<String> toUserPhones;
    private Set<String> cc;
    private String content;
    private List<String> attachments;

    public SendType[] getSendTypes() {
        return sendTypes;
    }

    public void setSendTypes(SendType[] sendTypes) {
        this.sendTypes = sendTypes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Set<String> getTo() {
        return to;
    }

    public void setTo(Set<String> to) {
        this.to = to;
    }

    public Set<String> getToUserAccounts() {
        return toUserAccounts;
    }

    public void setToUserAccounts(Set<String> toUserAccounts) {
        this.toUserAccounts = toUserAccounts;
    }

    public Set<String> getToUserPhones() {
        return toUserPhones;
    }

    public void setToUserPhones(Set<String> toUserPhones) {
        this.toUserPhones = toUserPhones;
    }

    public Set<String> getCc() {
        return cc;
    }

    public void setCc(Set<String> cc) {
        this.cc = cc;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }

    public enum SendType {
        O, E, P
    }
}
