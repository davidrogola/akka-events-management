package local;

import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import helpers.AkkaClusterHelper;

public class LocalClusterStartUp {

    public static void main(String [] args){

        startUpNode(1);
        startUpNode(2);
        startUpNode(3);
    }

    private static void startUpNode(int nodeNumber){

        Config localConfigBase = ConfigFactory.parseString(
                String.format("akka.remote.netty.tcp.hostname=127.0.0.%s%n", nodeNumber) +
                        String.format("akka.management.http.hostname=127.0.0.%s%n", nodeNumber))
                .withFallback(ConfigFactory.load("local.conf"));

        String actorSystemName = localConfigBase.getString(" actorsystem.name");

        ActorSystem actorSystem = ActorSystem.create(actorSystemName,localConfigBase);
        AkkaManagement.get(actorSystem).start();
        ClusterBootstrap.get(actorSystem).start();

        Runnable runnable = ()-> AkkaClusterHelper.initializeClusterSharding(actorSystem,localConfigBase);
        Cluster.get(actorSystem).registerOnMemberUp(runnable);

    }
}
