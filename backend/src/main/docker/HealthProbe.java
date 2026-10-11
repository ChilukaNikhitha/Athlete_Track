import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

public final class HealthProbe {
    private HealthProbe() {}

    public static void main(String[] args) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(args[0]).toURL().openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                System.exit(1);
            }
        } finally {
            connection.disconnect();
        }
    }
}
