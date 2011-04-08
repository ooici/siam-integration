package siam;

/**
 * Interface for objects to be notified about asynchronous calls via the
 * {@link IAsyncSiam} interface.
 * 
 * @author carueda
 * 
 * @param <T>
 *            The type for the result in the
 *            {@link AsyncCallback#onSuccess(Object)} operation.
 */
public interface AsyncCallback<T> {

	/**
	 * Called if an exception happens while trying to complete the requested
	 * operation.
	 * 
	 * @param e
	 *            the exception
	 */
	public void onFailure(Throwable e);

	/**
	 * Called upon a successful completion of the requested operation.
	 * 
	 * @param result
	 *            The result
	 */
	public void onSuccess(T result);
}