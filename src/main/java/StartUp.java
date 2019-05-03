
import actors.Event;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.discovery.awsapi.ecs.AsyncEcsServiceDiscovery;
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
import helpers.AwsConfigHelper;
import messages.EventMessageExtrator;
import routes.EventRouter;
import scala.util.Either;
import java.net.InetAddress;

public class StartUp {


    public static void main(String[] args) {
        try {
            Config configBase = ConfigFactory.load("application.conf");
            String actorSystemName = configBase.getString("app.cluster.name");
            String environment = configBase.getString("app.aws.envType");
            
            System.out.println("Updated Image found");
            AwsConfigHelper awsConfig = new AwsConfigHelper(configBase);

            if(isAWS(environment)) {
                InetAddress privateAddress = getContainerAddress();
                String dockerAddress = privateAddress.getHostAddress();
                System.out.println("dockerAddress: " + dockerAddress);

                String hostAddress = awsConfig.getHostAddress() ;
                System.out.println("IPV4_Address: " + hostAddress);

              //  System.out.println("NetworkInterfaceIPv4Address: "+awsConfig.getNetworkInterfaceIPv4Address());
               // configBase = setUpAkkaManagementConfig(dockerAddress,dockerAddress);
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
        AwsConfigHelper awsConfig = new AwsConfigHelper(config);

        String hostName = awsConfig.getHostAddress(); // config.getString("akka.remote.netty.tcp.bind-hostname");
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
        final Either<String,InetAddress> address =  AsyncEcsServiceDiscovery.getContainerAddress();
        if(address.isLeft())
        {
            System.err.println("Unable to get container address, so exiting -"+ address.left().get());
            System.exit(1);
        }
        return  address.right().get();
    }


    private  static  boolean isAWS(String environment){
        return "AWS".equals(environment);
    }


}

