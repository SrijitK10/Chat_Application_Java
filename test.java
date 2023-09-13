import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Base64;

public class test extends JFrame {
    private static final String SERVER_ADDRESS = "10.3.131.199";
    private static final int SERVER_PORT = 3000;

    private PrintWriter writer;

    private JTextField inputField;
    private JTextArea chatArea;
    private JButton sendButton; // New button for sending messages

    public test() {
        initUI();
        connectToServer();
    }

    private void initUI() {
        setTitle("Chat Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 300);

        inputField = new JTextField();
        chatArea = new JTextArea();
        chatArea.setEditable(false);

        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        sendButton = new JButton("Send"); // Initialize the send button
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        inputField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(chatScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void connectToServer() {
        int loginAttempts = 0;

        while (loginAttempts < 3) {
            try {
                Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);

                String name = JOptionPane.showInputDialog("Enter your name:");
                writer.println(name);
                String password = JOptionPane.showInputDialog("Enter your password:");
                writer.println(password);

                String response = reader.readLine();
                if (response.equals("AUTHENTICATED")) {
                    JOptionPane.showMessageDialog(this, "Authenticated successfully!");
                    break;
                } else if (response.equals("AUTH_FAILED")) {
                    loginAttempts++;
                    JOptionPane.showMessageDialog(this, "Authentication failed. Please try again.");
                }

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (loginAttempts >= 3) {
            JOptionPane.showMessageDialog(this, "Authentication failed 3 times. Exiting...");
            System.exit(0);
        }

        Thread outputThread = new Thread(() -> {
            try {
                Socket socket;
                socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String message;
                while ((message = reader.readLine()) != null) {
                    chatArea.append(message + "\n");
                }

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        outputThread.start();
    }

    private void sendMessage() {
        String message = inputField.getText();
        if (!message.isEmpty()) {
            if (message.startsWith("/sendfile")) {
                String filePath = message.substring(10); // Remove the '/sendfile ' prefix
                sendFile(filePath);
            } else {
                writer.println(message);
            }
            inputField.setText("");
        }
    }
    
    private void sendFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] fileData = fileInputStream.readAllBytes();
                fileInputStream.close();
    
                String base64FileData = Base64.getEncoder().encodeToString(fileData);
                writer.println("/file " + file.getName()); // Send a file marker
                writer.println(base64FileData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new test());
    }
}