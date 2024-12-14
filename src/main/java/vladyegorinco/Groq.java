package vladyegorinco;

import okhttp3.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Groq {
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private String apikey;
    private final OkHttpClient client;

    public Groq() {
        this.client = new OkHttpClient();
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new FileNotFoundException("config.properties not found in resources folder.");
            }
            properties.load(input);
            apikey = properties.getProperty("AI-API-KEY");
            if (apikey == null || apikey.isEmpty()) {
                throw new IllegalArgumentException("API Key is missing or invalid in config.properties");
            }
            System.out.println("API Key loaded successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public String sendMessage(String userInput) throws IOException {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", "llama-3.3-70b-versatile");
        JSONArray messagesArray = new JSONArray();
        messagesArray.put(new JSONObject().put("role", "user").put("content", userInput));
        jsonBody.put("messages", messagesArray);
        jsonBody.put("max_tokens", 1000);

        System.out.println("Sending Request Body: " + jsonBody.toString());

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                jsonBody.toString()
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + apikey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                System.out.println("Error: " + response.code() + " - " + response.message());
                System.out.println("Response Body: " + errorBody);
                throw new IOException("Unexpected response code: " + response.code());
            }

            String responseBody = response.body().string();
            System.out.println("API Response: " + responseBody);

            JSONObject jsonResponse = new JSONObject(responseBody);
            return jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        }
    }
}
