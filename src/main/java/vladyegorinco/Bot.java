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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

public class Bot extends TelegramLongPollingBot {


    private String botToken;
    private boolean waitingForUserResponse = false;
    private Long currentUserWaiting = null;
    private String selectedTag = null;

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
            sendText(id, "Welcome to Task Manager! \nSee what I can do by typing \n/help\n");

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
            // Log and process user response
            System.out.println("User response received: " + msg.getText());

            // Format the timestamp using the provided helper method
            String formattedDate = getFormattedDate(msg.getDate());

            // Save the task to the database
            saveTaskToDb(id, msg.getText(), selectedTag, formattedDate);

            // Send confirmation to the user
            sendText(id, "You added a task: " + msg.getText() + "\nSee the list of all tasks by typing \n/showtasklist");

            // Reset flags after processing
            waitingForUserResponse = false;
            currentUserWaiting = null;
            selectedTag = null;
        }else {
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
            selectedTag = "red";
            waitingForUserResponse = true;
            currentUserWaiting = chatId; // Set the user who is expected to respond
        } else {
            text = "Describe the task (🟢 - Not Important)";
            selectedTag = "green";
            waitingForUserResponse = true;
            currentUserWaiting = chatId; // Set the user who is expected to respond
        }

        EditMessageText newMessageText = EditMessageText.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .text(text)
                .build();
        //System.out.println(newMessageText);
        // Execute actions
        execute(closeQuery);
        execute(clearKeyboard);
        execute(newMessageText);
    }

    private static Connection connection = null; // Singleton connection

    // Establish a single database connection (lazy initialization)
    private static Connection getConnection() throws SQLException {
        if (connection == null) {
            synchronized (Bot.class) {
                if (connection == null) {
                    try {
                        // Load SQLite driver
                        Class.forName("org.sqlite.JDBC");
                        String dbUrl = "jdbc:sqlite:Tasks.db";
                        connection = DriverManager.getConnection(dbUrl);
                        System.out.println("Connected to database with URL: " + dbUrl);

                        // Ensure table exists (create schema if not already present)
                        initializeDatabase();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        throw new SQLException("SQLite JDBC Driver not found.");
                    }
                }
            }
        }
        return connection;
    }

    // Validate & ensure database schema is set up
    private static void initializeDatabase() {
        String schemaSql = "CREATE TABLE IF NOT EXISTS Tasks (" +
                "userID INTEGER NOT NULL, " +
                "taskName TEXT NOT NULL, " +
                "tag TEXT NOT NULL, " +
                "dateCreated TEXT NOT NULL" +
                ");";
        try (PreparedStatement stmt = getConnection().prepareStatement(schemaSql)) {
            stmt.execute();
            System.out.println("Ensured database schema is ready.");
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error while ensuring database schema: " + e.getMessage());
        }
    }


    // Optimized database save function
    private void saveTaskToDb(Long userId, String taskName, String tag, String formattedDate) {
        String sql = "INSERT INTO Tasks (userID, taskName, tag, dateCreated) VALUES (?, ?, ?, ?);";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            if (userId == null || taskName == null || tag == null || formattedDate == null) {
                System.err.println("Validation Error: One or more parameters are null.");
                return;
            }

            pstmt.setLong(1, userId);
            pstmt.setString(2, taskName);
            pstmt.setString(3, tag);
            pstmt.setString(4, formattedDate);

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("Task successfully saved to the database.");
            } else {
                System.err.println("Failed to save the task to the database.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Database operation error: " + e.getMessage());
        }
    }

    private String getFormattedDate(Integer unixTimestamp) {
        if (unixTimestamp == null || unixTimestamp <= 0) {
            // Return current date in UTC
            return LocalDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }

        try {
            // Parse timestamp in UTC
            LocalDateTime userDate = Instant.ofEpochSecond(unixTimestamp)
                    .atZone(ZoneId.of("UTC")) // Use UTC for consistency
                    .toLocalDateTime();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return userDate.format(formatter);
        } catch (Exception e) {
            e.printStackTrace();
            return LocalDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
    }

    private void IDontUnderstand(Message msg, Long id) {
        sendText(id, "I don't understand. Type /help to see what I can do.");

    }

}
