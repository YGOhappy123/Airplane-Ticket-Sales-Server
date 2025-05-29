package com.ygohappy123.server.controllers;

import com.ygohappy123.server.AppConfig;
import com.ygohappy123.server.SqlServerConnector;
import com.ygohappy123.server.models.PlaneSeat;
import com.ygohappy123.server.threads.ReadServer;
import com.ygohappy123.server.threads.WriteServer;
import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialogBuilder;
import io.github.palexdev.materialfx.dialogs.MFXStageDialog;
import io.github.palexdev.materialfx.enums.ScrimPriority;
import io.github.palexdev.materialfx.utils.ToggleButtonsUtil;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.sql.ResultSet;
import java.util.*;

public class MainController implements Initializable {
    private final int serverPort = 6543;
    private final Stage stage;
    private double xOffset;
    private double yOffset;
    private final ToggleGroup toggleGroup;
    private MFXGenericDialog dialogContent;
    private MFXStageDialog dialog;
    private SqlServerConnector connector;
    private List<Socket> clientList = new ArrayList<>();
    private List<PlaneSeat> seatList = new ArrayList<>();
    private WriteServer writeServer;
    private ServerSocket socketServer;

    @FXML
    private HBox windowHeader;

    @FXML
    private MFXFontIcon closeIcon;

    @FXML
    private MFXFontIcon minimizeIcon;

    @FXML
    private MFXFontIcon alwaysOnTopIcon;

    @FXML
    private AnchorPane rootPane;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private VBox messageBox;

    @FXML
    private MFXTableView<PlaneSeat> seatsTable;

    public MainController(Stage stage) {
        this.stage = stage;
        this.toggleGroup = new ToggleGroup();
        ToggleButtonsUtil.addAlwaysOneSelectedSupport(toggleGroup);
    }

    public List<Socket> getClientList() {
        return this.clientList;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        closeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            try {
                for (Socket client : clientList) {
                    client.close();
                }
                if (socketServer != null && !socketServer.isClosed()) socketServer.close();
                if (writeServer != null) writeServer.interrupt();

                Platform.exit();
                System.exit(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        minimizeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED,
                event -> ((Stage) rootPane.getScene().getWindow()).setIconified(true));
        alwaysOnTopIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            boolean newVal = !stage.isAlwaysOnTop();
            alwaysOnTopIcon.pseudoClassStateChanged(PseudoClass.getPseudoClass("always-on-top"), newVal);
            stage.setAlwaysOnTop(newVal);
        });

        windowHeader.setOnMousePressed(event -> {
            xOffset = stage.getX() - event.getScreenX();
            yOffset = stage.getY() - event.getScreenY();
        });
        windowHeader.setOnMouseDragged(event -> {
            stage.setOpacity(0.7);
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
        });
        windowHeader.setOnMouseReleased(event -> {
            stage.setOpacity(1.0);
        });

        messageBox.getChildren().clear();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        seatsTable.autosizeColumnsOnInitialization();
        connector = new SqlServerConnector(AppConfig.getJdbcUrl(), AppConfig.getSqlDriver());

        Platform.runLater(() -> {
            setupDialog();
            setupTable();
            startIntervalTimer();

            try {
                startSocketServer();
            } catch (IOException e) {
                showErrorDialog("Lỗi khởi động server", "Không thể khởi động server: " + e.getMessage());
            }
        });
    }

    private void setupTable() {
        MFXTableColumn<PlaneSeat> idColumn = new MFXTableColumn<>("Mã ID Vé", false, Comparator.comparing(s -> s.getSeatId()));
        idColumn.setRowCellFactory(invoice -> new MFXTableRowCell<>(s -> s.getSeatId()));
        idColumn.prefWidthProperty().bind(seatsTable.widthProperty().multiply(0.15));

        MFXTableColumn<PlaneSeat> numberColumn = new MFXTableColumn<>("Số Ghế", false, Comparator.comparing(s -> s.getSeatNumber()));
        numberColumn.setRowCellFactory(invoice -> new MFXTableRowCell<>(s -> s.getSeatNumber()));
        numberColumn.prefWidthProperty().bind(seatsTable.widthProperty().multiply(0.15));

        MFXTableColumn<PlaneSeat> statusColumn = new MFXTableColumn<>("Trạng Thái", false, Comparator.comparing(s -> s.getStatus()));
        statusColumn.setRowCellFactory(invoice -> new MFXTableRowCell<>(s -> s.getMappedStatus()));
        statusColumn.prefWidthProperty().bind(seatsTable.widthProperty().multiply(0.2));

        MFXTableColumn<PlaneSeat> ownerColumn = new MFXTableColumn<>("SĐT Mua / Chọn", false, Comparator.comparing(s -> s.getHeldBy()));
        ownerColumn.setRowCellFactory(invoice -> new MFXTableRowCell<>(s -> s.getHeldBy()));
        ownerColumn.prefWidthProperty().bind(seatsTable.widthProperty().multiply(0.25));

        MFXTableColumn<PlaneSeat> expirationColumn = new MFXTableColumn<>("Thời Gian Mở Khóa", false, Comparator.comparing(s -> s.getHoldExpiresAt()));
        expirationColumn.setRowCellFactory(invoice -> new MFXTableRowCell<>(s -> s.getFormattedHoldExpiresAt()));
        expirationColumn.prefWidthProperty().bind(seatsTable.widthProperty().multiply(0.25));

        seatsTable.getTableColumns().addAll(idColumn, numberColumn, statusColumn, ownerColumn, expirationColumn);
        seatsTable.setFooterVisible(false);
    }

    private void setupDialog() {
        dialogContent = MFXGenericDialogBuilder.build()
                .makeScrollable(true)
                .setShowMinimize(false)
                .setShowAlwaysOnTop(false)
                .get();

        dialog = MFXGenericDialogBuilder.build(dialogContent)
                .toStageDialogBuilder()
                .initOwner(stage)
                .initModality(Modality.APPLICATION_MODAL)
                .setDraggable(false)
                .setOwnerNode((Pane) stage.getScene().getRoot())
                .setScrimPriority(ScrimPriority.NODE)
                .setScrimOwner(true)
                .get();

        dialogContent.addActions(
                Map.entry(new MFXButton("Xác nhận"), event -> dialog.close())
        );
        dialogContent.setMaxSize(stage.getMaxWidth(), stage.getMaxHeight());
    }

    public void showErrorDialog(String headerText, String contentText) {
        MFXFontIcon warnIcon = new MFXFontIcon("fas-circle-xmark", 18);
        dialogContent.setHeaderIcon(warnIcon);
        dialogContent.setHeaderText(headerText);
        dialogContent.setContentText(contentText);
        dialogContent.getStyleClass().removeIf(
                s -> s.equals("mfx-info-dialog") || s.equals("mfx-warn-dialog")
        );
        dialogContent.getStyleClass().add("mfx-error-dialog");
        dialog.show();
    }

    public void showSuccessDialog(String headerText, String contentText) {
        MFXFontIcon warnIcon = new MFXFontIcon("fas-circle-info", 18);
        dialogContent.setHeaderIcon(warnIcon);
        dialogContent.setHeaderText(headerText);
        dialogContent.setContentText(contentText);
        dialogContent.getStyleClass().removeIf(
                s -> s.equals("mfx-warn-dialog") || s.equals("mfx-error-dialog")
        );
        dialogContent.getStyleClass().add("mfx-info-dialog");
        dialog.show();
    }

    public void addNotification(String message) {
        Label notification = new Label(message);
        notification.getStyleClass().add("notifyMessage");
        notification.setWrapText(true);
        notification.setPrefWidth(346.0);

        messageBox.getChildren().add(notification);
        scrollPane.layout();
        scrollPane.setVvalue(1.0);
    }

    private void releaseExpiredHolds() {
        connector.executeUpdate("UPDATE dbo.Seats " +
                "SET Status = 'AVAILABLE', HoldExpiresAt = NULL, HeldByPhone = NULL " +
                "WHERE Status = 'HELD' AND HoldExpiresAt <= GETDATE();");
    }

    private void fillTicketTable() {
        try {
            releaseExpiredHolds();
            ResultSet result = connector.loadData("SELECT * FROM dbo.Seats;");
            seatList.clear();
            while (result.next()) {
                PlaneSeat seat = new PlaneSeat(
                        result.getInt(1),
                        result.getString(2),
                        result.getString(3),
                        result.getString(4),
                        result.getString(5)
                );
                seatList.add(seat);
            }
            seatsTable.setItems(FXCollections.observableArrayList(seatList));
        } catch (Exception ex) {
            System.out.println("Failed to load ticket data.");
            showErrorDialog("Không thể tải dữ liệu", "Tải dữ liệu từ database thất bại. Vui lòng thử lại sau.");
        }
    }

    private void startSocketServer() throws IOException {
        new Thread(() -> {
            try {
                socketServer = new ServerSocket(this.serverPort);
                Platform.runLater(() -> showSuccessDialog("Server đã khởi động", "Server đã khởi động thành công tại cổng: " + serverPort + ". Sẵn sàng nhận yêu cầu từ khách hàng."));
                System.out.println("Server started.");

                writeServer = new WriteServer(this);
                writeServer.start();

                while (true) {
                    Socket socket = socketServer.accept();
                    Platform.runLater(() -> addNotification("Server đã kết nối với khách hàng " + socket.getInetAddress()));
                    System.out.println("Server connected with client " + socket + ".");

                    this.clientList.add(socket);
                    ReadServer readServer = new ReadServer(socket, this);
                    readServer.start();
                }
            } catch (IOException e) {
                Platform.runLater(() -> showErrorDialog("Lỗi khởi động server", "Không thể khởi động server: " + e.getMessage()));
            }
        }).start();
    }

    private void startIntervalTimer() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(1000), e -> fillTicketTable()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
}