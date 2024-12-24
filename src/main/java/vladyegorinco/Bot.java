package vladyegorinco;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Bot extends TelegramLongPollingBot {


    private String botToken;
    private String aiPrompt;
    private boolean waitingForUserResponse = false;
    private Long currentUserWaiting = null;
    private String selectedTag = null;
    private boolean waitingForAIprompt = false;
    //private Boolean wayOfCallingTask = null;//true if by hand, false if AI
    private boolean waitingForTaskNumber = false; // Indicates bot is waiting for a task number
    private Long userWaitingForTaskNumber = null; // Tracks the user who is expected to respond
    private boolean waitingForAiAnswerApproval = false;
    private boolean waitingForWayOfNamingTask = false;
    private boolean willCallItMyself = false;
    private List<Integer> taskIdList = new ArrayList<>();
    public Groq groqie;
    private final InlineKeyboardButton redTag = InlineKeyboardButton.builder().text("🔴 - Important").callbackData("red").build();
    private final InlineKeyboardButton greenTag = InlineKeyboardButton.builder().text("🟢 - Not Important").callbackData("green").build();
    private final InlineKeyboardMarkup keyboardImportanceTag = InlineKeyboardMarkup.builder().
            keyboardRow(List.of(redTag)).
            keyboardRow(List.of(greenTag)).
            build();

    String aiResponse = "";
    //private final ReplyKeyboardMarkup whatis = ReplyKeyboardMarkup.builder().


    @Override
    public String getBotUsername() {
        return "dailytaskmanager_bot";

    }

    public Bot() {
        // Load properties from config file in resources folder
        this.groqie = new Groq();
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new FileNotFoundException("config.properties not found in resources folder.");
            }
            properties.load(input);
            botToken = properties.getProperty("TELEGRAM_BOT_TOKEN");
            aiPrompt = properties.getProperty("AI_PROMPT");
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









    public void sendWayOfNamingTaskKeyboard(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Choose one of the options below");

        // Define the custom keyboard
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true); // Adjust keyboard size for user
        keyboardMarkup.setOneTimeKeyboard(true);
        // Create keyboard rows
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Generate task's name with the help of AI 🤖");


        KeyboardRow row2 = new KeyboardRow();
        row2.add("I'll name the task myself");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Cancel");

        // Add rows to the keyboard
        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        // Attach keyboard to the markup
        keyboardMarkup.setKeyboard(keyboard);

        // Attach the keyboard to the message
        message.setReplyMarkup(keyboardMarkup);

        // Send the message
        try {
            execute(message); // execute() sends the message to the user
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String aiResponseTest(Long id, Message msg){
        String airesponse = null;
        aiPrompt = "Rewrite the task name to sound slightly more advanced while keeping it short. The task name will be either in Russian or English. If the task name is in Russian, respond ONLY in Russian. If the task name is in English, respond ONLY in English. Do not mix languages. Provide ONLY the rewritten task name without any extra text or explanations. Examples: выгулять собаку → прогулка с компаньоном, приготовить ужин → вечернее приготовление еды, peel potatoes → potato skin elimination, walk the dog → Canine Exercise Routine. REMEMBER: Identify the language of the task name and respond in the SAME language, either Russian or English. Answer should BEGIN from a capital letter in both languages. TASK NAME:";
        String text = msg.getText();
        try {
            airesponse = groqie.sendMessage(aiPrompt+text);
            System.out.println(aiPrompt+text);
        } catch (IOException e) {
            airesponse = "Sorry, I couldn't process your request. Please try again later.";
            e.printStackTrace();  // Log the error
        }

        //sendText(id,"Response: " + airesponse);
        return airesponse;
        //System.out.println(id + airesponse);  // Send response back to the user
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
            showTasksToRemove(id);
        } else if (waitingForTaskNumber && userWaitingForTaskNumber != null && userWaitingForTaskNumber.equals(id)) {
            removeTask(id, msg.getText().trim());
        }  else if (msg.getText().equals("/showtasklist")) {
            printTasks(id);
        } else if (msg.getText().equals("/help")) {
            sendText(id, "/addtask - Add a task\n/removetask - Remove a task\n/showtasklist - Show list of tasks");//update
        } else if(msg.getText().equals("/showimportant")){
            showOnlyOneTagTask(id,"red");
        }
        else if(msg.getText().equals("/shownotimportant")){
            showOnlyOneTagTask(id,"green");
        } else if(msg.getText().equals("/keyboard")){
            sendWayOfNamingTaskKeyboard(msg.getChatId());
        }
        else if (msg.getText().equalsIgnoreCase("hello") || msg.getText().equalsIgnoreCase("hi")) {
            sendText(id, "Hi there!");
        } else if (msg.getText().equalsIgnoreCase("привет") || msg.getText().equalsIgnoreCase("привет!")) {
            sendText(id, "Приветики!");
        } else if (msg.getText().equalsIgnoreCase("Thank you")) {
            sendText(id, "You are welcome!");
        } else if (msg.getText().equalsIgnoreCase("how are you") || msg.getText().equalsIgnoreCase("how are you?")) {
            sendText(id, "I'm good, thank you!");
        } else if(msg.getText().equalsIgnoreCase("/tryai")){

            waitingForAIprompt = true;
        } else if(msg.getText().equals("I'll name the task myself") && waitingForWayOfNamingTask){
            willCallItMyself = true;
            sendText(id,"Describe the task: ");
        } else if(msg.getText().equals("Generate task's name with the help of AI 🤖") && waitingForWayOfNamingTask){

            sendText(id,"Describe the task for AI to improve it: ");
            waitingForAIprompt = true;


        }
        else if (waitingForUserResponse && currentUserWaiting != null && currentUserWaiting.equals(id) && willCallItMyself) {
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
            willCallItMyself = false;

        }else if(waitingForAIprompt){
            aiResponse = aiResponseTest(id, msg);
            sendText(id, "AI came up with this task name: " + aiResponse + "\nDo you like it?");
            waitingForAiAnswerApproval = true;
            waitingForAIprompt = false;
        } else if(waitingForAiAnswerApproval){
            if(msg.getText().equalsIgnoreCase("yes")){
                String formattedDate = getFormattedDate(msg.getDate());

                // Save the task to the database
                saveTaskToDb(id, aiResponse, selectedTag, formattedDate);

                // Send confirmation to the user
                sendText(id, "You added a task: " + aiResponse + "\nSee the list of all tasks by typing \n/showtasklist");
            }else if(msg.getText().equalsIgnoreCase("no")){
                System.out.println("user said no");
            }
            waitingForAiAnswerApproval = false;
        }
        else {
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

        execute(closeQuery);

        String updatedText = "";
        if (data.equals("red")) {
            //text = "Describe the task (🔴 - Important)";
            //text = "Choose one of the options below";
            selectedTag = "red";
            updatedText = "✅ You chose: 🔴 Important";

            waitingForUserResponse = true;
            currentUserWaiting = chatId; // Set the user who is expected to respond
            waitingForWayOfNamingTask = true;
            //sendCustomKeyboard(callbackQuery.getMessage().getChatId());

        } else {
            //text = "Describe the task (🟢 - Not Important)";
            //text = "Choose one of the options below";
            selectedTag = "green";
            updatedText = "✅ You chose: 🟢 Not Important";
            waitingForUserResponse = true;
            currentUserWaiting = chatId; // Set the user who is expected to respond
            waitingForWayOfNamingTask = true;
            //sendCustomKeyboard(callbackQuery.getMessage().getChatId());
        }
        EditMessageText editMessageText = EditMessageText.builder()
                .chatId(chatId.toString())  // The chat where the message is
                .messageId(messageId)       // The message ID to update
                .text(updatedText)          // Updated text
                .build();

        execute(editMessageText);
//
        sendWayOfNamingTaskKeyboard(chatId);

        //execute(newMessageText);
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
        System.out.println("Database is assumed to be initialized.");
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

    private void printTasks(Long userId) {
        String sql = "SELECT taskName, tag, dateCreated FROM Tasks WHERE userID = ? ORDER BY CASE WHEN tag = 'red' THEN 1 ELSE 2 END;";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, userId);

            try (var rs = pstmt.executeQuery()) {
                StringBuilder tasks = new StringBuilder("Your Tasks:\n\n");
                int taskCount = 0;

                while (rs.next()) {
                    taskCount++;
                    String taskName = rs.getString("taskName");
                    String tag = rs.getString("tag").equals("red") ? "🔴" : "🟢";
                    String dateCreated = rs.getString("dateCreated");

                    tasks.append(tag).append(taskCount).append(". ").append(taskName)
                            .append(" (").append("Created: ").append(dateCreated).append(")\n");
                }

                if (taskCount == 0) {
                    sendText(userId, "You don't have any tasks yet. Add a new task using \n/addtask.");
                } else {
                    sendText(userId, tasks.toString());
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sendText(userId, "An error occurred while retrieving your tasks. Please try again later.");
        }
    }

    private void showOnlyOneTagTask(Long userId, String tag) {
        String sql = "SELECT taskName, tag, dateCreated FROM Tasks WHERE userID = ? and tag = ?;";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setString(2,tag);
            try (var rs = pstmt.executeQuery()) {
                StringBuilder tasks = new StringBuilder("Your " + (tag.equals("red") ? "🔴" : "🟢") + " Tasks:\n\n");
                int taskCount = 0;

                while (rs.next()) {
                    taskCount++;
                    String taskName = rs.getString("taskName");
                    String tagToPrint = rs.getString("tag").equals("red") ? "🔴" : "🟢";
                    String dateCreated = rs.getString("dateCreated");

                    tasks.append(tagToPrint).append(taskCount).append(". ").append(taskName)
                            .append(" (").append("Created: ").append(dateCreated).append(")\n");
                }

                if (taskCount == 0) {
                    sendText(userId, "You don't have any tasks yet. Add a new task using \n/addtask.");
                } else {
                    sendText(userId, tasks.toString());
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sendText(userId, "An error occurred while retrieving your tasks. Please try again later.");
        }
    }

    private void showTasksToRemove(Long userId) {
        String sql = "SELECT rowid, taskName, tag, dateCreated FROM Tasks WHERE userID = ? ORDER BY CASE WHEN tag = 'red' THEN 1 ELSE 2 END;";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:Tasks.db");
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {//collect all correct rows in the resultset
                StringBuilder message = new StringBuilder("Select a task number to delete:\n\n");
                taskIdList.clear();
                int index = 1;

                while (rs.next()) {//traverse through the resultset
                    int taskId = rs.getInt("rowid");
                    String taskName = rs.getString("taskName");
                    String tag = rs.getString("tag").equals("red") ? "🔴" : "🟢";
                    String dateCreated = rs.getString("dateCreated");

                    message.append(tag).append(index).append(". ").append(taskName).append(" - ").append(dateCreated).append("\n");
                    taskIdList.add(taskId);
                    index++;
                }

                if (taskIdList.isEmpty()) {
                    message.append("You have no tasks to remove.");
                } else {
                    message.append("\nSend the number of the task to delete.");
                    waitingForTaskNumber = true;
                    userWaitingForTaskNumber = userId;
                }

                sendText(userId, message.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendText(userId, "Failed to fetch tasks.");
        }
    }

    private void removeTask(Long userId, String userInput) {
        try {
            int taskNumber = Integer.parseInt(userInput);
            if (taskNumber >= 1 && taskNumber <= taskIdList.size()) {
                int taskIdToRemove = taskIdList.get(taskNumber - 1);

                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:Tasks.db");
                     PreparedStatement stmt = conn.prepareStatement("DELETE FROM Tasks WHERE rowid = ?")) {

                    stmt.setInt(1, taskIdToRemove);
                    stmt.executeUpdate();
                    sendText(userId, "Task removed successfully.");
                }

                waitingForTaskNumber = false;
                userWaitingForTaskNumber = null;
                taskIdList.clear();
            } else {
                sendText(userId, "Invalid task number. Please try again.");
            }
        } catch (NumberFormatException e) {
            sendText(userId, "Please send a valid number.");
        } catch (SQLException e) {
            e.printStackTrace();
            sendText(userId, "Failed to remove task.");
        }
    }


    private String getFormattedDate(Integer unixTimestamp) {//formatting time to dd/mm/yyyy format
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