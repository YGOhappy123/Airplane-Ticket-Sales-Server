package com.ygohappy123.server.threads;

import com.ygohappy123.server.AppConfig;
import com.ygohappy123.server.SqlServerConnector;
import com.ygohappy123.server.controllers.MainController;
import org.json.JSONArray;
import org.json.JSONObject;
import javafx.application.Platform;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.ResultSet;

public class ReadServer extends Thread {
    private Socket socket;
    private SqlServerConnector connector;
    private final MainController controller;

    public ReadServer(Socket socket, MainController controller) {
        this.socket = socket;
        this.controller = controller;
    }

    @Override
    public void run() {
        DataInputStream inputStream = null;

        connector = new SqlServerConnector(AppConfig.getJdbcUrl(), AppConfig.getSqlDriver());
        try {
            inputStream = new DataInputStream(socket.getInputStream());

            while (true) {
                String message = inputStream.readUTF();
                JSONObject request = new JSONObject(message);
                String action = request.getString("action");

                switch (action) {
                    case "HOLD_SEAT":
                        handleHoldSeat(request);
                        break;
                    case "BOOK_SEAT":
                        handleBookSeat(request);
                        break;
                    case "RETURN_SEAT":
                        handleReturnSeat(request);
                        break;
                    case "RELEASE_SEAT":
                        handleReleaseSeat(request);
                        break;
                    case "FETCH_SEATS":
                        handleFetchSeats();
                        break;
                    default:
                        System.out.println("Unknown action received");
                }
            }
        } catch (Exception e) {
            System.out.println(socket + " disconnected to the server");
            Platform.runLater(() -> controller.addNotification("Khách hàng " + socket.getInetAddress() + " đã ngắt kết nối đến server"));

            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                socket.close();
            } catch (IOException ex) {
                System.out.println(socket + " disconnected to the server");
            }
        }
    }

    private void handleHoldSeat(JSONObject request) {
        int seatId = request.getInt("seatId");
        String phoneNumber = request.getString("phoneNumber");

        try {
            int result = connector.executeUpdate(String.format("UPDATE dbo.Seats " +
                    "SET Status = 'HELD', HoldExpiresAt = DATEADD(SECOND, 30, GETDATE()), HeldByPhone = '%s' " +
                    "WHERE SeatId = %d AND Status = 'AVAILABLE';", phoneNumber, seatId));

            JSONObject response = new JSONObject();
            if (result == 1) {
                response.put("action", "HOLD_SEAT_SUCCESS");
                response.put("message", "Ghế đã được chọn thành công và được giữ cho bạn trong 30 giây. Sau 30 giây ghế sẽ được mở khóa.");

                Platform.runLater(() -> controller.addNotification("Khách hàng " + socket.getInetAddress() + " đã chọn ghế số " + seatId));
            } else {
                response.put("action", "UPDATE_FAILED");
                response.put("message", "Ghế đã được chọn hoặc đặt bởi khách hàng khác.");
            }
            sendToClient(response.toString());
        } catch (Exception ex) {
            System.out.println("Failed to update seat.");
        }
    }

    private void handleBookSeat(JSONObject request) {
        int seatId = request.getInt("seatId");
        String phoneNumber = request.getString("phoneNumber");

        try {
            int result = connector.executeUpdate(String.format("UPDATE dbo.Seats " +
                    "SET Status = 'BOOKED', HoldExpiresAt = NULL " +
                    "WHERE SeatId = %d AND Status = 'HELD' AND HeldByPhone = '%s' AND HoldExpiresAt > GETDATE();", seatId, phoneNumber));

            JSONObject response = new JSONObject();
            if (result == 1) {
                response.put("action", "BOOK_SEAT_SUCCESS");
                response.put("message", "Ghế đã được mua thành công.");

                Platform.runLater(() -> controller.addNotification("Khách hàng " + socket.getInetAddress() + " đã mua ghế số " + seatId));
            } else {
                response.put("action", "UPDATE_FAILED");
                response.put("message", "Ghế đã được chọn bởi khách hàng khác hoặc hết thời gian chờ 30 giây.");
            }
            sendToClient(response.toString());
        } catch (Exception ex) {
            System.out.println("Failed to update seat.");
        }
    }

    private void handleReturnSeat(JSONObject request) {
        int seatId = request.getInt("seatId");
        String phoneNumber = request.getString("phoneNumber");

        try {
            int result = connector.executeUpdate(String.format("UPDATE dbo.Seats " +
                    "SET Status = 'AVAILABLE', HoldExpiresAt = NULL, HeldByPhone = NULL " +
                    "WHERE SeatId = %d AND Status = 'BOOKED' AND HeldByPhone = '%s';", seatId, phoneNumber));

            JSONObject response = new JSONObject();
            if (result == 1) {
                response.put("action", "RETURN_SEAT_SUCCESS");
                response.put("message", "Ghế đã hủy thành công.");

                Platform.runLater(() -> controller.addNotification("Khách hàng " + socket.getInetAddress() + " đã hủy chọn ghế số " + seatId));
            } else {
                response.put("action", "UPDATE_FAILED");
                response.put("message", "Ghế đã được chọn bởi khách hàng.");
            }
            sendToClient(response.toString());
        } catch (Exception ex) {
            System.out.println("Failed to update seat.");
        }
    }

    private void handleReleaseSeat(JSONObject request) {
        int seatId = request.getInt("seatId");
        String phoneNumber = request.getString("phoneNumber");

        try {
            int result = connector.executeUpdate(String.format("UPDATE dbo.Seats " +
                    "SET Status = 'AVAILABLE', HoldExpiresAt = NULL, HeldByPhone = NULL " +
                    "WHERE SeatId = %d AND Status = 'HELD' AND HeldByPhone = '%s';", seatId, phoneNumber));

            JSONObject response = new JSONObject();
            if (result == 1) {
                response.put("action", "RELEASE_SEAT_SUCCESS");
                response.put("message", "Ghế đã hủy chọn thành công.");

                Platform.runLater(() -> controller.addNotification("Khách hàng " + socket.getInetAddress() + " đã hủy chọn ghế số " + seatId));
            } else {
                response.put("action", "UPDATE_FAILED");
                response.put("message", "Đã hết thời gian chờ 30 giây.");
            }
            sendToClient(response.toString());
        } catch (Exception ex) {
            System.out.println("Failed to update seat.");
        }
    }

    private void handleFetchSeats() {
        try {
            ResultSet result = connector.loadData("SELECT * FROM dbo.Seats;");
            JSONArray seats = new JSONArray();

            while (result.next()) {
                JSONObject seat = new JSONObject();
                seat.put("seatId", result.getInt("SeatId"));
                seat.put("seatNumber", result.getString("SeatNumber"));
                seat.put("status", result.getString("Status"));
                seat.put("heldBy", result.getString("HeldByPhone"));
                seats.put(seat);
            }

            JSONObject response = new JSONObject();
            response.put("action", "SEAT_LIST");
            response.put("seats", seats);

            sendToClient(response.toString());
        } catch (Exception ex) {
            System.out.println("Failed to load seats data.");
        }
    }

    private void sendToClient(String message) throws IOException {
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        outputStream.writeUTF(message);
    }
}
