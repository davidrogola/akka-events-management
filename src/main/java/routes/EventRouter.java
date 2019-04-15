package routes;
import actors.Event;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.sharding.ClusterSharding;
import akka.event.LoggingAdapter;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import akka.pattern.Patterns;
import akka.util.Timeout;
import messages.BookingResult;
import messages.commands.EventAddedResponse;
import messages.query.GetEvent;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

public class EventRouter extends AllDirectives {

    final private LoggingAdapter log;

    public EventRouter(LoggingAdapter log) {
       this.log = log;
    }

    Timeout timeout = new Timeout(Duration.create(5,TimeUnit.SECONDS)); // required for the actorRef.ask method
    FiniteDuration duration = Duration.create(2,TimeUnit.SECONDS);  // future wait duration

    public Route createRoutes(ActorSystem actorSystem) {

       ActorRef shardRegion = ClusterSharding.get(actorSystem).shardRegion("Events");

        return route(pathPrefix("events", () ->
                 route(path(PathMatchers.segment("add"),()-> route(addNewEvent(shardRegion))),
                 path(PathMatchers.segment("get").slash(PathMatchers.integerSegment()), eventId->route(getEvent(shardRegion,eventId))),
                 path(PathMatchers.segment("book"),()->route(bookTicket(shardRegion))))));
    }


    private Route addNewEvent(ActorRef shardRegion) {
        return post(() -> {
            return entity(
                    Jackson.unmarshaller(Event.AddNewEventCommand.class), event -> {

                        try {

                            Future<Object> response = Patterns.ask(shardRegion, event, timeout);

                            EventAddedResponse eventAddedResponse = (EventAddedResponse) Await.result(response, duration);
                            return complete(StatusCodes.OK, eventAddedResponse, Jackson.marshaller());

                        } catch (Exception e) {
                            log.error("An error {} occured while adding new event info with id {}", e.getMessage(), event.Id);
                            return complete(StatusCodes.INTERNAL_SERVER_ERROR);
                        }
                    }
            );
        });
    }

    private Route getEvent(ActorRef shardRegion, int event){
        return get(()->{
            try {

                Future<Object> eventInfoFuture =  Patterns.ask(shardRegion,new GetEvent(event),timeout);

                Event.EventInfo eventInfo =(Event.EventInfo) Await.result(eventInfoFuture,duration);

                if(eventInfo == null)
                    return complete(StatusCodes.NOT_FOUND);

                return complete(StatusCodes.OK,eventInfo,Jackson.marshaller());

            } catch (Exception e) {
                 log.error("An error {} occurred while getting event with id {}",e.getMessage(),event);
                 return complete(StatusCodes.INTERNAL_SERVER_ERROR);
            }

        });
    }


    private Route bookTicket(ActorRef shardRegion){
        return post(()-> entity(
                Jackson.unmarshaller(Event.BookEvent.class), event->{
                    try {
                        Future<Object> responseFuture = Patterns.ask(shardRegion, event, timeout);
                        BookingResult bookingResult = (BookingResult) Await.result(responseFuture,duration);
                        return  complete(StatusCodes.OK,bookingResult,Jackson.marshaller());
                    }
                    catch (Exception e){
                        log.error("An error {} occurred while booking tickets for event {} ",e.getMessage(),event.getName());
                        return complete(StatusCodes.INTERNAL_SERVER_ERROR);
                    }

                }
        ));
    }



}