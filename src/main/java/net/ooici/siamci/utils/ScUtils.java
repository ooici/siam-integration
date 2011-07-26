package net.ooici.siamci.utils;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.ooici.play.InstrDriverInterface.Command;
import net.ooici.play.InstrDriverInterface.Result;
import net.ooici.play.InstrDriverInterface.StringPair;
import net.ooici.play.InstrDriverInterface.SuccessFail;
import net.ooici.play.InstrDriverInterface.SuccessFail.Builder;
import net.ooici.play.InstrDriverInterface.SuccessFail.Item;

import com.google.protobuf.GeneratedMessage;

/**
 * Misc utilities.
 * 
 * @author carueda
 */
public class ScUtils {

    /**
     * Formats a request id: helps identify the specific request among the
     * various possible concurrent log messages.
     */
    public static String formatReqId(int reqId) {
        return "[" + reqId + "] ";
    }

    /**
     * Gets the value of the "publish_stream" field, if any.
     * 
     * @param cmd
     *            The command to examine.
     * 
     * @return the value of the "publish_stream" field; null if such argument is
     *         missing.
     */
    public static String getPublishStreamName(Command cmd) {
        return cmd.hasPublishStream() ? cmd.getPublishStream() : null;
    }

    /**
     * Creates an error response.
     * 
     * @param description
     *            Fail description, required.
     * @return A {@link SuccessFail} message for the error.
     */
    public static GeneratedMessage createFailResponse(String description) {
        SuccessFail sf = SuccessFail.newBuilder()
                .setResult(Result.ERROR)
                .addItem(Item.newBuilder()
                        .setType(Item.Type.STR)
                        .setStr(description)
                        .build())
                .build();
        return sf;
    }

    /**
     * Creates a simple success (OK) response with an optional string item.
     * 
     * @param string
     *            An string argument, included if not null.
     * @return A {@link SuccessFail} message for the success.
     */
    public static GeneratedMessage createSuccessResponse(String string) {
        Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);
        if (string != null) {
            buildr.addItem(Item.newBuilder()
                    .setType(Item.Type.STR)
                    .setStr(string));
        }
        SuccessFail response = buildr.build();
        return response;
    }

    /**
     * Creates a success (OK) response with a dictionary for the items.
     * 
     * @param map
     *            The pairs to be included in the items
     * @return A {@link SuccessFail} message for the success.
     * @throw IllegalArgumentException is argument is null
     */
    public static GeneratedMessage createSuccessResponseWithMap(Map<String, String> map) {
        if (map == null) {
            throw new IllegalArgumentException("argument cannot be null");
        }

        Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);
        for (Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            buildr.addItem(Item.newBuilder()
                    .setType(Item.Type.PAIR)
                    .setPair(StringPair.newBuilder()
                            .setFirst(key)
                            .setSecond(val)));
        }
        SuccessFail response = buildr.build();
        return response;
    }
}
