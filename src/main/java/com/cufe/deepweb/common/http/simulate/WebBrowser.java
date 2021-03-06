package com.cufe.deepweb.common.http.simulate;

import java.io.Closeable;
import java.util.List;
import java.util.Optional;

/**
 * 支持multithreaded
 */
public interface WebBrowser {
    /**
     * @param loginURL 登录页面链接
     * @param username 登录用户名
     * @param password 登录密码
     * @param usernameXpath 登录用户名输入框的xpath
     * @param passwordXpath 登录密码输入框的xpath
     * @param submitXpath 登录按钮的xpath
     * @return true if success
     */
    boolean login(String loginURL, String username, String password, String usernameXpath, String passwordXpath, String submitXpath);

    /**
     *
     * 获取页面内容（主要用于获取所有查询链接的算法）
     * @param URL
     * @return
     */
    Optional<String> getPageContent(String URL);

    /**
     * get the links from the specified page
     * @param URL
     * @param collector the link collector which collect links from the page corresponding to the specified URL
     * @return
     */
    List<String> getAllLinks(String URL, LinkCollector collector);

    void clearResource();
}
