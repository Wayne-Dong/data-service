package com.ht.dataService.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ht.dataService.model.Data;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;


@Service
public class DataService {
    @Value(value = "${login.code}")
    private String code;
    @Value(value = "${login.password}")
    private String password;
    @Value(value = "${postParam.moteui}")
    private String moteui;
    @Value(value = "${postParam.current}")
    private String current;
    @Value(value = "${postParam.limit}")
    private String limit;
    @Value(value = "${postParam.TerminalTypeId}")
    private String TerminalTypeId;
    @Value(value = "${postParam.EUI}")
    private String EUI;
    @Value(value = "${postParam.loginURL}")
    private String loginURL;
    @Value(value = "${postParam.dataURL}")
    private String dataURL;
    @Value(value = "${mqtt.broker}")
    String broker;
    @Value(value = "${mqtt.clientId}")
    String clientId;
    @Value(value = "${mqtt.topicName}")
    String topicName;
    @PostConstruct
    public void run(){
        //mqtt相关配置
        MqttClient client;
        MqttTopic topic;

        MqttMessage mqttMessage = new MqttMessage();
        MemoryPersistence persistence = new MemoryPersistence();

        //HTTP client相关配置
        CookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
        HttpPost httpPost = new HttpPost(loginURL);
        Map<String, String> loginMap = new HashMap<>();
        Map<String, String> formMap = new HashMap<>();
        loginMap.put("code",code);
        loginMap.put("password",password);
        formMap.put("moteui", moteui);
        formMap.put("current", current);
        formMap.put("limit", limit);
        formMap.put("TerminalTypeId", TerminalTypeId);
        formMap.put("EUI", EUI);
        CloseableHttpResponse response = null;
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
            list.clear();
            httpPost = new HttpPost(dataURL);
            while (formMapIterator.hasNext()) {
                Map.Entry<String, String> elem = formMapIterator.next();
                list.add(new BasicNameValuePair(elem.getKey(), elem.getValue()));
            }
            if (list.size() > 0) {
                UrlEncodedFormEntity dataEntity = new UrlEncodedFormEntity(list, "UTF-8");
                httpPost.setEntity(dataEntity);
            }
            Gson gson = new Gson();
            httpPost.setHeader("Cookies",cookies.get(0).toString());
            while (true){
                response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() == 200) {
                    //解析数据
                    HttpEntity responseEntity = response.getEntity();
                    String message = EntityUtils.toString(responseEntity, "utf-8");
                    ArrayList<Data> dataArrayList = gson.fromJson(message,new TypeToken<ArrayList<Data>>() {}.getType());
                    Data data = dataArrayList.get(0);
                    String dataJson = gson.toJson(data);
                    EntityUtils.consume(responseEntity);
                    String payloadbase64 = data.getPayloadbase64();
                    String status = payloadbase64.substring(18,20) == "00"?"关盖":"开盖";
                    System.out.println("TypeName: " + data.getTypeName() +", " +"moteeui: " + data.getMoteeui() + ", " + "status: " + status);
                    //MQTT测试数据
                    //向MQTT推送数据
                    client = new MqttClient(broker, clientId, persistence);
                    MqttConnectOptions options = new MqttConnectOptions();
                    options.setCleanSession(true);
                    options.setConnectionTimeout(10);
                    options.setKeepAliveInterval(20);
                    options.setUserName("admin");
                    options.setPassword("Ht@123456".toCharArray());
                    System.out.println("正在连接MQTT: " + broker + " ......");
                    client.connect(options);
                    System.out.println("连接成功！");
                    topic = client.getTopic(topicName);

                    mqttMessage.setQos(1);
                    mqttMessage.setRetained(false);
                    MqttToken token =topic.publish(mqttMessage);
                    token.waitForCompletion();
                    //释放资源
                    httpPost.releaseConnection();
                } else {
                    System.out.println("请求失败");
                    System.out.println(response.getStatusLine().getStatusCode());
                }
                Thread.sleep(2000);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try{
                response.close();
            }catch (Exception e){
                e.printStackTrace();
            }

        }


    }
}
