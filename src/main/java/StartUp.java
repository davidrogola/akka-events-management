
import actors.Event;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.discovery.awsapi.ecs.EcsServiceDiscovery;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.Http;
import akka.http.javadsl.ConnectHttp;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import akka.stream.javadsl.Flow;
import  akka.NotUsed;
import akka.http.javadsl.model.*;
import akka.stream.ActorMaterializer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import messages.EventMessageExtrator;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import routes.EventRouter;
import scala.util.Either;

import java.io.IOException;
import java.net.InetAddress;

public class StartUp {


    public static void main(String[] args) {
        try {
            Config configBase = ConfigFactory.load("application.conf");
            String actorSystemName = configBase.getString("app.cluster.name");
            String environment = configBase.getString("app.envType");

            if(isAWS(environment)) {
                InetAddress privateAddress = getContainerAddress();
                String dockerAddress = privateAddress.getHostAddress();

                System.out.println("dockerAddress: " + dockerAddress);
                String hostAddress = getHostAddress(configBase.getString("app.aws_http_endpoint")) ;
                configBase = setUpAkkaManagementConfig(hostAddress,dockerAddress);
            }

            ActorSystem actorSystem = ActorSystem.create(actorSystemName,configBase);

            AkkaManagement.get(actorSystem).start();
            ClusterBootstrap.get(actorSystem).start();

            final Config finalConfigBase = configBase;
            Runnable runnable = ()-> initializeClusterSharding(actorSystem, finalConfigBase);
            Cluster.get(actorSystem).registerOnMemberUp(runnable);

            System.out.println("Cluster Bootstrap Started successfully");

        } catch (Exception e)
        {
           System.out.println("An exception has been thrown :"+ e.getMessage());
        }
    }



    public static void initializeClusterSharding(ActorSystem actorSystem, Config config)
    {
        ClusterShardingSettings settings = ClusterShardingSettings.create(actorSystem);

        ActorRef eventsRegion = ClusterSharding.get(actorSystem).start("Events",Event.props(),
                settings, EventMessageExtrator.eventMessageExtractor);

        initializeHttpServer(actorSystem,eventsRegion,config);

    }

    private static  void initializeHttpServer(ActorSystem actorSystem, ActorRef shardRegion,Config config){
        Http http = Http.get(actorSystem);
        ActorMaterializer materializer = ActorMaterializer.create(actorSystem);

        final LoggingAdapter log = Logging.getLogger(actorSystem,shardRegion);

        EventRouter routes = new EventRouter(log);

        String hostName = config.getString("akka.remote.netty.tcp.bind-hostname");
        int port = config.getInt("app.webapi.http.port");
        Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = routes.createRoutes(actorSystem)
                .flow(actorSystem,materializer);
        http.bindAndHandle(routeFlow, ConnectHttp.toHost(hostName,port),materializer);
        log.info("Server online at http://{}:{}/", hostName, port);
    }



    private static  Config setUpAkkaManagementConfig(String hostAddress,String dockerAddress){
        return ConfigFactory.parseString(
                               String.format("akka.remote.netty.tcp.hostname=%s%n", hostAddress) +
                                  String.format("akka.remote.netty.tcp.bind-hostname=%s%n", dockerAddress) +
                                  String.format("akka.management.http.hostname=%s%n", hostAddress) +
                                  String.format("akka.management.http.bind-hostname=%s%n", dockerAddress))
                .withFallback(ConfigFactory.load("application.conf"));
    }

    private static InetAddress getContainerAddress(){
        final Either<String,InetAddress> address =  EcsServiceDiscovery.getContainerAddress();
        if(address.isLeft())
        {
            System.err.println("Unable to get container address, so exiting -"+ address.left().get());
            System.exit(1);
        }
        return  address.right().get();
    }

    private static  String getHostAddress(String awsEndpoint){
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(awsEndpoint);

            CloseableHttpResponse response = httpClient.execute(httpGet);
            org.apache.http.HttpEntity httpEntity = response.getEntity();

            String responseString = EntityUtils.toString(httpEntity, "UTF-8");
            httpClient.close();
            System.out.println(responseString + " EC2 IP Address");
            return responseString;

        } catch (IOException e) {
            e.printStackTrace();
            return  null;
        }
    }

    private  static  boolean isAWS(String environment){
        return "AWS".equals(environment);
    }


}

