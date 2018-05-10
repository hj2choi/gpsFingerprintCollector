package hk.ust.utils;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Created by HongJoon on 25-Oct-17.
 *
 * @Usage: HttpJSONUtils.sendJSONRequest(url, msg, callback, new ResponseHandler{});
 *
 *
 * TODO: read multiple lines of response (currently only gets first line of response String)
 * TODO: support multiple request method (only POST is supported now)
 */

public class HttpJSONUtils {

    public interface ResponseHandler {
        void callback(int responseCode, String response);
    }

    public static void sendJSONRequest(String url, JSONObject msg, ResponseHandler handler) {
        //Log.d("Debug:HttpJSONUtils:","SEND JSON REQUEST");
        HTTPThread workerThread = new HTTPThread(url, msg, handler);
        workerThread.start();
    }

    private static class HTTPThread extends Thread {
        private String targetURL;
        private JSONObject message;
        private ResponseHandler responseHandler;
        public HTTPThread(String url, JSONObject msg, ResponseHandler handler) {
            this.targetURL = url;
            this.message = msg;
            this.responseHandler = handler;
        }
        public void run() {
            Log.d("Debug:HttpJSONUtils:","SEND JSON REQUEST");
            HttpURLConnection client = null;
            try {
                URL url = new URL(targetURL);
                client = (HttpURLConnection) url.openConnection();
                client.setRequestMethod("POST");
                client.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                client.setRequestProperty("Accept", "application/json");
                client.setFixedLengthStreamingMode(message.toString().getBytes().length);
                client.setDoOutput(true);

                // send request
                OutputStream out = new BufferedOutputStream(client.getOutputStream());
                out.write(message.toString().getBytes());
                out.flush();

                // read response message
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String response = in.readLine();

                responseHandler.callback(client.getResponseCode(), response);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("Debug:HttpJSONUtils:","HTTP REQUEST ERROR: "+e);
            }
            finally {
                if(client != null) // Make sure the connection is not null.
                    client.disconnect();
            }
        }
    }

}
