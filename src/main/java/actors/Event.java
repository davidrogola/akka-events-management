package actors;
import akka.actor.*;
import akka.cluster.sharding.ShardRegion;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import messages.BookingResult;
import messages.commands.EventAddedResponse;
import messages.query.GetEvent;
import org.javatuples.Pair;

import java.io.Serializable;
import java.time.Duration;

public class Event extends AbstractActor {

    private final Address actorSystemAddress = getContext().system().provider().getDefaultAddress();
    private LoggingAdapter log = Logging.getLogger(getContext().system(), getSelf().path().toStringWithAddress(actorSystemAddress));

    private EventInfo eventInfo = null;

    public  Event() {

    }



    public static class EventInfo implements Serializable {

        public  EventInfo()
        {

        }

        public EventInfo(int id,String name, String code, int totalTickets,String actorAddress)
        {
            Id = id;
            EventName = name;
            Code = code;
            TotalTickets = totalTickets;
            AvailableTickets = totalTickets;
            TicketsBought = 0;
            ActorAddress = actorAddress;

        }
        @JsonProperty("id")
        private int Id;
        private  String EventName ;
        private  String Code;

        @JsonProperty("availableTickets")
        private int AvailableTickets;

        @JsonProperty("ticketsBought")
        private int TicketsBought;

        @JsonProperty("totalTickets")
        private int TotalTickets;

        @JsonProperty("actorAddress")
        private String ActorAddress;

        public String getEventName(){
            return EventName;
        }

        public  Pair<Boolean,String> bookTicket(int numberOfTickets){
            if(AvailableTickets == 0)
                return new Pair<>(false,"Event is fully booked");
            if(numberOfTickets > TotalTickets)
                return new Pair<>(false,"Number of tickets requested is more than the total tickets for event");
            if(numberOfTickets > AvailableTickets)
                return new Pair<>(false,"Number of tickets exceeds the currently available number");


            AvailableTickets -= numberOfTickets;
            TicketsBought += numberOfTickets;

            return new Pair<>(true,"Tickets for event booked succefully");

        }
    }


    public  static class  AddNewEventCommand implements Serializable {
        private static final long serialVersionUID = 1L;
        public  int Id;
        public String EventName;
        public int TotalTickets;
        public String EventCode;

        @JsonCreator
        public  AddNewEventCommand(@JsonProperty("id") int id, @JsonProperty("eventName") String eventName,
                                   @JsonProperty("eventCode") String eventCode,
                                   @JsonProperty("totalTickets") int totalTickets)
        {
            Id = id;
            EventName = eventName;
            TotalTickets = totalTickets;
            EventCode = eventCode;
        }
    }


    public static  class BookEvent implements Serializable {
        @JsonCreator()
        public  BookEvent(@JsonProperty("eventId") int eventId, @JsonProperty("name") String name,
                          @JsonProperty("numberOfTickets") int noOfTickets){
            EventId = eventId;
            Name = name;
            NumberOfTickets = noOfTickets;
        }
        private static final long serialVersionUID = 1L;
        public int EventId ;
        public String Name ;
        public int NumberOfTickets;

        public int getNumberOfTickets() {
            return NumberOfTickets;
        }

        public String getName() {
            return Name;
        }
    }





    public  static Props props(){
        return  Props.create(Event.class);
    }

    @Override
    public  void preStart() throws Exception{
        super.preStart();
        getContext().setReceiveTimeout(Duration.ofSeconds(120));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(BookEvent.class,  this::bookEvent)
                .match(GetEvent.class,this::getEvent)
                .match(AddNewEventCommand.class,this::addNewEvent)
                .matchEquals(ReceiveTimeout.getInstance(),msg-> passivate())
                .matchAny(a-> log.info("Message of unknown format received {}",a)).build();
    }

    private void bookEvent(BookEvent event){
        if(eventInfo == null)
        {
            getSender().tell(new BookingResult(String.format("Event %s with id %d does not exist",event.getName(),event.EventId),
                    0,false,null),ActorRef.noSender());
            return;
        }
        Pair<Boolean, String> response = eventInfo.bookTicket(event.getNumberOfTickets());
        getSender().tell( new BookingResult(response.getValue1(), event.getNumberOfTickets(), response.getValue0(),getActorInfo()),getSelf());
    }

    private void getEvent(GetEvent query){
         log.info("Received get event message " + query.EventId);
          if(eventInfo == null)
          {
              getSender().tell(new EventInfo(),getSelf());
               return;
          }
          getSender().tell(eventInfo,getSelf());

    }

    private void addNewEvent(AddNewEventCommand command){
         log.info("Received Add event command with id {}",command.Id);
         if(eventInfo == null){
             eventInfo = new EventInfo(command.Id,command.EventName,command.EventCode,command.TotalTickets,getActorInfo());
             log.info(eventInfo.getEventName() + "Created this actor");
             getSender().tell(new EventAddedResponse(command.EventName,"Event added successfully",getActorInfo()),getSelf());
             return;
         }
        getSender().tell(new EventAddedResponse(command.EventName,"Event already exists",getActorInfo()),getSelf());
    }

    private  void passivate(){
        //Tell ShardRegion to shut us down so as to save resources
        getContext().getParent().tell(new ShardRegion.Passivate(PoisonPill.getInstance()),getSelf());
    }

    private String getActorInfo(){
        return getSelf().path().toStringWithAddress(actorSystemAddress);
    }

}
