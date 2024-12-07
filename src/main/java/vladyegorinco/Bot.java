package vladyegorinco;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Bot extends TelegramLongPollingBot {
    private String botToken;

    @Override
    public String getBotUsername() {
        return "dailytaskmanager_bot";
    }

    public Bot() {
        // Load properties from config file in resources folder
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new FileNotFoundException("config.properties not found in resources folder.");
            }
            properties.load(input);
            botToken = properties.getProperty("TELEGRAM_BOT_TOKEN");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1); // Exit if the configuration file can't be loaded
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Handle updates here
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            System.out.printf("Received message: %s from chat ID: %d%n", messageText, chatId);
            // Add further handling logic as needed
        }
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
    public void sendText(Long who, String what){
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString()) //Who are we sending a message to
                .text(what).build();    //Message content
        try {
            execute(sm);                        //Actually sending the message
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);      //Any error will be printed here
        }
    }

}
