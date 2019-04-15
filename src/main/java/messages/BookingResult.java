package messages;

import java.io.Serializable;

public class BookingResult  implements Serializable {

    public  BookingResult(String response, int noOfTicketsBooked, boolean successful,String actorAddress){
        Response = response;
        NoOfTicketsBooked = noOfTicketsBooked;
        Successful = successful;
        ActorAddress = actorAddress;
    }
    private static final long serialVersionUID = 1L;
    private String Response;

    private int NoOfTicketsBooked ;

    private boolean Successful ;

    private String ActorAddress;

    public String getMessage (){
        return Successful ?  String.format("Booking succeed with %d tickets acquired",NoOfTicketsBooked) :
                             Response;
    }

}
