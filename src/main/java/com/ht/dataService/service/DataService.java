package com.ht.dataService.service;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.*;


@Service
public class DataService {
    @PostConstruct
    public void run(){
        String loginURL = "https://jinggai.bxzykj.com/hanhai/login/loginCheck";
        String dataURL = "https://jinggai.bxzykj.com/hanhai/getOtherLogsList";
        String URL = "http://www.baidu.com";
        CookieStore cookieStore = new BasicCookieStore();
//        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
        HttpPost httpPost = new HttpPost(loginURL);
        Map<String, String> loginMap = new HashMap<>();
        Map<String, String> formMap = new HashMap<>();
        loginMap.put("code","test123");
        loginMap.put("password","123456");
        formMap.put("moteui", "all");
        formMap.put("current", "0");
        formMap.put("limit", "10");
        formMap.put("TerminalTypeId", "4");
//        formMap.put("EUI", "3853014414640001");
        CloseableHttpResponse response = null;
        String result = null;
        try{
            List<NameValuePair> list = new ArrayList<>();
            Iterator<Map.Entry<String, String>> loginMapIterator = loginMap.entrySet().iterator();
            Iterator<Map.Entry<String, String>> formMapIterator = formMap.entrySet().iterator();
            while (loginMapIterator.hasNext()) {
                Map.Entry<String, String> elem = loginMapIterator.next();
                list.add(new BasicNameValuePair(elem.getKey(), elem.getValue()));
            }
            if (list.size() > 0) {
                UrlEncodedFormEntity loginEntity = new UrlEncodedFormEntity(list, "UTF-8");
                httpPost.setEntity(loginEntity);
            }
            response = httpClient.execute(httpPost);
            System.out.println("登陆请求结果为：" + response.getStatusLine().getStatusCode());
            List<Cookie> cookies = cookieStore.getCookies();
            System.out.println(cookies.get(0));
            list.clear();
            while (formMapIterator.hasNext()) {
                Map.Entry<String, String> elem = formMapIterator.next();
                list.add(new BasicNameValuePair(elem.getKey(), elem.getValue()));
            }
            if (list.size() > 0) {
                UrlEncodedFormEntity loginEntity = new UrlEncodedFormEntity(list, "UTF-8");
                httpPost.setEntity(loginEntity);
            }

            URI uri = new URI(dataURL);
            httpPost = new HttpPost(dataURL);
            httpPost.setHeader("Cookies",cookies.get(0).toString());
            while (true){
//                httpPost.setURI(uri);
//                httpPost.setHeader("Cookies",cookies.get(0).toString());
                response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() == 200) {
                    HttpEntity resEntity = response.getEntity();
                    String message = EntityUtils.toString(resEntity, "utf-8");
                    System.out.println(message);
                } else {
                    System.out.println("请求失败");
                    System.out.println(response.getStatusLine().getStatusCode());
                }
//                System.out.println(response.getEntity().getContent());
//                httpPost.releaseConnection();
                Thread.sleep(2000);
            }
//            while (true){
//                httpPost.setHeader("Cookies",cookies.get(0).toString());
//            }
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
