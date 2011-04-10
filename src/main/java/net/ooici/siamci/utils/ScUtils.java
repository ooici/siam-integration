package net.ooici.siamci.utils;

import com.google.protobuf.GeneratedMessage;

import net.ooici.play.InstrDriverInterface.ChannelParameterPair;
import net.ooici.play.InstrDriverInterface.Command;
import net.ooici.play.InstrDriverInterface.Result;
import net.ooici.play.InstrDriverInterface.SuccessFail;
import net.ooici.play.InstrDriverInterface.SuccessFail.Builder;
import net.ooici.play.InstrDriverInterface.SuccessFail.Item;

/**
 * Misc utilities.
 * 
 * @author carueda
 */
public class ScUtils {

	/**
	 * Gets the value of the "publish_stream" argument, if any.
	 * 
	 * @param cmd
	 *            The command to examine.
	 * 
	 * @return the value of the "publish_stream" argument; null if such argument
	 *         is missing.
	 */
	public static String getPublishStreamName(Command cmd) {
		for (int i = 0, count = cmd.getArgsCount(); i < count; i++) {
			ChannelParameterPair cp = cmd.getArgs(i);
			if ("publish_stream".equals(cp.getChannel())) {
				String publish = cp.getParameter();
				return publish;
			}
		}
		return null;
	}

	/**
	 * Creates an error response.
	 * 
	 * @param description
	 *            Fail description, required.
	 * @return A {@link SuccessFail} message for the error.
	 */
	public static GeneratedMessage createFailResponse(String description) {
		SuccessFail sf = SuccessFail.newBuilder().setResult(Result.ERROR)
				.addItem(
						Item.newBuilder().setType(Item.Type.STR).setStr(
								description).build()).build();
		return sf;
	}

	/**
	 * Creates a simple success response.
	 * 
	 * @param string
	 *            An string argument, included if not null.
	 * @return A {@link SuccessFail} message for the success.
	 */
	public static GeneratedMessage createSuccessResponse(String string) {
		Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);
		if (string != null) {
			buildr.addItem(Item.newBuilder().setType(Item.Type.STR).setStr(
					string));
		}
		SuccessFail response = buildr.build();
		return response;
	}

}
