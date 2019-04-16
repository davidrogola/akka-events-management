
import actors.Event;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.Http;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.ServerBinding;
import akka.stream.javadsl.Flow;
import  akka.NotUsed;
import akka.http.javadsl.model.*;
import akka.stream.ActorMaterializer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import messages.EventMessageExtrator;
import routes.EventRouter;

import java.util.concurrent.CompletionStage;

public class StartUp {


    public static void main(String[] args) {
        Config configBase = ConfigFactory.load();

        String actorSystemName = "EventsManagementService";
        if(args.length == 0)
        {
            //initializeCluster(new String[]{"2551", "8080", "2552", "8081"},actorSystemName);
            initializeActorSystem(actorSystemName,configBase);
            return;
        }

        if (args.length % 2 == 1) {
            System.out.println("Pass even number of ports! One remoting and one HTTP port for each actor system.");
            System.exit(1);
        }

        initializeCluster(args,actorSystemName);

    }

    private  static void initializeCluster(String [] ports,String actorSystem)
    {
        for (int index = 0; index < ports.length; index +=2){
             String remotingPort = ports[index];
             String httpServerPort  = ports[index + 1];
             initializeActorSystem(actorSystem,setupClusterNodeConfig(remotingPort,httpServerPort));
        }
    }

    private static void initializeActorSystem(String actorName, Config config)
    {
        ActorSystem actorSystem = ActorSystem.create(actorName,config);

        ClusterShardingSettings settings = ClusterShardingSettings.create(actorSystem);
        ActorRef eventsRegion = ClusterSharding.get(actorSystem).start("Events",Event.props(),
                settings, EventMessageExtrator.eventMessageExtractor);

        Http http = Http.get(actorSystem);
        ActorMaterializer materializer = ActorMaterializer.create(actorSystem);

        final LoggingAdapter log = Logging.getLogger(actorSystem,eventsRegion);

        EventRouter routes = new EventRouter(log);

        String hostName = config.getString("sample.http.hostname");
        int port = config.getInt("sample.http.port");
        Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = routes.createRoutes(actorSystem)
                .flow(actorSystem,materializer);
        CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow, ConnectHttp.toHost(hostName,port),materializer);
        log.info("Server online at http://{}:{}/", hostName, port);
    }

    private static Config setupClusterNodeConfig(String remoting,String http)
    {
        return ConfigFactory.parseString(
                String.format("akka.remote.netty.tcp.port=%s%n", remoting) +
                        String.format("sample.http.port=%s%n", http))
                .withFallback(ConfigFactory.load());
    }


}

