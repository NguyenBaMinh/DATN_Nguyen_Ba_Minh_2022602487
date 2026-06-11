package app.edu.app.interfaces;

/**
 * Interface for callbacks when fetching URLs from Firebase Storage
 */
public interface OnUrlFetchedListener {
    /**
     * Called when a URL has been fetched or when the fetch operation fails
     *
     * @param url The URL string if successful, null if not found or an error occurred
     */
    void onUrlFetched(String url);
}