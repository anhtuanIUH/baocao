import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 8888;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.print("Nhập tên của bạn: ");
            String name = scanner.nextLine().trim();
            if (name.isEmpty()) name = "User_" + System.currentTimeMillis() % 1000;
            out.println(name);

            // Thread lắng nghe tin nhắn từ Server
            Thread listenerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("ROOM_LIST:")) {
                            String rooms = line.split(":", 2)[1];
                            System.out.println("\n--- DANH SÁCH PHÒNG ---");
                            System.out.println("Các phòng hiện có: " + rooms);
                            System.out.print("Nhập tên phòng muốn vào (hoặc /exit): ");
                        } else if (line.startsWith("JOINED:")) {
                            String room = line.split(":", 2)[1];
                            System.out.println("\n[+] Đã vào phòng [" + room + "]. Bắt đầu chat (gõ /quit để về danh sách phòng, /exit để thoát):");
                            System.out.print("Bạn: ");
                        } else if (line.equals("LEFT_ROOM")) {
                            System.out.println("\n[+] Đã rời phòng thành công.");
                        } else if (line.startsWith("ERROR:")) {
                            System.out.println("\n[THÔNG BÁO] " + line.split(":", 2)[1]);
                            System.out.print("Nhập tên phòng muốn vào: ");
                        } else {
                            System.out.println("\r" + line);
                            System.out.print("Bạn: ");
                        }
                    }
                } catch (IOException e) {
                    System.out.println("\n[-] Đã mất kết nối tới server.");
                }
            });
            listenerThread.start();

            // Vòng lặp gửi tin nhắn từ bàn phím
            while (scanner.hasNextLine()) {
                String msg = scanner.nextLine();
                out.println(msg);
                if (msg.equalsIgnoreCase("/exit")) {
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("Không thể kết nối đến server: " + e.getMessage());
        }
    }
}