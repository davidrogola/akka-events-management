# Akka Cluster On AWS ECS with EC2 Launch Type
  This a sample project that implements akka cluster, akka http, cluster sharding and akka management cluster bootstrap using 
  discovery method AWS API-ECS discovery.
  
  Cluster sharding is useful when you need to distribute actors across several nodes in the cluster and want to be able 
  to interact with them using their logical identifier, but without having to care about their physical location in the cluster, 
  which might also change over time. Refer to this for more details [Cluster Sharding](https://doc.akka.io/docs/akka/current/cluster-sharding.html)
  
  For the sample project its an event booking project with events represented as akka actors. 
  The event actor receives listens to three message types: AddEventCommnad, GetEvent, BookEvent. When a message is sent to an Event actor
  the ShardCoordinator ensures that the message is routed to the correct actor within the cluster.
  
  The AWS ECS discovery method is the bootstrapping method used to form the cluster on ECS. Refer to the application.conf file for 
  the configuration details. You are required to specify the aws cluster name that your application is running on and the service name for
  which the task instances are running on. This is to ensure that the akka cluster is formed successfully when the running task instances are discovered.
  Please refer to this [AWS Discovery API](https://developer.lightbend.com/docs/akka-management/current/discovery/aws.html) for more details.
  
  ## Running the project locally
   Change the main class configuration in the *pom.xml* file from *StartUp* to point the *LocalClusterStartUp* class for running the cluster locally.
   This example uses 2 loopback addressess localy to test the cluster formation a single machine. Refer here [Loopback Address](https://www.techopedia.com/definition/2440/loopback-address)
   To enable loopback address on Mac run the following command on the terminal before starting the running the project:
   
      sudo ifconfig lo0 alias 127.0.0.2 up
      sudo ifconfig lo0 alias 127.0.0.3 up
  
  On Linux the command is not required.
  The configuration used for the local cluster formation is as shown below. Please refer to [this](https://developer.lightbend.com/docs/akka-management/current/bootstrap/local-config.html) for more details.
     
       akka.discovery {
          config.services = {
            events-management-system = {
            endpoints = [
            {
              host = "127.0.0.1"
              port = 8558
            },
            {
              host = "127.0.0.2"
              port = 8558
            },
            {
              host = "127.0.0.3"
              port = 8558
            }
          ]
       }
     }
    }
    #bootstrap
      akka.management {
        cluster.bootstrap {
          contact-point-discovery {
            service-name = "events-management-system"
            discovery-method = config
          }
        }
      }
      
  You can view the full configuration file in the solution project folder resources(local.conf)
  To run the project you can either right click on the *LocalClusterStartUp* class in the local package or run the following command
  in terminal: 
     
     mvn exec:java -Dexec.mainClass="local.LocalClusterStartUp"
  
  You should be able to see on the terminal the nodes joining the cluster and their statuses being changed to up and the node with the lowest address being selected as 
  the leader as shown in the following logs:
  
        [INFO] [05/09/2019 11:20:29.429] [events-management-service-akka.actor.default-dispatcher-15] [akka.cluster.Cluster(akka://events-management-service)] Cluster Node [akka.tcp://events-management-service@127.0.0.3:2551] - Received InitJoinAck message from [Actor[akka.tcp://events-management-service@127.0.0.1:2551/system/cluster/core/daemon#-869905226]] to [akka.tcp://events-management-service@127.0.0.3:2551]
        [INFO] [05/09/2019 11:20:29.466] [events-management-service-akka.actor.default-dispatcher-2] [akka.cluster.Cluster(akka://events-management-service)] Cluster Node [akka.tcp://events-management-service@127.0.0.1:2551] - Node [akka.tcp://events-management-service@127.0.0.3:2551] is JOINING, roles [dc-default]
        [INFO] [05/09/2019 11:20:29.467] [events-management-service-akka.actor.default-dispatcher-20] [akka.cluster.Cluster(akka://events-management-service)] Cluster Node [akka.tcp://events-management-service@127.0.0.2:2551] - Received InitJoinAck message from [Actor[akka.tcp://events-management-service@127.0.0.1:2551/system/cluster/core/daemon#-869905226]] to [akka.tcp://events-management-service@127.0.0.2:2551]
        [INFO] [05/09/2019 11:20:29.470] [events-management-service-akka.actor.default-dispatcher-15] [akka.cluster.Cluster(akka://events-management-service)] Cluster Node [akka.tcp://events-management-service@127.0.0.1:2551] - Node [akka.tcp://events-management-service@127.0.0.2:2551] is JOINING, roles [dc-default]
        [INFO] [05/09/2019 11:20:29.616] [events-management-service-akka.actor.default-dispatcher-2] [akka.cluster.Cluster(akka://events-management-service)] Cluster Node [akka.tcp://events-management-service@127.0.0.3:2551] - Welcome from [akka.tcp://events-management-service@127.0.0.1:2551]
        [INFO] [05/09/2019 11:20:29.616] [events-management-service-akka.actor.default-dispatcher-2] [akka.cluster.Cluster(akka://events-management-service)] Cluster Node [akka.tcp://events-management-service@127.0.0.2:2551] - Welcome from [akka.tcp://events-management-service@127.0.0.1:2551]
        [INFO] [05/09/2019 11:20:29.787] [events-management-service-akka.actor.default-dispatcher-4] [akka.cluster.Cluster(akka://events-management-service)] Cluster Node [akka.tcp://events-management-service@127.0.0.1:2551] - Leader is moving node [akka.tcp://events-management-service@127.0.0.2:2551] to [Up]
        [INFO] [05/09/2019 11:20:29.787] [events-management-service-akka.actor.default-dispatcher-4] [akka.cluster.Cluster(akka://events-management-service)] Cluster Node [akka.tcp://events-management-service@127.0.0.1:2551] - Leader is moving node [akka.tcp://events-management-service@127.0.0.3:2551] to [Up]
        [INFO] [05/09/2019 11:20:30.284] [events-management-service-akka.actor.default-dispatcher-19] [akka://events-management-service/system/sharding/Events] Server online at http://127.0.0.3:8080/
        [INFO] [05/09/2019 11:20:30.288] [events-management-service-akka.actor.default-dispatcher-33] [akka.tcp://events-management-service@127.0.0.3:2551/system/sharding/EventsCoordinator] ClusterSingletonManager state change [Start -> Younger]
        [INFO] [05/09/2019 11:20:30.805] [events-management-service-akka.actor.default-dispatcher-14] [akka.tcp://events-management-service@127.0.0.2:2551/system/sharding/EventsCoordinator] ClusterSingletonManager state change [Start -> Younger]
        [INFO] [05/09/2019 11:20:30.807] [events-management-service-akka.actor.default-dispatcher-15] [akka://events-management-service/system/sharding/Events] Server online at http://127.0.0.2:8080/
 
 Send a get request to any of the running nodes to see the cluster members present in the newly formed cluster. 
 i.e *http://127.0.0.1:8080/cluster/members* should return the following response:
       
           "members": [
            {
                "node": "akka.tcp://events-management-service@127.0.0.1:2551",
                "nodeUid": "76146868",
                "roles": [
                    "dc-default"
                ],
                "status": "Up"
            },
            {
                "node": "akka.tcp://events-management-service@127.0.0.2:2551",
                "nodeUid": "-1938368855",
                "roles": [
                    "dc-default"
                ],
                "status": "Up"
            },
            {
                "node": "akka.tcp://events-management-service@127.0.0.3:2551",
                "nodeUid": "465237686",
                "roles": [
                    "dc-default"
                ],
                "status": "Up"
            }
        ],
        "oldest": "akka.tcp://events-management-service@127.0.0.1:2551",
        "oldestPerRole": {
            "dc-default": "akka.tcp://events-management-service@127.0.0.1:2551"
        },
        "selfNode": "akka.tcp://events-management-service@127.0.0.1:2551",
        "unreachable": []
      }
     
 The result should show all the member nodes present in the local cluster with the UP status meaning all nodes in the cluster are reachable.
 
 ## Running the project on AWS ECS
 
  Ensure that the main class configuration in the *pom.xml* file points to *StartUp* as the entry point. Package the project using 
  the following command to generate a docker image.
    
     mvn clean package dockerfile:build
     
   Push the image to a docker repository of your choice. The image will be pulled when defining the AWS task definition that should 
   run in the AWS ECS cluster.
   The configuration file for this set up is as follows: 
       
       akka {
             actor {
                provider = "cluster"
                warn-about-java-serializer-usage = false
              }

              cluster {
                   seed-nodes = []
                   shutdown-after-unsuccessful-join-seed-nodes = 30s
                   downing-provider-class = com.ajjpj.simpleakkadowning.SimpleAkkaDowningProvider
                }
                #coorindated-shutdown
                coordinated-shutdown.exit-jvm = on

              remote {
                log-remote-lifecycle-events = off

                enabled-transports = ["akka.remote.netty.tcp"]
                netty.tcp {
                  hostname = ${?HOST}
                  port = 2552
                }
              }
                management {
                  cluster.bootstrap {
                   #set to on for the initial cluster depolyment and off
                   new-cluster-enabled = on
                    contact-point-discovery {
                      # pick the discovery method you'd like to use:
                       discovery-method = aws-api-ecs-async
                       #name of the aws service your task instances are running in
                       service-name = akka-events-service
                      #Wait until there are 2 contact points present before attempting initial cluster formation
                      required-contact-point-nr = 2
                    }

                  contact-point {
                    # If no port is discovered along with the host/ip of a contact point this port will be used as fallback
                    fallback-port = 8558
                  }
                  http {
                      port = 8558
                      hostname = ${?HOST}

                  }
                  #health
                  health-checks {
                    readiness-path = "health/ready"
                    liveness-path = "health/alive"
                  }
                  #health
                }
              }

              discovery {
                  method =aws-api-ecs-async
                  aws-api-ecs-async {
                        #AWS cluster name will be default if not provided
                        cluster = "events-cluster" . 
                      }
              }
          }
      
  The cluster discovery method being used is *aws-api-ecs-async* provided by akka management. The bootstrap method will automatically 
  discover the running instances in the same aws cluster specfied above and initiate the process of akka cluster formation.
  For this bootstrapping method to work with AWS ECS with EC2 launch type the following configurations should be done:
       
       - The following IAM roles should be assigned to the Task Execution role associated with the AWS task defination:
       
          1. AmazonEC2ContainerServiceforEC2Role
          2. AmazonECSFullAccess (Or a role that has permissions to ListTasks and DescribeTasks)
          3. AmazonECSTaskExecutionRolePolicy(This is automatically created by AWS if not specified)
          
       - The awsvpc network mode does not provide task elastic network interfaces with public IP addresses for tasks that use the EC2 launch type.
         Hence the tasks have no outbound network access and this is required for the bootstrap process to succuessfully run and form a cluster.
         To access the internet, tasks that use the EC2 launch type must be launched in a private subnet that is configured to use a NAT gateway.
        
   Please refer to [AWS Task Networking](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-networking.html) and [Task Roles](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html) for more details.
   On succesful set up you should be able to see the cluster forming and view the cluster members using the endpoint *cluster/members*.
  
