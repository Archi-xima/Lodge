package com.lodge.controller;

import com.lodge.model.*;
import com.lodge.service.AuthService;
import com.lodge.service.BookingService;
import com.lodge.service.BillingService;
import com.lodge.service.GuestService;
import com.lodge.service.RoomService;
import com.lodge.util.Money;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reservations: create a booking, check guests in and out, cancel.
 * Check-out raises the invoice automatically.
 */
public class BookingsController implements ViewController {

    @FXML private ComboBox<Guest> guestCombo;
    @FXML private ComboBox<Room> roomCombo;
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private TextField notesField;
    @FXML private Label nightsLabel;
    @FXML private Label estimateLabel;
    @FXML private Button createBookingBtn;

    @FXML private TableView<Booking> bookingsTable;
    @FXML private TableColumn<Booking, String> colBookingId;
    @FXML private TableColumn<Booking, String> colGuest;
    @FXML private TableColumn<Booking, String> colRoom;
    @FXML private TableColumn<Booking, String> colCheckIn;
    @FXML private TableColumn<Booking, String> colCheckOut;
    @FXML private TableColumn<Booking, String> colNights;
    @FXML private TableColumn<Booking, String> colStatus;

    @FXML private Button checkInBtn;
    @FXML private Button checkOutBtn;
    @FXML private Button cancelBookingBtn;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        colBookingId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookingId()));
        colGuest.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getGuestName()));
        colRoom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRoomNumber() + " (" + d.getValue().getRoomTypeName() + ")"));
        colCheckIn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCheckIn() == null ? "" : d.getValue().getCheckIn().toString()));
        colCheckOut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCheckOut() == null ? "" : d.getValue().getCheckOut().toString()));
        colNights.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getNights())));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatusLabel()));

        guestCombo.getItems().setAll(GuestService.getInstance().getGuests());
        checkInPicker.setValue(LocalDate.now());
        checkOutPicker.setValue(LocalDate.now().plusDays(1));

        checkInPicker.valueProperty().addListener((obs, o, n) -> updateEstimate());
        checkOutPicker.valueProperty().addListener((obs, o, n) -> updateEstimate());
        roomCombo.valueProperty().addListener((obs, o, n) -> updateEstimate());

        bookingsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> updateButtons(n));

        checkInBtn.setDisable(true);
        checkOutBtn.setDisable(true);
        cancelBookingBtn.setDisable(true);
    }

    @Override
    public void onShown() {
        guestCombo.getItems().setAll(GuestService.getInstance().getGuests());
        refreshRoomCombo();
        refreshTable();
    }

    private void refreshRoomCombo() {
        Room selected = roomCombo.getValue();
        List<Room> sellable = RoomService.getInstance().sellableRooms();
        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();

        List<Room> free = new ArrayList<>();
        for (Room room : sellable) {
            if (in == null || out == null
                    || BookingService.getInstance().isRoomFree(room.getRoomNumber(), in, out, null)) {
                free.add(room);
            }
        }
        roomCombo.getItems().setAll(free);
        if (selected != null && free.contains(selected)) {
            roomCombo.setValue(selected);
        } else if (!free.isEmpty()) {
            roomCombo.setValue(free.get(0));
        } else {
            roomCombo.setValue(null);
        }
        updateEstimate();
    }

    private void updateEstimate() {
        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();
        int nights = 1;
        if (in != null && out != null && out.isAfter(in)) {
            nights = (int) java.time.temporal.ChronoUnit.DAYS.between(in, out);
        }
        nightsLabel.setText(nights + " night(s)");

        Room room = roomCombo.getValue();
        if (room == null) {
            estimateLabel.setText("Select a free room to see the estimate.");
            return;
        }
        double charge = room.getEffectiveRate() * nights;
        double discount = nights >= Bill.LONG_STAY_MIN_NIGHTS ? charge * Bill.LONG_STAY_DISCOUNT_RATE : 0;
        double subtotal = charge - discount;
        double total = subtotal * (1 + Bill.VAT_RATE);
        estimateLabel.setText(String.format("%s / night  →  %s charge, less %s discount  =  %s incl. 16%% VAT",
                room.getFormattedRate(), Money.format(charge), Money.format(discount), Money.format(Money.round(total))));
    }

    private void refreshTable() {
        Booking selected = bookingsTable.getSelectionModel().getSelectedItem();
        String keepId = selected == null ? null : selected.getBookingId();
        bookingsTable.getItems().setAll(BookingService.getInstance().getBookings());
        if (keepId != null) {
            for (Booking booking : bookingsTable.getItems()) {
                if (keepId.equals(booking.getBookingId())) {
                    bookingsTable.getSelectionModel().select(booking);
                    break;
                }
            }
        }
        updateButtons(bookingsTable.getSelectionModel().getSelectedItem());
    }

    private void updateButtons(Booking booking) {
        boolean hasSelection = booking != null;
        checkInBtn.setDisable(!hasSelection || booking.getStatus() != BookingStatus.CONFIRMED);
        checkOutBtn.setDisable(!hasSelection || booking.getStatus() != BookingStatus.CHECKED_IN);
        cancelBookingBtn.setDisable(!hasSelection || booking.getStatus() == BookingStatus.CHECKED_OUT
                || booking.getStatus() == BookingStatus.CANCELLED);
    }

    @FXML
    private void handleDateChanged() {
        refreshRoomCombo();
    }

    @FXML
    private void handleCreateBooking() {
        Guest guest = guestCombo.getValue();
        Room room = roomCombo.getValue();
        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();

        if (guest == null) {
            warn("Please choose a guest (create one in the Guests module first).");
            return;
        }
        if (room == null) {
            warn("Please choose a free room.");
            return;
        }
        if (in == null || out == null || !out.isAfter(in)) {
            warn("Check-out must be after check-in.");
            return;
        }
        if (!BookingService.getInstance().isRoomFree(room.getRoomNumber(), in, out, null)) {
            warn("Room " + room.getRoomNumber() + " is not free for those dates.");
            refreshRoomCombo();
            return;
        }

        Booking booking = BookingService.getInstance().createBooking(guest, room, in, out,
                notesField.getText(), AuthService.getInstance().currentUserName());
        notesField.clear();
        refreshRoomCombo();
        refreshTable();
        bookingsTable.getSelectionModel().select(booking);
        setStatus("Created " + booking.getBookingId() + " for " + guest.getFullName()
                + " in room " + room.getRoomNumber() + ".", true);
    }

    @FXML
    private void handleCheckIn() {
        Booking booking = bookingsTable.getSelectionModel().getSelectedItem();
        if (booking == null) {
            return;
        }
        BookingService.getInstance().checkIn(booking);
        refreshRoomCombo();
        refreshTable();
        setStatus("Checked in " + booking.getGuestName() + " → room " + booking.getRoomNumber() + ".", true);
    }

    @FXML
    private void handleCheckOut() {
        Booking booking = bookingsTable.getSelectionModel().getSelectedItem();
        if (booking == null) {
            return;
        }
        TextInputDialog extrasDialog = new TextInputDialog("0");
        extrasDialog.setTitle("Check-out");
        extrasDialog.setHeaderText("Check out " + booking.getGuestName());
        extrasDialog.setContentText("Extras charged during the stay (K):");
        String extrasText = extrasDialog.showAndWait().orElse("0");
        double extras = 0;
        try {
            extras = com.lodge.util.Money.parse(extrasText);
        } catch (NumberFormatException ignored) {
            extras = 0;
        }

        Bill bill = BookingService.getInstance().checkOut(booking, extras);
        refreshRoomCombo();
        refreshTable();
        setStatus("Checked out. Bill " + bill.getBillId() + " raised — total "
                + bill.getFormattedTotal() + ", balance " + bill.getFormattedBalance() + ".", true);

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Guest checked out");
        info.setHeaderText("Bill " + bill.getBillId() + " created for " + bill.getGuestName());
        info.setContentText("Total: " + bill.getFormattedTotal() + "\n"
                + "Balance due: " + bill.getFormattedBalance() + "\n\n"
                + "Open the Billing module to take payment and print the receipt.");
        info.showAndWait();

        MainController shell = MainController.getActive();
        if (shell != null) {
            shell.navigateTo("billing");
        }
    }

    @FXML
    private void handleCancelBooking() {
        Booking booking = bookingsTable.getSelectionModel().getSelectedItem();
        if (booking == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel booking");
        confirm.setHeaderText("Cancel " + booking.getBookingId() + " for " + booking.getGuestName() + "?");
        confirm.setContentText("The room will become available again.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            BookingService.getInstance().cancel(booking);
            refreshRoomCombo();
            refreshTable();
            setStatus("Booking " + booking.getBookingId() + " cancelled.", false);
        }
    }

    @FXML
    private void handleNewGuestShortcut() {
        MainController shell = MainController.getActive();
        if (shell != null) {
            shell.navigateTo("guests");
        }
    }

    private void setStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().setAll(success ? "success-text" : "error-text");
    }

    private void warn(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Reservations");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
