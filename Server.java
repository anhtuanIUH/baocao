import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 8888;
    // Danh sách các phòng: Tên phòng -> Tập hợp các client đang ở trong phòng đó
    private static final Map<String, Set<ClientHandler>> rooms = new ConcurrentHashMap<>();
    private static final Set<ClientHandler> allClients = ConcurrentHashMap.newKeySet();
    private static final Set<PrintWriter> dashboardWriters = ConcurrentHashMap.newKeySet();

    static {
        rooms.put("Study", ConcurrentHashMap.newKeySet());
        rooms.put("Game", ConcurrentHashMap.newKeySet());
        rooms.put("TraiNghiem", ConcurrentHashMap.newKeySet());
        rooms.put("ChatChit", ConcurrentHashMap.newKeySet()); // Phòng đề xuất thêm
    }

    public static void main(String[] args) {
        System.out.println("[SERVER STARTED] Server đang lắng nghe tại cổng " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastDashboard() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"stats\",\"total_clients\":").append(allClients.size()).append(",\"rooms\":{");
        
        int rIdx = 0;
        for (Map.Entry<String, Set<ClientHandler>> entry : rooms.entrySet()) {
            if (rIdx > 0) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":[");
            int cIdx = 0;
            for (ClientHandler client : entry.getValue()) {
                if (cIdx > 0) sb.append(",");
                sb.append("\"").append(client.getClientName()).append("\"");
                cIdx++;
            }
            sb.append("]");
            rIdx++;
        }
        sb.append("}}\n");
        String jsonMsg = sb.toString();

        for (PrintWriter writer : dashboardWriters) {
            writer.print(jsonMsg);
            writer.flush();
        }
    }

    public static Set<String> getRoomNames() {
        return rooms.keySet();
    }

    public static void joinRoom(String roomName, ClientHandler client) {
        leaveCurrentRoom(client);
        if (rooms.containsKey(roomName)) {
            rooms.get(roomName).add(client);
            client.setCurrentRoom(roomName);
            client.sendMessage("JOINED:" + roomName);
            broadcastDashboard();
        }
    }

    public static void leaveCurrentRoom(ClientHandler client) {
        String currentRoom = client.getCurrentRoom();
        if (currentRoom != null && rooms.containsKey(currentRoom)) {
            rooms.get(currentRoom).remove(client);
            client.setCurrentRoom(null);
            client.sendMessage("LEFT_ROOM");
            broadcastDashboard();
        }
    }

    public static void sendToRoom(String roomName, String senderName, String message) {
        if (rooms.containsKey(roomName)) {
            String packet = "[" + senderName + "]: " + message + "\n";
            for (ClientHandler client : rooms.get(roomName)) {
                if (!client.getClientName().equals(senderName)) {
                    client.sendMessage(packet);
                }
            }
        }
    }

    public static void addDashboard(PrintWriter writer) {
        dashboardWriters.add(writer);
        broadcastDashboard();
    }

    public static void removeDashboard(PrintWriter writer) {
        dashboardWriters.remove(writer);
    }

    public static void removeClient(ClientHandler client) {
        leaveCurrentRoom(client);
        allClients.remove(client);
        broadcastDashboard();
    }

    public static void registerClient(ClientHandler client) {
        allClients.add(client);
    }
}

class ClientHandler extends Thread {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientName;
    private String currentRoom;
    private boolean isDashboard = false;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public String getClientName() { return clientName; }
    public String getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(String room) { this.currentRoom = room; }
    public void sendMessage(String msg) {
        if (out != null) {
            out.print(msg + (msg.endsWith("\n") ? "" : "\n"));
            out.flush();
        }
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            clientName = in.readLine();
            if (clientName == null) return;
            clientName = clientName.trim();

            if ("__DASHBOARD__".equals(clientName)) {
                isDashboard = true;
                Server.addDashboard(out);
                while (in.readLine() != null) {}
                return;
            }

            Server.registerClient(this);
            System.out.println("[+] Client kết nối: " + clientName);

            while (true) {
                // Gửi danh sách phòng
                String roomList = String.join(",", Server.getRoomNames());
                sendMessage("ROOM_LIST:" + roomList);

                String choice = in.readLine();
                if (choice == null || choice.equalsIgnoreCase("/exit")) {
                    break;
                }
                choice = choice.trim();

                if (Server.getRoomNames().contains(choice)) {
                    Server.joinRoom(choice, this);

                    // Vòng lặp chat trong phòng
                    while (true) {
                        String msg = in.readLine();
                        if (msg == null) break;
                        msg = msg.trim();

                        if (msg.equalsIgnoreCase("/quit")) {
                            Server.leaveCurrentRoom(this);
                            break; // Thoát ra chọn lại phòng
                        } else if (msg.equalsIgnoreCase("/exit")) {
                            return;
                        } else {
                            Server.sendToRoom(currentRoom, clientName, msg);
                        }
                    }
                } else {
                    sendMessage("ERROR:Phòng không tồn tại!");
                }
            }
        } catch (IOException e) {
            // Ngắt kết nối
        } finally {
            if (isDashboard) {
                Server.removeDashboard(out);
            } else {
                Server.removeClient(this);
                System.out.println("[-] Client ngắt kết nối: " + clientName);
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}