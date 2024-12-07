package vladyegorinco;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Bot extends TelegramLongPollingBot {
    private String botToken;

    @Override
    public String getBotUsername() {
        return null;
    }

    public Bot() {
        // Load properties from config file
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            properties.load(fis);
            botToken = properties.getProperty("TELEGRAM_BOT_TOKEN");
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the error, maybe set a default or exit
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}