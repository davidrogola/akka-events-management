package actors;
import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import messages.BookingResult;
import messages.commands.EventAddedResponse;

public class Booking  extends AbstractLoggingActor {

    public  Booking(){

    }

    public  static Props props(){
        return  Props.create(Booking.class);
    }

    @Override
    public Receive createReceive() {
        return  receiveBuilder().match(BookingResult.class, booking-> {
                log().info(booking.getMessage());
             }).match(EventAddedResponse.class,event->{
              log().info(event.Message);
        }).build();
    }
}
