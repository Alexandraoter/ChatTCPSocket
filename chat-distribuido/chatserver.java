import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private final int port;
    private final List<String> replicaServers;
    private final CopyOnWriteArrayList<ClientHandler> clients;
    private final CopyOnWriteArrayList<String> messageHistory;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private final String serverName;

    public ChatServer(int port, String serverName, List<String> replicaServers) {
        this.port = port;
        this.serverName = serverName;
        this.replicaServers = replicaServers;
        this.clients = new CopyOnWriteArrayList<>();
        this.messageHistory = new CopyOnWriteArrayList<>();
        this.running = true;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[" + serverName + "] Servidor iniciado en puerto " + port);
            
            // Hilo para sincronizar con réplicas
            new Thread(this::syncWithReplicas).start();
            
            // Hilo para heartbeat
            new Thread(this::sendHeartbeat).start();

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket);
                    clients.add(handler);
                    new Thread(handler).start();
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error aceptando cliente: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error iniciando servidor: " + e.getMessage());
        }
    }

    private void syncWithReplicas() {
        while (running) {
            try {
                Thread.sleep(5000); // Sincronizar cada 5 segundos
                for (String replica : replicaServers) {
                    try {
                        String[] parts = replica.split(":");
                        String host = parts[0];
                        int replicaPort = Integer.parseInt(parts[1]);
                        
                        Socket socket = new Socket();
                        socket.connect(new InetSocketAddress(host, replicaPort), 2000);
                        
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        
                        out.println("SYNC_REQUEST");
                        String response = in.readLine();
                        
                        if (response != null && response.startsWith("SYNC_DATA:")) {
                            String data = response.substring(10);
                            if (!data.isEmpty()) {
                                String[] messages = data.split("\\|\\|");
                                for (String msg : messages) {
                                    if (!messageHistory.contains(msg) && !msg.isEmpty()) {
                                        messageHistory.add(msg);
                                    }
                                }
                            }
                        }
                        
                        socket.close();
                    } catch (Exception e) {
                        // Réplica no disponible
                    }
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void sendHeartbeat() {
        while (running) {
            try {
                Thread.sleep(3000);
                broadcast("HEARTBEAT:" + serverName, null);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
        
        // Replicar a otros servidores
        if (!message.startsWith("HEARTBEAT:") && !message.startsWith("SYNC_")) {
            replicateToServers(message);
        }
    }

    private void replicateToServers(String message) {
        for (String replica : replicaServers) {
            try {
                String[] parts = replica.split(":");
                String host = parts[0];
                int replicaPort = Integer.parseInt(parts[1]);
                
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(host, replicaPort), 1000);
                
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("REPLICATE:" + message);
                socket.close();
            } catch (Exception e) {
                // Réplica no disponible
            }
        }
    }

    class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("SERVIDOR:" + serverName);
                out.println("Ingresa tu nombre de usuario:");
                username = in.readLine();
                
                if (username == null || username.trim().isEmpty()) {
                    socket.close();
                    return;
                }

                System.out.println("[" + serverName + "] " + username + " conectado");
                
                // Enviar historial
                out.println("--- Historial de mensajes ---");
                for (String msg : messageHistory) {
                    out.println(msg);
                }
                out.println("--- Fin del historial ---");

                String joinMsg = username + " se unió al chat";
                messageHistory.add(joinMsg);
                broadcast(joinMsg, this);

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equals("SYNC_REQUEST")) {
                        StringBuilder syncData = new StringBuilder("SYNC_DATA:");
                        for (String msg : messageHistory) {
                            syncData.append(msg).append("||");
                        }
                        out.println(syncData.toString());
                    } else if (message.startsWith("REPLICATE:")) {
                        String replicatedMsg = message.substring(10);
                        if (!messageHistory.contains(replicatedMsg)) {
                            messageHistory.add(replicatedMsg);
                        }
                    } else {
                        String fullMsg = username + ": " + message;
                        messageHistory.add(fullMsg);
                        broadcast(fullMsg, this);
                    }
                }
            } catch (IOException e) {
                System.err.println("[" + serverName + "] Error con cliente: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void cleanup() {
            clients.remove(this);
            if (username != null) {
                String leaveMsg = username + " salió del chat";
                messageHistory.add(leaveMsg);
                broadcast(leaveMsg, this);
                System.out.println("[" + serverName + "] " + username + " desconectado");
            }
            try {
                socket.close();
            } catch (IOException e) {
                // Ignorar
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java ChatServer <puerto> <nombre> [replica1:puerto1] [replica2:puerto2] ...");
            System.out.println("Ejemplo: java ChatServer 5000 Server1 localhost:5001 localhost:5002");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String serverName = args[1];
        List<String> replicas = new ArrayList<>();
        
        for (int i = 2; i < args.length; i++) {
            replicas.add(args[i]);
        }

        ChatServer server = new ChatServer(port, serverName, replicas);
        server.start();
    }
}
