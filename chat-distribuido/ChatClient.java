import java.io.*;
import java.net.*;
import java.util.*;

public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private BufferedReader consoleReader;
    private String username;
    private List<ServerInfo> servers;
    private int currentServerIndex;
    private volatile boolean running;
    private String currentServerName;

    static class ServerInfo {
        String host;
        int port;

        ServerInfo(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public String toString() {
            return host + ":" + port;
        }
    }

    public ChatClient(List<ServerInfo> servers) {
        this.servers = servers;
        this.currentServerIndex = 0;
        this.running = true;
        this.consoleReader = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start() {
        if (!connectToServer()) {
            System.out.println("No se pudo conectar a ningún servidor. Saliendo...");
            return;
        }

        // Hilo para recibir mensajes
        Thread receiverThread = new Thread(this::receiveMessages);
        receiverThread.start();

        // Hilo para enviar mensajes
        sendMessages();
    }

    private boolean connectToServer() {
        int attempts = 0;
        int maxAttempts = servers.size() * 2;

        while (attempts < maxAttempts && running) {
            ServerInfo server = servers.get(currentServerIndex);
            
            try {
                System.out.println("Conectando a " + server + "...");
                socket = new Socket();
                socket.connect(new InetSocketAddress(server.host, server.port), 3000);
                
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Leer nombre del servidor
                String serverMsg = in.readLine();
                if (serverMsg != null && serverMsg.startsWith("SERVIDOR:")) {
                    currentServerName = serverMsg.substring(9);
                    System.out.println("✓ Conectado a " + currentServerName);
                }

                // Si ya tenemos username, enviarlo
                if (username != null) {
                    String prompt = in.readLine(); // Leer prompt de usuario
                    out.println(username);
                    System.out.println("Reconectado como " + username);
                } else {
                    String prompt = in.readLine();
                    System.out.print(prompt + " ");
                    username = consoleReader.readLine();
                    out.println(username);
                }

                return true;

            } catch (IOException e) {
                System.out.println("✗ Fallo al conectar a " + server + ": " + e.getMessage());
                currentServerIndex = (currentServerIndex + 1) % servers.size();
                attempts++;
                
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }

        return false;
    }

    private void receiveMessages() {
        while (running) {
            try {
                String message = in.readLine();
                
                if (message == null) {
                    // Conexión perdida
                    System.out.println("\n⚠ Conexión perdida con " + currentServerName);
                    System.out.println("Intentando reconectar...");
                    
                    closeCurrentConnection();
                    currentServerIndex = (currentServerIndex + 1) % servers.size();
                    
                    if (connectToServer()) {
                        System.out.println("✓ Reconexión exitosa");
                    } else {
                        System.out.println("✗ No se pudo reconectar. Saliendo...");
                        running = false;
                        break;
                    }
                } else if (message.startsWith("HEARTBEAT:")) {
                    // Ignorar heartbeats en cliente
                    continue;
                } else {
                    System.out.println(message);
                }
                
            } catch (IOException e) {
                if (running) {
                    System.out.println("\n⚠ Error de conexión: " + e.getMessage());
                    System.out.println("Intentando reconectar...");
                    
                    closeCurrentConnection();
                    currentServerIndex = (currentServerIndex + 1) % servers.size();
                    
                    if (!connectToServer()) {
                        System.out.println("✗ No se pudo reconectar. Saliendo...");
                        running = false;
                        break;
                    }
                }
            }
        }
    }

    private void sendMessages() {
        try {
            while (running) {
                String message = consoleReader.readLine();
                
                if (message == null || message.equalsIgnoreCase("/salir")) {
                    running = false;
                    break;
                }

                if (message.equalsIgnoreCase("/servidores")) {
                    System.out.println("\n=== Servidores disponibles ===");
                    for (int i = 0; i < servers.size(); i++) {
                        String indicator = (i == currentServerIndex) ? " [ACTUAL]" : "";
                        System.out.println((i + 1) + ". " + servers.get(i) + indicator);
                    }
                    System.out.println("==============================\n");
                    continue;
                }

                if (message.equalsIgnoreCase("/ayuda")) {
                    System.out.println("\n=== Comandos disponibles ===");
                    System.out.println("/servidores - Muestra lista de servidores");
                    System.out.println("/ayuda - Muestra esta ayuda");
                    System.out.println("/salir - Salir del chat");
                    System.out.println("===========================\n");
                    continue;
                }

                if (out != null && !message.trim().isEmpty()) {
                    out.println(message);
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo entrada: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void closeCurrentConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignorar
        }
    }

    private void cleanup() {
        running = false;
        closeCurrentConnection();
        System.out.println("\nDesconectado del chat. ¡Hasta luego!");
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Uso: java ChatClient <servidor1:puerto1> <servidor2:puerto2> ...");
            System.out.println("Ejemplo: java ChatClient localhost:5000 localhost:5001 localhost:5002");
            return;
        }

        List<ServerInfo> servers = new ArrayList<>();
        
        for (String arg : args) {
            try {
                String[] parts = arg.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                servers.add(new ServerInfo(host, port));
            } catch (Exception e) {
                System.err.println("Formato inválido: " + arg);
            }
        }

        if (servers.isEmpty()) {
            System.out.println("No se especificaron servidores válidos.");
            return;
        }

        System.out.println("=== Cliente de Chat Distribuido ===");
        System.out.println("Servidores configurados: " + servers.size());
        
        ChatClient client = new ChatClient(servers);
        client.start();
    }
}
