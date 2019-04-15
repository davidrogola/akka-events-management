package messages.commands;

import java.io.Serializable;

public class EventAddedResponse  implements  Serializable {
    private static final long serialVersionUID = 1L;
        public  EventAddedResponse(String eventName, String message, String actorAddress){
            EventName = eventName;
            Message = message;
            ActorAddress = actorAddress;
        }
        public  String EventName ;
        public  String Message ;
        public  String ActorAddress;
    }

