
package com.lodge.controller;

import com.lodge.model.Bill;
import com.lodge.model.Booking;
import com.lodge.model.Guest;
import com.lodge.model.Room;
import com.lodge.model.RoomStatus;
import com.lodge.model.RoomType;
import com.lodge.model.User;
import com.lodge.service.AuthService;
import com.lodge.service.BookingService;
import com.lodge.service.GuestService;
import com.lodge.service.RoomService;
import com.lodge.util.FxImages;
import com.lodge.util.Money;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class CustomerController implements ViewController {

    @FXML private Label customerNameLabel;
    @FXML private FlowPane roomGallery;
    @FXML private FlowPane availabilitySummaryFlow;

    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;

    @FXML private ComboBox<Room> roomCombo;
    @FXML private Button bookRoomButton;

    @FXML private Label estimateLabel;
    @FXML private Label bookingMessageLabel;

    @FXML private TableView<Booking> bookingsTable;
    @FXML private TableColumn<Booking, String> colBookingId;
    @FXML private TableColumn<Booking, String> colRoom;
    @FXML private TableColumn<Booking, String> colCheckIn;
    @FXML private TableColumn<Booking, String> colCheckOut;
    @FXML private TableColumn<Booking, String> colStatus;

    @FXML
    public void initialize() {

        User customer = AuthService.getInstance().getCurrentUser();

        customerNameLabel.setText(
                customer == null ? "Welcome" :
                        "Welcome, " + customer.getFullName()
        );

        colBookingId.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().getBookingId())
        );

        colRoom.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().getRoomNumber() + " — "
                                + d.getValue().getRoomTypeName()
                )
        );

        colCheckIn.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().getCheckIn() == null ? "" :
                                d.getValue().getCheckIn().toString()
                )
        );

        colCheckOut.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().getCheckOut() == null ? "" :
                                d.getValue().getCheckOut().toString()
                )
        );

        colStatus.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().getStatusLabel())
        );

        checkInPicker.setValue(LocalDate.now());
        checkOutPicker.setValue(LocalDate.now().plusDays(1));

        checkInPicker.valueProperty().addListener(
                (obs, oldValue, newValue) -> refreshAvailability()
        );

        checkOutPicker.valueProperty().addListener(
                (obs, oldValue, newValue) -> refreshAvailability()
        );

        roomCombo.valueProperty().addListener(
                (obs, oldValue, newValue) -> updateEstimate()
        );

        setupRoomGallery();
        refreshAvailability();
        refreshBookings();
    }

    // ---------------------------------------------------------
    // ROOM GALLERY
    // ---------------------------------------------------------

    private void setupRoomGallery() {

        roomGallery.getChildren().clear();

        for (RoomType type : RoomType.values()) {

            VBox card = new VBox(7);
            card.getStyleClass().add("room-card");

            ImageView image = FxImages.box(
                    type.getImageResource(), 220, 130
            );

            Label name = new Label(type.getDisplayName());
            name.getStyleClass().add("room-card-title");

            Label description = new Label(type.getDescription());
            description.setWrapText(true);
            description.setMaxWidth(220);
            description.getStyleClass().add("room-card-features");

            Label rate = new Label(
                    Money.format(type.getRatePerNight()) + " / night"
            );
            rate.getStyleClass().add("room-card-rate");

            card.getChildren().addAll(
                    image, name, description, rate
            );

            roomGallery.getChildren().add(card);
        }
    }

    // ---------------------------------------------------------
    // DATE-SPECIFIC ROOM AVAILABILITY
    // ---------------------------------------------------------

    private List<Room> getAvailableRooms(
            LocalDate checkIn, LocalDate checkOut) {

        List<Room> available = new ArrayList<>();

        if (checkIn == null || checkOut == null
                || checkIn.isBefore(LocalDate.now())
                || !checkOut.isAfter(checkIn)) {
            return available;
        }

        BookingService bookingService = BookingService.getInstance();

        for (Room room : RoomService.getInstance().getRooms()) {

            if (room.getStatus() == RoomStatus.MAINTENANCE) {
                continue;
            }

            if (bookingService.isRoomFree(
                    room.getRoomNumber(),
                    checkIn,
                    checkOut,
                    null
            )) {
                available.add(room);
            }
        }

        return available;
    }

    private void refreshAvailability() {

        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();

        boolean validDates =
                checkIn != null
                        && checkOut != null
                        && !checkIn.isBefore(LocalDate.now())
                        && checkOut.isAfter(checkIn);

        if (validDates) {
            renderAvailabilitySummary(
                    checkIn, checkOut
            );
        } else {
            showDateSelectionMessage();
        }

        List<Room> available = validDates
                ? getAvailableRooms(checkIn, checkOut)
                : new ArrayList<>();

        Room previousSelection = roomCombo.getValue();

        roomCombo.getItems().setAll(available);

        if (previousSelection != null
                && available.contains(previousSelection)) {
            roomCombo.setValue(previousSelection);

        } else if (!available.isEmpty()) {
            roomCombo.setValue(available.get(0));

        } else {
            roomCombo.setValue(null);
        }

        bookRoomButton.setDisable(
                !validDates || available.isEmpty()
        );

        if (!validDates) {
            estimateLabel.setText(
                    "Choose valid check-in and check-out dates."
            );

        } else if (available.isEmpty()) {
            estimateLabel.setText(
                    "No rooms are available for those dates. "
                            + "Choose another date range."
            );

        } else {
            updateEstimate();
        }
    }

    // ---------------------------------------------------------
    // AVAILABILITY DASHBOARD
    // ---------------------------------------------------------

    private void renderAvailabilitySummary(
            LocalDate checkIn, LocalDate checkOut) {

        availabilitySummaryFlow.getChildren().clear();

        List<Room> allRooms =
                RoomService.getInstance().getRooms();

        List<Room> availableRooms =
                getAvailableRooms(checkIn, checkOut);

        for (RoomType type : RoomType.values()) {

            int total = 0;
            int available = 0;

            for (Room room : allRooms) {
                if (room.getType() == type) {
                    total++;
                }
            }

            for (Room room : availableRooms) {
                if (room.getType() == type) {
                    available++;
                }
            }

            int unavailable = total - available;

            double unavailablePercentage = total == 0
                    ? 0
                    : (double) unavailable / total * 100;

            String status;

            if (total == 0) {
                status = "No Rooms";
            } else if (available == 0) {
                status = "Fully Booked";
            } else if ((double) available / total < 0.30) {
                status = "Limited";
            } else {
                status = "Available";
            }

            VBox card = new VBox(9);
            card.setPrefWidth(225);
            card.setMinHeight(150);
            card.setStyle(
                    "-fx-background-color: white;"
                            + "-fx-background-radius: 10;"
                            + "-fx-border-color: #DCE3EA;"
                            + "-fx-border-radius: 10;"
                            + "-fx-padding: 15;"
            );

            Label name = new Label(type.getDisplayName());
            name.setStyle(
                    "-fx-font-size: 15px;"
                            + "-fx-font-weight: bold;"
                            + "-fx-text-fill: #263445;"
            );

            Label count = new Label(
                    available + " of " + total + " rooms available"
            );
            count.setStyle(
                    "-fx-font-size: 17px;"
                            + "-fx-font-weight: bold;"
                            + "-fx-text-fill: #167D65;"
            );

            ProgressBar progress = new ProgressBar(
                    total == 0 ? 0 : (double) available / total
            );
            progress.setMaxWidth(Double.MAX_VALUE);

            Label percentage = new Label(
                    String.format(
                            "%.0f%% unavailable for selected dates",
                            unavailablePercentage
                    )
            );
            percentage.setStyle(
                    "-fx-font-size: 11px;"
                            + "-fx-text-fill: #64748B;"
            );

            Label statusLabel = new Label(status);
            statusLabel.setStyle(
                    "-fx-font-weight: bold;"
                            + "-fx-text-fill: "
                            + getStatusColor(status) + ";"
            );

            Label rate = new Label(
                    Money.format(type.getRatePerNight()) + " / night"
            );
            rate.setStyle(
                    "-fx-text-fill: #475569;"
                            + "-fx-font-size: 12px;"
            );

            card.getChildren().addAll(
                    name,
                    count,
                    progress,
                    percentage,
                    statusLabel,
                    rate
            );

            availabilitySummaryFlow.getChildren().add(card);
        }
    }

    private String getStatusColor(String status) {

        switch (status) {
            case "Available":
                return "#15803D";

            case "Limited":
                return "#D97706";

            case "Fully Booked":
                return "#DC2626";

            default:
                return "#64748B";
        }
    }

    private void showDateSelectionMessage() {

        availabilitySummaryFlow.getChildren().clear();

        Label message = new Label(
                "Choose valid check-in and check-out dates "
                        + "to view room availability."
        );

        message.setWrapText(true);
        message.setStyle(
                "-fx-text-fill: #64748B;"
                        + "-fx-font-size: 13px;"
        );

        availabilitySummaryFlow.getChildren().add(message);
    }

    // ---------------------------------------------------------
    // BOOKING ESTIMATE
    // ---------------------------------------------------------

    private void updateEstimate() {

        Room room = roomCombo.getValue();
        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();

        if (room == null || checkIn == null || checkOut == null
                || !checkOut.isAfter(checkIn)) {

            if (room != null) {
                estimateLabel.setText(
                        "Choose a valid check-in and check-out date."
                );
            }

            return;
        }

        int nights = (int) ChronoUnit.DAYS.between(
                checkIn, checkOut
        );

        double charge = room.getEffectiveRate() * nights;

        double discount = nights >= Bill.LONG_STAY_MIN_NIGHTS
                ? charge * Bill.LONG_STAY_DISCOUNT_RATE
                : 0;

        double total = Money.round(
                (charge - discount) * (1 + Bill.VAT_RATE)
        );

        estimateLabel.setText(
                room + "  •  " + nights + " night(s)"
                        + "  •  estimated total "
                        + Money.format(total)
                        + " including VAT"
        );
    }

    // ---------------------------------------------------------
    // CUSTOMER BOOKINGS
    // ---------------------------------------------------------

    private void refreshBookings() {

        User customer = AuthService.getInstance().getCurrentUser();

        if (customer == null || customer.getGuestId() == null) {
            bookingsTable.getItems().clear();
            return;
        }

        List<Booking> ownBookings = new ArrayList<>();

        for (Booking booking :
                BookingService.getInstance().getBookings()) {

            if (customer.getGuestId().equals(
                    booking.getGuestId())) {
                ownBookings.add(booking);
            }
        }

        bookingsTable.getItems().setAll(ownBookings);
    }

    // ---------------------------------------------------------
    // RESERVATION
    // ---------------------------------------------------------

    @FXML
    private void handleReserve() {

        User customer = AuthService.getInstance().getCurrentUser();

        Room room = roomCombo.getValue();

        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();

        Guest guest = customer == null || customer.getGuestId() == null
                ? null
                : GuestService.getInstance().findById(
                customer.getGuestId()
        );

        if (guest == null) {
            showError(
                    "Your guest profile could not be found. "
                            + "Please contact the lodge."
            );
            return;
        }

        if (room == null || checkIn == null || checkOut == null
                || checkIn.isBefore(LocalDate.now())
                || !checkOut.isAfter(checkIn)) {

            showError(
                    "Choose a room and valid dates. "
                            + "Check-out must be after check-in."
            );
            return;
        }

        if (room.getStatus() == RoomStatus.MAINTENANCE
                || !BookingService.getInstance().isRoomFree(
                room.getRoomNumber(),
                checkIn,
                checkOut,
                null
        )) {

            showError(
                    "That room is no longer available. "
                            + "Please choose another."
            );

            refreshAvailability();
            return;
        }

        Booking booking =
                BookingService.getInstance().createBooking(
                        guest,
                        room,
                        checkIn,
                        checkOut,
                        "Customer self-service reservation",
                        customer.getUsername()
                );

        refreshAvailability();
        refreshBookings();

        bookingsTable.getSelectionModel().select(booking);

        bookingMessageLabel.setText(
                "Reservation " + booking.getBookingId()
                        + " confirmed. "
                        + "You can review it under Your reservations."
        );

        bookingMessageLabel.getStyleClass().setAll(
                "success-text"
        );
    }

    // ---------------------------------------------------------
    // SIGN OUT
    // ---------------------------------------------------------

    @FXML
    private void handleSignOut() {

        AuthService.getInstance().logout();

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/lodge/view/login.fxml"
                    )
            );

            Parent root = loader.load();

            Stage stage = (Stage) customerNameLabel
                    .getScene().getWindow();

            Scene scene = new Scene(root, 1080, 720);

            scene.getStylesheets().add(
                    getClass().getResource(
                            "/com/lodge/css/style.css"
                    ).toExternalForm()
            );

            stage.setScene(scene);
            stage.setTitle(
                    "Sign In — SkyRidge Lodge Billing System"
            );
            stage.centerOnScreen();

        } catch (IOException e) {

            bookingMessageLabel.setText(
                    "Could not return to sign in: " + e.getMessage()
            );

            bookingMessageLabel.getStyleClass().setAll(
                    "error-text"
            );
        }
    }

    private void showError(String message) {

        bookingMessageLabel.setText(message);

        bookingMessageLabel.getStyleClass().setAll(
                "error-text"
        );
    }

    @Override
    public void onShown() {
        refreshAvailability();
        refreshBookings();
    }
}
