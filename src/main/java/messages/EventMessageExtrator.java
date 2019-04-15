package messages;

import actors.Event;
import akka.cluster.sharding.ShardRegion;
import messages.query.GetEvent;

 public  class EventMessageExtrator {
    public static final ShardRegion.MessageExtractor eventMessageExtractor = new ShardRegion.MessageExtractor() {
        @Override
        public String shardId(Object message) {
            int numberOfShards = 100;
            if(message instanceof Event.AddNewEventCommand)
                return String.valueOf(((Event.AddNewEventCommand) message).Id % numberOfShards);

            if(message instanceof Event.BookEvent)
                return  String.valueOf(((Event.BookEvent) message).EventId % numberOfShards);

            if (message instanceof GetEvent)
                return String.valueOf(((GetEvent) message).EventId % numberOfShards);

            return null;
        }

        @Override
        public String entityId(Object message) {
            if(message instanceof Event.AddNewEventCommand)
                return String.valueOf((((Event.AddNewEventCommand) message).Id));
            if (message instanceof Event.BookEvent)
                return String.valueOf((((Event.BookEvent) message).EventId));
            else if (message instanceof GetEvent)
                return String.valueOf(((GetEvent) message).EventId);
            else  return null;
        }

        @Override
        public Object entityMessage(Object message) {
            if(message instanceof Event.AddNewEventCommand)
                return message;
            else if(message instanceof Event.BookEvent)
                return message;
            else if(message instanceof GetEvent)
                return message;
            else return message;
        }
    };
}
