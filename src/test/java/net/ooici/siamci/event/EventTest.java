package net.ooici.siamci.event;

import static com.mycila.event.Topic.topic;
import static com.mycila.event.Topics.only;

import org.testng.Assert;
import org.testng.annotations.Test;

import siamcitest.BaseTestCase;

import com.mycila.event.Dispatcher;
import com.mycila.event.Dispatchers;
import com.mycila.event.Event;
import com.mycila.event.Subscriber;

/**
 * Basic tests of the event handling mechanism. The tests using {@link EventMan}
 * assume that the "synchronous safe" dispatching is in place.
 * 
 * See <a href=
 * "http://code.google.com/p/mycila/wiki/MycilaEvent#Event_dispatching_strategies"
 * > Event_dispatching_strategies</a>
 * 
 * @author carueda
 */
public class EventTest extends BaseTestCase {

    static class MyEvent {
        final String str;

        MyEvent(String str) {
            this.str = str;
        }

        public String toString() {
            return str;
        }
    }

    /**
     * Using @ link EventMan} .
     */
    @Test
    public void testUsingEventMan() {

        // subscribe
        final MyEvent[] received = new MyEvent[1];
        EventMan.subscribe(MyEvent.class, new Subscriber<MyEvent>() {
            public void onEvent(Event<MyEvent> event) throws Exception {
                MyEvent myEvent = event.getSource();
                received[0] = myEvent;
            }
        });

        // fire event
        String str = String.valueOf(Math.random());
        EventMan.fireEvent(new MyEvent(str));

        /*
         * NOTE that we assert the received event right away because EventMan
         * uses Dispatchers.synchronousSafe(). With other dispatcher strategy
         * there may be a need to wait for a bit or something.
         */

        Assert.assertNotNull(received[0]);
        Assert.assertEquals(str, received[0].str);
    }

    /**
     * Direct use of mycila-event API, using "synchronous safe" dispatch
     * strategy.
     */
    @Test
    public void testDirectMycilaEvent() {
        Dispatcher dispatcher = Dispatchers.synchronousSafe();

        final MyEvent[] received = new MyEvent[1];

        // subscribe
        String name = MyEvent.class.getName();
        dispatcher.subscribe(only(name),
                MyEvent.class,
                new Subscriber<MyEvent>() {
                    public void onEvent(Event<MyEvent> event) throws Exception {
                        MyEvent myEvent = event.getSource();
                        received[0] = myEvent;
                    }
                });

        // fire event
        String str = String.valueOf(Math.random());
        dispatcher.publish(topic(name), new MyEvent(str));

        //
        // NOTE that we assert the received event right away because we are
        // using the Dispatchers.synchronousSafe() strategy
        //

        Assert.assertNotNull(received[0]);
        Assert.assertEquals(str, received[0].str);
    }

}
