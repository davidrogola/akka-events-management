
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
import helpers.AkkaClusterHelper;
import helpers.AkkaClusterManagement;
import helpers.AwsConfigHelper;
import messages.EventMessageExtrator;
import routes.EventRouter;
import java.net.InetAddress;

public class StartUp {


    public static void main(String[] args) {
        try {
            Config configBase = ConfigFactory.load("application.conf");
            String actorSystemName = configBase.getString("app.cluster.name");
            String environment = configBase.getString("app.aws.envType");
            AwsConfigHelper awsConfig = new AwsConfigHelper(configBase);

            if(AkkaClusterHelper.isAWS(environment)) {
                InetAddress privateAddress =awsConfig.getContainerAddress();
                String dockerAddress = privateAddress.getHostAddress();
                System.out.println("dockerAddress: " + dockerAddress);
            }

            ActorSystem actorSystem = ActorSystem.create(actorSystemName,configBase);

            AkkaManagement.get(actorSystem).start();
            ClusterBootstrap.get(actorSystem).start();

            final Config finalConfigBase = configBase;
            Runnable runnable = ()->AkkaClusterHelper.initializeClusterSharding(actorSystem, finalConfigBase);
            Cluster.get(actorSystem).registerOnMemberUp(runnable);

            System.out.println("Cluster Bootstrap Started successfully");

        } catch (Exception e)
        {
           System.out.println("An exception has been thrown :"+ e.getMessage());
        }
    }



}

