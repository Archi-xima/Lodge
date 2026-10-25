package com.lodge.controller;

import com.lodge.model.Bill;
import com.lodge.model.Booking;
import com.lodge.service.AuthService;
import com.lodge.service.BookingService;
import com.lodge.service.BillingService;
import com.lodge.service.ReportService;
import com.lodge.util.FxImages;
import com.lodge.util.Money;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Home screen: live occupancy, money at stake, today's arrivals and departures.
 */
public class DashboardController implements ViewController {

    @FXML private Label welcomeLabel;
    @FXML private Label occupancyValueLabel;
    @FXML private Label occupiedValueLabel;
    @FXML private Label availableValueLabel;
    @FXML private Label revenueValueLabel;
    @FXML private Label outstandingValueLabel;

    @FXML private TableView<Booking> arrivalsTable;
    @FXML private TableColumn<Booking, String> colArrivalId;
    @FXML private TableColumn<Booking, String> colArrivalGuest;
    @FXML private TableColumn<Booking, String> colArrivalRoom;
    @FXML private TableColumn<Booking, String> colArrivalNights;

    @FXML private TableView<Booking> departuresTable;
    @FXML private TableColumn<Booking, String> colDepartureId;
    @FXML private TableColumn<Booking, String> colDepartureGuest;
    @FXML private TableColumn<Booking, String> colDepartureRoom;
    @FXML private TableColumn<Booking, String> colDepartureBalance;

    @FXML private TableView<Bill> recentBillsTable;
    @FXML private TableColumn<Bill, String> colRecentBill;
    @FXML private TableColumn<Bill, String> colRecentGuest;
    @FXML private TableColumn<Bill, String> colRecentTotal;
    @FXML private TableColumn<Bill, String> colRecentStatus;

    @FXML private FlowPane galleryBox;

    @FXML
    public void initialize() {
        setupTables();
        setupGallery();
    }

    private void setupTables() {
        colArrivalId.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getBookingId()));
        colArrivalGuest.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getGuestName()));
        colArrivalRoom.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getRoomNumber()));
        colArrivalNights.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getNights() + " nights"));

        colDepartureId.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getBookingId()));
        colDepartureGuest.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getGuestName()));
        colDepartureRoom.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getRoomNumber()));
        colDepartureBalance.setCellValueFactory(d -> {
            Bill bill = BillingService.getInstance().findById(d.getValue().getBillId());
            String text = bill == null ? "Bill raised on check-out" : bill.getFormattedBalance() + " due";
            return new javafx.beans.property.SimpleStringProperty(text);
        });

        colRecentBill.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getBillId()));
        colRecentGuest.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getGuestName()));
        colRecentTotal.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getFormattedTotal()));
        colRecentStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getStatusLabel()));
    }

    private void setupGallery() {
        galleryBox.getChildren().setAll(
                photo("com/lodge/images/hero-lodge.jpg", "Thatched main lodge"),
                photo("com/lodge/images/pool.jpg", "Pool & gardens"),
                photo("com/lodge/images/restaurant.jpg", "Open-air restaurant"),
                photo("com/lodge/images/cottage.jpg", "VIP cottages")
        );
    }

    private VBoxPhoto photo(String path, String caption) {
        return new VBoxPhoto(FxImages.box(path, 220, 130), caption);
    }

    /** Tiny helper combining a photo with its caption. */
    private static class VBoxPhoto extends javafx.scene.layout.VBox {
        VBoxPhoto(ImageView image, String caption) {
            super(4);
            getStyleClass().add("gallery-item");
            Label label = new Label(caption);
            label.getStyleClass().add("gallery-caption");
            getChildren().addAll(image, label);
        }
    }

    @Override
    public void onShown() {
        ReportService reports = ReportService.getInstance();
        AuthService auth = AuthService.getInstance();

        if (welcomeLabel != null) {
            int hour = java.time.LocalTime.now().getHour();
            String greeting = hour < 12 ? "Good morning" : (hour < 17 ? "Good afternoon" : "Good evening");
            welcomeLabel.setText(greeting + ", " + auth.currentUserName() + " — here is today at the lodge.");
        }

        occupancyValueLabel.setText(String.format("%.0f%%", reports.occupancyRate()));
        occupiedValueLabel.setText(reports.occupiedRooms() + " / " + reports.totalRooms());
        availableValueLabel.setText(String.valueOf(reports.availableRooms()));
        revenueValueLabel.setText(Money.format(reports.revenueToday()));
        outstandingValueLabel.setText(Money.format(reports.outstandingBalance()));

        LocalDate today = LocalDate.now();
        arrivalsTable.getItems().setAll(BookingService.getInstance().arrivalsOn(today));
        departuresTable.getItems().setAll(BookingService.getInstance().departuresOn(today));

        var bills = BillingService.getInstance().getBills();
        int size = bills.size();
        recentBillsTable.getItems().setAll(bills.subList(Math.max(0, size - 8), size));

        YearMonth thisMonth = YearMonth.now();
        if (revenueValueLabel != null) {
            revenueValueLabel.setTooltip(new javafx.scene.control.Tooltip(
                    "Collected in " + thisMonth + ": " + Money.format(reports.revenueToday())));
        }
    }
}
