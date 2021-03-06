package com.cufe.deepweb.common.http.simulate;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * the Browser implemented by HtmlUnit
 */
public final class HtmlUnitBrowser implements WebBrowser {
    private Logger logger = LoggerFactory.getLogger(HtmlUnitBrowser.class);
    static {
        //close HtmlUnit log output
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }
    private int timeout;
    private CookieManager cookieManager;
    private volatile boolean isLogin;
    ThreadLocal<WebClient> threadClient = new ThreadLocal<WebClient>() {
        @Override
        protected WebClient initialValue() {
            WebClient client = new WebClient(BrowserVersion.BEST_SUPPORTED);
            client.setCookieManager(cookieManager);
            client.getOptions().setCssEnabled(false);//headless browser no need to support css
            client.getOptions().setDownloadImages(false);//headless browser no need to support download imgs
            client.getOptions().setJavaScriptEnabled(true);
            client.getOptions().setThrowExceptionOnFailingStatusCode(false);//wouldn't log when access error
            client.getOptions().setThrowExceptionOnScriptError(false);//wouldn't log when js run error
            client.getOptions().setTimeout(timeout);//set the timeout for browser to connect
            client.getOptions().setDoNotTrackEnabled(true);
            client.getOptions().setHistoryPageCacheLimit(1);//limit the cache number
            client.getOptions().setHistorySizeLimit(1);
            client.setAjaxController(new NicelyResynchronizingAjaxController());
            client.setJavaScriptTimeout(timeout);
            client.waitForBackgroundJavaScript(timeout);
            return client;
        }

        @Override
        public void remove() {
            get().close();
            super.remove();
        }
    };
    /**
     * initialize a new HtmlUnitBrowser
     */
    public HtmlUnitBrowser(CookieManager cookieManager, int timeout){
        this.timeout = timeout;
        this.cookieManager = cookieManager;
        this.isLogin = false;//if cookieManager has preserved the login information

    }



    /**
     * override WebBrowser's method
     * @param loginURL login page URL
     * @param username account name
     * @param password login password
     * @param usernameXpath the xpath of account input
     * @param passwordXpath the xpath of password input
     * @param submitXpath the xpath of submit button
     * @return
     */
    @Override
    public boolean login(String loginURL, String username, String password, String usernameXpath, String passwordXpath, String submitXpath) {
        WebClient client = null;
        try {
            client = threadClient.get();
            HtmlTextInput userNameInput = null;//用户名输入框
            HtmlPasswordInput passwordInput = null;//密码输入框
            HtmlElement button = null;//登录按钮,最好不要限定为必须button
            HtmlPage page = null;
            page = client.getPage(loginURL);
            if ((!StringUtils.isBlank(usernameXpath)) && (!StringUtils.isBlank(passwordXpath)) && (!StringUtils.isBlank(submitXpath))) {//如果xpath都有指定
                List uList = page.getByXPath(usernameXpath);
                List pList = page.getByXPath(passwordXpath);
                List sList = page.getByXPath(submitXpath);
                if (!uList.isEmpty()) {
                    userNameInput = (HtmlTextInput)uList.get(0);
                    userNameInput.setText(username);
                }
                if (!pList.isEmpty()) {
                    passwordInput = (HtmlPasswordInput)pList.get(0);
                    passwordInput.setText(password);
                }
                if (!sList.isEmpty()) {
                    button = (HtmlElement) sList.get(0);
                    button.click();
                    isLogin = true;//修改登录状态为已登录
                    return true;
                }
            }//必须指定xpath，否则无法登录，后续可拓展成指定id等等
            return false;
        } catch (Exception ex) {
            logger.error("Eception happen when get login page", ex);
            return false;
        }
    }

    @Override
    public Optional<String> getPageContent(String URL) {
        WebClient client = null;

        try {
            client = threadClient.get();
            HtmlPage page = client.getPage(URL);
            return Optional.ofNullable(page.getBody().asText());
        } catch (Exception ex) {
            logger.error("Exception happen when get page content from" + URL, ex);
            return Optional.empty();
        }
    }

    public List<String> getAllLinks(String URL, LinkCollector collector) {
        WebClient client = null;

        try {
            client = threadClient.get();
            HtmlPage page = client.getPage(URL);
            return collector.collect(page.asXml(), page.getUrl());
        } catch (Exception ex) {
            logger.error("Exception happen when get page content from {}", URL);
            return Collections.emptyList();
        }
    }

    @Deprecated
    public List<String> getAllLinks(String URL) {
        List<String> links = new ArrayList<>();
        WebClient client = null;

        try {
            client = threadClient.get();
            HtmlPage page = client.getPage(URL);
            URL curURL = page.getUrl();//the URL of current page
            List<HtmlAnchor> anchors = page.getAnchors();
            for (HtmlAnchor anchor : anchors) {
                String anchorURL;
                String hrefAttr = anchor.getHrefAttribute().trim();
                if ("".equals(hrefAttr)) continue;//if the value of href is blank, just jump next

                if (!hrefAttr.startsWith("http")) {//if no start with http, this href is relative address or no use http protocol
                    if (hrefAttr.startsWith(".") || hrefAttr.startsWith("/")) {//if start with . or /, this href is relative address
                        anchorURL = new URL(curURL, hrefAttr).toString();
                    } else { //no use http protocol, just jump next
                        continue;
                    }
                } else {//if start with http, use this href directly
                    anchorURL = hrefAttr;
                }
                links.add(anchorURL);
            }
        }catch (IOException ex) {
            logger.error("IOException happen when get page content", ex);
        } catch (NullPointerException ex) {
            logger.error("NullPointerException happen when get page content", ex);
        } catch (Exception ex) {

        }
        return links;
    }

    @Override
    public void clearResource() {
        threadClient.remove();

    }

    public static void main(String[] args) {

    }
}
