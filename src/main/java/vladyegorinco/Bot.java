package vladyegorinco;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class Bot extends TelegramLongPollingBot {
    private String botToken;
    private boolean waitingForUserResponse = false;
    private Long currentUserWaiting = null;


    private final InlineKeyboardButton redTag = InlineKeyboardButton.builder().text("🔴 - Important").callbackData("red").build();
    private final InlineKeyboardButton greenTag = InlineKeyboardButton.builder().text("🟢 - Not Important").callbackData("green").build();
    private final InlineKeyboardMarkup keyboardImportanceTag = InlineKeyboardMarkup.builder().
            keyboardRow(List.of(redTag)).
            keyboardRow(List.of(greenTag)).
            build();

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
        if (update.hasMessage()) {
            var msg = update.getMessage();
            var user = msg.getFrom();
            var id = user.getId();

            System.out.println("\nNew message!");
            System.out.println("User ID: " + id);
            System.out.println("Username: " + user.getUserName());

            if (msg.hasText()) {
                System.out.println("Text message: " + msg.getText());
                handleTextMessages(msg, id); // Handle the text message
            } else {
                IDontUnderstand(msg, id);
            }
        }

        if (update.hasCallbackQuery()) {
            try {
                handleCallbackQuery(update.getCallbackQuery());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
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


    private void handleTextMessages(Message msg, Long id) {
        if (msg.getText().equals("/start")) {
            sendText(id, "Welcome to Task Manager! \nSee what I can do by typing /help\n");

        } else if (msg.getText().equals("/addtask")) {
            sendMenu(id, "<b><i> Choose an importance tag for your task</i></b>", keyboardImportanceTag);
        } else if (msg.getText().equals("/removetask")) {
            sendText(id, "to be done");
        } else if (msg.getText().equals("/showtasklist")) {
            sendText(id, "to be done");
        } else if (msg.getText().equals("/help")) {
            sendText(id, "/addtask - Add a task\n/removetask - Remove a task\n/showtasklist - Show list of tasks");
        } else if (msg.getText().equalsIgnoreCase("hello") || msg.getText().equalsIgnoreCase("hi")) {
            sendText(id, "Hi there!");
        } else if (msg.getText().equalsIgnoreCase("привет") || msg.getText().equalsIgnoreCase("привет!")) {
            sendText(id, "Приветики!");
        } else if (msg.getText().equalsIgnoreCase("Thank you")) {
            sendText(id, "You are welcome!");
        } else if (msg.getText().equalsIgnoreCase("how are you") || msg.getText().equalsIgnoreCase("how are you?")) {
            sendText(id, "I'm good, thank you!");
        } else if (waitingForUserResponse && currentUserWaiting != null && currentUserWaiting.equals(id)) {
            // Handle response only if in waiting state and the user matches
            System.out.println("User response received: " + msg.getText());
            sendText(id, "You described the task as: " + msg.getText()+"\nSee the list of all tasks by typing \n/showtasklist");

            // Reset the flag after processing the response
            waitingForUserResponse = false;
            currentUserWaiting = null;
        } else {
            IDontUnderstand(msg, id);
        }
    }


    public void sendMenu(Long who, String txt, InlineKeyboardMarkup kb){
        SendMessage sm = SendMessage.builder().chatId(who.toString())
                .parseMode("HTML").text(txt)
                .replyMarkup(kb).build();

        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) throws TelegramApiException {
        Long chatId = callbackQuery.getMessage().getChatId();
        String queryId = callbackQuery.getId();
        String data = callbackQuery.getData();
        int messageId = callbackQuery.getMessage().getMessageId();

        // Log user choice
        System.out.println("User selected: " + data);

        // Acknowledge the callback query
        AnswerCallbackQuery closeQuery = AnswerCallbackQuery.builder()
                .callbackQueryId(queryId)
                .build();

        EditMessageReplyMarkup clearKeyboard = EditMessageReplyMarkup.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .replyMarkup(null)
                .build();

        String text;
        if (data.equals("red")) {
            text = "Describe the task (🔴 - Important)";
            waitingForUserResponse = true;
            currentUserWaiting = chatId; // Set the user who is expected to respond
        } else {
            text = "Describe the task (🟢 - Not Important)";
            waitingForUserResponse = true;
            currentUserWaiting = chatId; // Set the user who is expected to respond
        }

        EditMessageText newMessageText = EditMessageText.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .text(text)
                .build();

        // Execute actions
        execute(closeQuery);
        execute(clearKeyboard);
        execute(newMessageText);
    }



    private void IDontUnderstand(Message msg, Long id) {
        sendText(id, "I don't understand. Try /help to see what I can do.");

    }

}
