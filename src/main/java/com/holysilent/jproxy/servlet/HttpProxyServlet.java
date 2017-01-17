package com.holysilent.jproxy.servlet;

import com.holysilent.jproxy.constant.Constants;
import com.holysilent.jproxy.utils.PropertiesUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Created by silent on 2017/1/6.
 */
public class HttpProxyServlet extends HttpServlet{
    private String targetURL;
    private String cookieExclude;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void init(ServletConfig config) throws ServletException {
        this.targetURL = PropertiesUtils.get(Constants.TARGET_APP_BASE_URL) == null ? "" : PropertiesUtils.get(Constants.TARGET_APP_BASE_URL);
        this.cookieExclude = PropertiesUtils.get("cookie-exclude") == null ? "" : PropertiesUtils.get("cookie-exclude");
        logger.info("param {} : {}", new Object[]{Constants.TARGET_APP_BASE_URL, this.targetURL});
        logger.info("param {} : {}", new Object[]{Constants.COOKIE_EXCLUDE, this.cookieExclude});
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(req.getSession().getAttribute("httpclient")==null){
//            HttpClient httpClient = HttpClients.createDefault();
            BasicCookieStore cookieStore = new BasicCookieStore();
            HttpClient httpClient =  HttpClients.custom().setDefaultCookieStore(cookieStore).build();
            req.getSession().setAttribute("httpclient",httpClient);
            req.getSession().setAttribute("cookieStore", cookieStore);
        }
        super.service(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (StringUtils.isBlank(this.targetURL)) {
            resp.setStatus(404);
            resp.getWriter().write("<h1>WARNING : target url is not configured.</h1>");
            resp.getWriter().flush();
            resp.getWriter().close();
            return;
        }

        String reqURIStr = req.getRequestURI();
        String queryStr = req.getQueryString();
        String targetURL = this.targetURL + reqURIStr + (StringUtils.isBlank(queryStr) ? "" : "?"+queryStr);
        logger.info("targetURL : {}", targetURL);
        HttpGet get = new HttpGet(targetURL);
        HttpClientContext context = HttpClientContext.create();

        setSendHttpHead(get, req);
        setSendCookie(req, context);

        CloseableHttpResponse httpResponse = null;
        try {
            CloseableHttpClient httpClient = (CloseableHttpClient)req.getSession().getAttribute("httpclient");
//            get.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build());
            httpResponse = httpClient.execute(get,context);

            setRespHttpHeader(httpResponse, resp);
            setRespCookie(httpResponse,resp);
            writeResponse(httpResponse,resp);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(httpResponse!=null) {
                    httpResponse.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    }

    private void setSendHttpHead(HttpRequest httpRequest, HttpServletRequest req) {
        Enumeration headers = req.getHeaderNames();

        while(headers.hasMoreElements()){
            String name = headers.nextElement().toString();
            String value = req.getHeader(name);
            httpRequest.setHeader(name,value);
        }
//        httpRequest.setHeader("host", req.getSession().getAttribute("domain").toString());
    }

    private void setSendCookie(HttpServletRequest req, HttpClientContext context) {
        Cookie[] cookies=req.getCookies();
        BasicCookieStore cookieStore = new BasicCookieStore();
        if(cookies!=null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().startsWith(Constants.JPROXY_COOKIE_START)) {
                    BasicClientCookie httpcookie = new BasicClientCookie(cookie.getName().substring(Constants.JPROXY_COOKIE_START.length()), cookie.getValue());
//                    httpcookie.setDomain(req.getSession().getAttribute("domain").toString());
                    httpcookie.setPath("/");
                    cookieStore.addCookie(httpcookie);
                }
            }
        }
        context.setCookieStore(cookieStore);
    }

    private void setRespHttpHeader(CloseableHttpResponse httpResponse,HttpServletResponse resp) {
        try {
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
                Header[] locations = httpResponse.getHeaders("Location");
                if (locations.length > 0) {
                    String location = locations[0].getValue();
                    String redirct = null;
                    if (location.startsWith(Constants.URL_HTTP_HEAD)) {
                        URL re = new URL(location);
                        if (re.getQuery() == null || re.getQuery().equals("")) {
                            redirct = re.getPath();
                        } else {
                            redirct = re.getPath() + "?" + re.getQuery();
                        }
                    } else {
                        redirct = location;
                    }
                    resp.setHeader("Location", redirct);
                }
            }
            Header[] headers = httpResponse.getAllHeaders();
            for(Header header:headers){
                if (header.getName().trim().toLowerCase().equals("set-cookie")) {
                    continue;
                }
                if (StringUtils.isNotBlank(this.cookieExclude) && this.cookieExclude.contains(header.getName().trim().toLowerCase())) {
                    continue;
                }
                resp.setHeader(header.getName(),header.getValue());
            }

        }catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void setRespCookie(CloseableHttpResponse httpResponse,HttpServletResponse resp) {
        Header[] headers = httpResponse.getHeaders("Set-Cookie");
        if(headers.length > 0) {
            for(Header header: headers) {
                if (StringUtils.isNotBlank(this.cookieExclude) && this.cookieExclude.contains(header.getName().trim().toLowerCase())) {
                    continue;
                }
                String[] cookies = header.getValue().split(";");
                String[] strs = cookies[0].split("=");
                if (strs.length == 2) {
                    Cookie cookie = new Cookie(Constants.JPROXY_COOKIE_START + strs[0], strs[1]);
                    cookie.setPath("/");
                    resp.addCookie(cookie);
                }
            }
        }
    }

    private void writeResponse(CloseableHttpResponse httpResponse,HttpServletResponse resp) throws IOException {
        resp.setStatus(httpResponse.getStatusLine().getStatusCode());
        HttpEntity entity = httpResponse.getEntity();
        if(entity==null){
            return;
        }
        InputStream is =entity.getContent();
        byte[] byteArr = new byte[1024];
        int readCount = 0;
        readCount = is.read(byteArr);

        OutputStream outputStream = resp.getOutputStream();
        while (readCount != -1) {
            outputStream.write(byteArr, 0, readCount);
            readCount = is.read(byteArr);
        }
        outputStream.flush();
        outputStream.close();
        is.close();

    }
}
