package helpers;

import com.typesafe.config.Config;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public  class  AwsConfigHelper
 {
     Config configuration = null;
     public  AwsConfigHelper(Config config){
          configuration = config;
     }

     public  String getHostAddress(){
        return sendGetRequest(configuration.getString("app.aws.ipv4"));
     }

     public String getNetworkInterfaceIPv4Address(){
         String mac = getNetworkInterfaceMac();
         String endpoint = String.format(configuration.getString("app.aws.network_interface_ipv4"),mac);
         return sendGetRequest(endpoint);
     }

     private String getNetworkInterfaceMac(){
         return sendGetRequest(configuration.getString("app.aws.network_interface_mac"));
     }


     private  String sendGetRequest(String endpoint){
         try {
             CloseableHttpClient httpClient = HttpClients.createDefault();
             HttpGet httpGet = new HttpGet(endpoint);

             CloseableHttpResponse response = httpClient.execute(httpGet);
             org.apache.http.HttpEntity httpEntity = response.getEntity();

             String responseString = EntityUtils.toString(httpEntity, "UTF-8");
             httpClient.close();
             System.out.println(responseString + " AWS Response String");

             return responseString;
         } catch (IOException e) {
             e.printStackTrace();
             return  null;
         }
     }
 }
