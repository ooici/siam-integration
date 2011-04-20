package net.ooici.siamci.event;

/**
 * Indicates that a message has failed to be delivered/routed.
 * 
 * @author carueda
 */
public class ReturnEvent {

    private final String routingKey;

    public ReturnEvent(String routingKey) {
        this.routingKey = routingKey;

    }

    public String getRountingKey() {
        return routingKey;
    }

}
