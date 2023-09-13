import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatServer {
    private static final int PORT = 3000;
    private static Map<PrintWriter, String> clientMap = new HashMap<>();
    private static Map<String, String> userCredentials = new HashMap<>();
    private static List<String> chatHistory = new ArrayList<>();

    public static void main(String[] args) {
        userCredentials.put("Diptayan", "12");
        userCredentials.put("Palash", "12");
        userCredentials.put("Srijit", "12");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat server is running...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket);

                Thread clientThread = new ClientHandler(clientSocket);
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter writer;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);

                String username = reader.readLine();
                String password = reader.readLine();

                if (authenticateUser(username, password)) {
                    writer.println("AUTHENTICATED");
                    this.username = username;

                    synchronized (clientMap) {
                        clientMap.put(writer, username);
                    }

                    // Send chat history to the client
                    for (String historyMessage : chatHistory) {
                        writer.println(historyMessage);
                    }

                    String message;
                    while ((message = reader.readLine()) != null) {
                        if (message.startsWith("SEND_FILE:")) {
                            String[] parts = message.split(":", 3);
                            if (parts.length == 3) {
                                String sender = username;
                                String recipient = parts[1];
                                String fileName = parts[2];

                                broadcastFileTransfer(sender, recipient, fileName);
                            }
                        } else {
                            String formattedMessage = username + ": " + message;
                            System.out.println("Received: " + formattedMessage);
                            chatHistory.add(formattedMessage);
                            broadcast(formattedMessage);
                        }
                    }
                } else {
                    writer.println("AUTH_FAILED");
                    socket.close();
                }
                InputStream inputStream = socket.getInputStream();
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);

                FileOutputStream fileOutputStream = new FileOutputStream("received_file.txt"); // Modify the filename as
                                                                                               // needed
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = objectInputStream.read(buffer)) != -1) {
                    bufferedOutputStream.write(buffer, 0, bytesRead);
                }

                bufferedOutputStream.flush();
                bufferedOutputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                synchronized (clientMap) {
                    clientMap.remove(writer);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        private void broadcast(String message) {
            synchronized (clientMap) {
                for (PrintWriter writer : clientMap.keySet()) {
                    writer.println(message);
                }
            }
        }

        private void broadcastFileTransfer(String sender, String recipient, String fileName) {
            synchronized (clientMap) {
                for (PrintWriter writer : clientMap.keySet()) {
                    if (clientMap.get(writer).equals(recipient)) {
                        writer.println("RECEIVE_FILE:" + sender + ":" + fileName);
                    }
                }
            }
        }

        private boolean authenticateUser(String username, String password) {
            return userCredentials.containsKey(username) && userCredentials.get(username).equals(password);
        }
    }
}
