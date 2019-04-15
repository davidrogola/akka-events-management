package messages.query;

import java.io.Serializable;

public class GetEvent implements Serializable {

    public GetEvent(int eventId){
        EventId = eventId;
    }

    public int EventId;
}


