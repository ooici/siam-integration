package net.ooici.siamci.event;

import static com.mycila.event.Topic.topic;
import static com.mycila.event.Topics.only;

import com.mycila.event.Dispatcher;
import com.mycila.event.Dispatchers;
import com.mycila.event.Subscriber;

/**
 * This is our high-level, simple event manager. It is preliminary but enough
 * for the purposes at hand with the SIAM-CI protoype. Internally it uses <a
 * href="http://code.google.com/p/mycila/wiki/MycilaEvent">MycilaEvent</a>. A
 * later version could expose the MycilaEvent API or use some other in-jvm event
 * bus library.
 * 
 * @author carueda
 */
public class EventMan {

    private static final Dispatcher dispatcher = Dispatchers.synchronousSafe();

    public static <E> void subscribe(Class<?> eventType, Subscriber<E> subscriber) {
        String name = eventType.getName();
        dispatcher.subscribe(only(name), eventType, subscriber);

    }

    /**
     * Fires the given event.
     * 
     * @param event
     *            the event
     */
    public static void fireEvent(Object event) {
        /*
         * Note, we use the event's class name as the topic. This means there is
         * a 1-to-1 relationship between event classes and topics, which is just
         * enough for the purposes at hand in the SIAM-CI protoype.
         */
        String name = event.getClass().getName();
        dispatcher.publish(topic(name), event);
    }

    private EventMan() {
    }
}
