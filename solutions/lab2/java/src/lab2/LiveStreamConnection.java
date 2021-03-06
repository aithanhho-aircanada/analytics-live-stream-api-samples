package lab2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class LiveStreamConnection {

  public static final Logger log = LoggerFactory.getLogger(LiveStreamConnection.class);

  protected String endpointUrl;
  protected String accessToken;

  public LiveStreamConnection(String endpointUrl, String accessToken)
  {
    this.endpointUrl = endpointUrl;
    this.accessToken = accessToken;
  }

  public void open() throws IOException {
    log.debug("opening live stream connection to " + this.endpointUrl);
    URL url = new URL(this.endpointUrl);
    HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
    connection.setReadTimeout(10000);
    connection.setConnectTimeout(10000);
    connection.setRequestProperty("Authorization", "Bearer " + this.accessToken);
    connection.setRequestProperty("Accept-Encoding", "gzip");
    connection.setRequestMethod("GET");

    log.debug("HTTP response code "+connection.getResponseCode());

    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
      throw new IOException(connection.getResponseMessage() + " (" + connection.getResponseCode() + ")");
    }

    InputStream inputStream = new GZIPInputStream(connection.getInputStream());
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

    String line;
    while ((line = reader.readLine()) != null) {
      if (line.length() > 0) {
	System.out.println(line);
	calculate_lag(line);
	System.out.println("-------------------------------------");
      }
    }
  }

  public void calculate_lag(String record) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> jsonModel = mapper.readValue(record, Map.class);
    Integer timestamp = (Integer)jsonModel.get("receivedTimeGMT");
    if (timestamp == null) {
      log.error("unable to find receivedTimeGMT field in record for lag calculation");
    } else {
      long now = System.currentTimeMillis() / 1000; // now should be in seconds
      System.out.println("calculate_lag: record lag is " + (now - timestamp.longValue()) + " seconds");
    }
  }
}
