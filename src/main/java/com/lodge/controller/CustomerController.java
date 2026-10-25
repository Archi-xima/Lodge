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
        customerNameLabel.setText(customer == null ? "Welcome" : "Welcome, " + customer.getFullName());

        colBookingId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookingId()));
        colRoom.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getRoomNumber() + " — " + d.getValue().getRoomTypeName()));
        colCheckIn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCheckIn() == null ? "" : d.getValue().getCheckIn().toString()));
        colCheckOut.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCheckOut() == null ? "" : d.getValue().getCheckOut().toString()));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatusLabel()));

        checkInPicker.setValue(LocalDate.now());
        checkOutPicker.setValue(LocalDate.now().plusDays(1));
        checkInPicker.valueProperty().addListener((obs, oldValue, newValue) -> refreshAvailability());
        checkOutPicker.valueProperty().addListener((obs, oldValue, newValue) -> refreshAvailability());
        roomCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateEstimate());

        setupRoomGallery();
        refreshAvailability();
        refreshBookings();
    }

    private void setupRoomGallery() {
        roomGallery.getChildren().clear();
        for (RoomType type : RoomType.values()) {
            VBox card = new VBox(7);
            card.getStyleClass().add("room-card");
            ImageView image = FxImages.box(type.getImageResource(), 220, 130);
            Label name = new Label(type.getDisplayName());
            name.getStyleClass().add("room-card-title");
            Label description = new Label(type.getDescription());
            description.setWrapText(true);
            description.setMaxWidth(220);
            description.getStyleClass().add("room-card-features");
            Label rate = new Label(Money.format(type.getRatePerNight()) + " / night");
            rate.getStyleClass().add("room-card-rate");
            card.getChildren().addAll(image, name, description, rate);
            roomGallery.getChildren().add(card);
        }
    }

    private void refreshAvailability() {
        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();
        List<Room> available = new ArrayList<>();
        if (checkIn != null && checkOut != null && !checkIn.isBefore(LocalDate.now())
                && checkOut.isAfter(checkIn)) {
            BookingService bookings = BookingService.getInstance();
            for (Room room : RoomService.getInstance().getRooms()) {
                if (room.getStatus() != RoomStatus.MAINTENANCE
                        && bookings.isRoomFree(room.getRoomNumber(), checkIn, checkOut, null)) {
                    available.add(room);
                }
            }
        }

        Room previousSelection = roomCombo.getValue();
        roomCombo.getItems().setAll(available);
        if (previousSelection != null && available.contains(previousSelection)) {
            roomCombo.setValue(previousSelection);
        } else if (!available.isEmpty()) {
            roomCombo.setValue(available.get(0));
        } else {
            roomCombo.setValue(null);
        }
        bookRoomButton.setDisable(available.isEmpty());
        if (available.isEmpty()) {
            estimateLabel.setText("No rooms are available for those dates. Choose another date range.");
        } else {
            updateEstimate();
        }
    }

    private void updateEstimate() {
        Room room = roomCombo.getValue();
        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();
        if (room == null || checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            if (room != null) {
                estimateLabel.setText("Choose a valid check-in and check-out date.");
            }
            return;
        }
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        double charge = room.getEffectiveRate() * nights;
        double discount = nights >= Bill.LONG_STAY_MIN_NIGHTS
                ? charge * Bill.LONG_STAY_DISCOUNT_RATE : 0;
        double total = Money.round((charge - discount) * (1 + Bill.VAT_RATE));
        estimateLabel.setText(room + "  •  " + nights + " night(s)  •  estimated total "
                + Money.format(total) + " including VAT");
    }

    private void refreshBookings() {
        User customer = AuthService.getInstance().getCurrentUser();
        if (customer == null || customer.getGuestId() == null) {
            bookingsTable.getItems().clear();
            return;
        }
        List<Booking> ownBookings = new ArrayList<>();
        for (Booking booking : BookingService.getInstance().getBookings()) {
            if (customer.getGuestId().equals(booking.getGuestId())) {
                ownBookings.add(booking);
            }
        }
        bookingsTable.getItems().setAll(ownBookings);
    }

    @FXML
    private void handleReserve() {
        User customer = AuthService.getInstance().getCurrentUser();
        Room room = roomCombo.getValue();
        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();
        Guest guest = customer == null || customer.getGuestId() == null
                ? null : GuestService.getInstance().findById(customer.getGuestId());
        if (guest == null) {
            showError("Your guest profile could not be found. Please contact the lodge.");
            return;
        }
        if (room == null || checkIn == null || checkOut == null
                || checkIn.isBefore(LocalDate.now()) || !checkOut.isAfter(checkIn)) {
            showError("Choose a room and valid dates. Check-out must be after check-in.");
            return;
        }
        if (!BookingService.getInstance().isRoomFree(room.getRoomNumber(), checkIn, checkOut, null)) {
            showError("That room has just been reserved for those dates. Please choose another.");
            refreshAvailability();
            return;
        }

        Booking booking = BookingService.getInstance().createBooking(guest, room, checkIn, checkOut,
                "Customer self-service reservation", customer.getUsername());
        refreshAvailability();
        refreshBookings();
        bookingsTable.getSelectionModel().select(booking);
        bookingMessageLabel.setText("Reservation " + booking.getBookingId() + " confirmed. "
                + "You can review it under Your reservations.");
        bookingMessageLabel.getStyleClass().setAll("success-text");
    }

    @FXML
    private void handleSignOut() {
        AuthService.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lodge/view/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) customerNameLabel.getScene().getWindow();
            Scene scene = new Scene(root, 1080, 720);
            scene.getStylesheets().add(getClass().getResource("/com/lodge/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Sign In — SkyRidge Lodge Billing System");
            stage.centerOnScreen();
        } catch (IOException e) {
            bookingMessageLabel.setText("Could not return to sign in: " + e.getMessage());
            bookingMessageLabel.getStyleClass().setAll("error-text");
        }
    }

    private void showError(String message) {
        bookingMessageLabel.setText(message);
        bookingMessageLabel.getStyleClass().setAll("error-text");
    }

    @Override
    public void onShown() {
        refreshAvailability();
        refreshBookings();
    }
}
