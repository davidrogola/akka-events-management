package helpers;

import actors.Event;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import com.typesafe.config.Config;
import messages.EventMessageExtrator;
import routes.EventRouter;

public class AkkaClusterHelper {

    public static void initializeClusterSharding(ActorSystem actorSystem, Config config)
    {
        ClusterShardingSettings settings = ClusterShardingSettings.create(actorSystem);

        ActorRef eventsRegion = ClusterSharding.get(actorSystem).start("Events", Event.props(),
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



    public  static  boolean isAWS(String environment){
        return "AWS".equals(environment);
    }
}
