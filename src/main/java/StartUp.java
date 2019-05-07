
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
import helpers.AkkaClusterManagement;
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
            AwsConfigHelper awsConfig = new AwsConfigHelper(configBase);

            if(isAWS(environment)) {
                InetAddress privateAddress =awsConfig.getContainerAddress();
                String dockerAddress = privateAddress.getHostAddress();
                System.out.println("dockerAddress: " + dockerAddress);
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
        AwsConfigHelper awsConfigHelper = new AwsConfigHelper(config);

        String hostName = isAWS(config.getString("app.aws.envType")) ?
                                 awsConfigHelper.getContainerAddress().getHostAddress():
                                 config.getString("akka.management.http.hostname");

        int port = config.getInt("app.webapi.http.port");
        EventRouter routes = new EventRouter(log,new AkkaClusterManagement(awsConfigHelper,hostName));

        Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = routes.createRoutes(actorSystem)
                .flow(actorSystem,materializer);

        http.bindAndHandle(routeFlow, ConnectHttp.toHost(hostName,port),materializer);
        log.info("Server online at http://{}:{}/", hostName, port);
    }



    private  static  boolean isAWS(String environment){
        return "AWS".equals(environment);
    }


}

