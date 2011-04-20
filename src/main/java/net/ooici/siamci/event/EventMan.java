package net.ooici.siamci.event;

import static com.mycila.event.Topic.topic;
import static com.mycila.event.Topics.only;

import com.mycila.event.Dispatcher;
import com.mycila.event.Dispatchers;
import com.mycila.event.Subscriber;

/**
 * This is our high-level, simple event manager. It is preliminary but enough
 * for the purposes at hand with the SIAM-CI protoype. It is based on <a
 * href="http://code.google.com/p/mycila/wiki/MycilaEvent">MycilaEvent</a>,
 * which is easy to use and works fine. Although the underlying library is not
 * completely hidden (in particular, {@link Subscriber} is exposed), the idea
 * with this manager is to provide a simple API for handling events while
 * facilitating an eventual transition to a different underlying libray should
 * it became necessary.
 * 
 * @see <a
 *      href="http://code.google.com/p/mycila/wiki/MycilaEvent">http://code.google.com/p/mycila/wiki/MycilaEvent</a>
 * @see <a
 *      href="http://java.dzone.com/articles/event-driven-development">http://java.dzone.com/articles/event-driven-development</a>
 * 
 * @author carueda
 */
public class EventMan {

    private static final Dispatcher dispatcher = Dispatchers.synchronousSafe();

    /*
     * Note, we use the event's class name as the topic. This means there is a
     * 1-to-1 relationship between event classes and mycila-event topics in our
     * use of that library; this is just enough for the purposes at hand in the
     * SIAM-CI protoype.
     */

    /**
     * Registers a subscriber to events of the given type.
     * 
     * @param <E>
     * @param eventType
     * @param subscriber
     */
    public static <E> void subscribe(Class<?> eventType,
            Subscriber<E> subscriber) {
        String name = eventType.getName();
        dispatcher.subscribe(only(name), eventType, subscriber);

    }

    /**
     * Fires the given event.
     * 
     * @param event
     *            the event. This event's class name is used to identify the
     *            registered subscribers that should be notified.
     * @throws IllegalArgumentException
     *             if the event is null
     */
    public static void fireEvent(Object event) {
        if (event == null) {
            throw new IllegalArgumentException("event canno be null");
        }
        String name = event.getClass().getName();
        dispatcher.publish(topic(name), event);
    }

    private EventMan() {
    }
}
