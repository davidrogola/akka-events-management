package helpers;

import akka.discovery.awsapi.ecs.AsyncEcsServiceDiscovery;
import com.typesafe.config.Config;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import scala.util.Either;

import java.io.IOException;
import java.net.InetAddress;

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

     public InetAddress getContainerAddress(){
         final Either<String,InetAddress> address =  AsyncEcsServiceDiscovery.getContainerAddress();
         if(address.isLeft())
         {
             System.err.println("Unable to get container address, so exiting -"+ address.left().get());
             System.exit(1);
         }
         return  address.right().get();
     }

     public  String getAkkaManagementPort(){
         return  configuration.getString("akka.management.http.port");
     }

     private String getNetworkInterfaceMac(){
         return sendGetRequest(configuration.getString("app.aws.network_interface_mac"));
     }


     public   String sendGetRequest(String endpoint){
         try {
             CloseableHttpClient httpClient = HttpClients.createDefault();
             HttpGet httpGet = new HttpGet(endpoint);

             CloseableHttpResponse response = httpClient.execute(httpGet);
             org.apache.http.HttpEntity httpEntity = response.getEntity();
             String responseString = EntityUtils.toString(httpEntity, "UTF-8");
             httpClient.close();
             return responseString;
         } catch (IOException e) {
             e.printStackTrace();
             return  null;
         }
     }

     public  String getStringResponse(HttpEntity httpEntity){
         try {
             return  EntityUtils.toString(httpEntity, "UTF-8");
         } catch (IOException e) {
             e.printStackTrace();
             return  null;
         }
     }
 }
