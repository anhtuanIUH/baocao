import java.io.*;
import java.net.*;

public class Dashboard {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 8888;

    public static void main(String[] args) {
        System.out.println("Đang kết nối tới Dashboard Server...");
        try (Socket socket = new Socket(SERVER_IP, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)) {

            // Gửi định danh Dashboard
            out.println("__DASHBOARD__");

            String line;
            while ((line = in.readLine()) != null) {
                // Xóa màn hình đơn giản bằng cách in nhiều dòng trống
                System.out.print("\033[H\033[2J");
                System.out.flush();

                System.out.println("==================================================");
                System.out.println("          DASHBOARD QUẢN LÝ PHÒNG CHAT TCP         ");
                System.out.println("==================================================");
                
                // Phân tích cú pháp JSON thủ công hoặc in trực tiếp chuỗi JSON nhận được
                System.out.println("Dữ liệu thô từ Server:\n" + line);
                System.out.println("--------------------------------------------------");
                System.out.println("Đang cập nhật real-time... (Nhấn Ctrl+C để thoát)");
            }

        } catch (IOException e) {
            System.out.println("Không thể kết nối dashboard tới server: " + e.getMessage());
        }
    }
}