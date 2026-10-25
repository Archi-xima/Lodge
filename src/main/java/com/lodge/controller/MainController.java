package com.lodge.controller;

import com.lodge.model.Bill;
import com.lodge.model.RoomType;
import com.lodge.model.User;
import com.lodge.service.AuthService;
import com.lodge.service.BillingService;
import com.lodge.service.DataStore;
import com.lodge.service.ReportService;
import com.lodge.util.Money;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Shell of the application: header, side navigation and the content area
 * where each module (dashboard, rooms, bookings ...) is loaded.
 */
public class MainController implements ViewController {

    /** Simple reference so child screens can ask the shell to navigate. */
    private static MainController active;

    public static MainController getActive() {
        return active;
    }

    @FXML private Label currentUserLabel;
    @FXML private Label userRoleBadge;
    @FXML private Label occupancyBadge;
    @FXML private Label statusLabel;
    @FXML private StackPane contentPane;

    @FXML private Button navDashboard;
    @FXML private Button navRooms;
    @FXML private Button navBookings;
    @FXML private Button navGuests;
    @FXML private Button navUsers;
    @FXML private Button navBilling;
    @FXML private Button navReports;

    private final Map<Button, String> navMap = new LinkedHashMap<>();
    private final Map<String, ViewController> loadedControllers = new LinkedHashMap<>();

    private Button activeNavButton;

    @FXML
    public void initialize() {
        active = this;

        navMap.put(navDashboard, "dashboard");
        navMap.put(navRooms, "rooms");
        navMap.put(navBookings, "bookings");
        navMap.put(navGuests, "guests");
        navMap.put(navUsers, "users");
        navMap.put(navBilling, "billing");
        navMap.put(navReports, "reports");

        navMap.forEach((button, key) -> button.setOnAction(event -> navigateTo(key)));

        User user = AuthService.getInstance().getCurrentUser();
        if (user != null) {
            currentUserLabel.setText(user.getFullName());
            userRoleBadge.setText(user.getRoleLabel());
        }
        refreshBadges();
        navigateTo("dashboard");
    }

    /** Refreshes the occupancy pill in the sidebar. */
    private void refreshBadges() {
        ReportService reports = ReportService.getInstance();
        occupancyBadge.setText(String.format("%.0f%%  •  %d of %d rooms",
                reports.occupancyRate(), reports.occupiedRooms(), reports.totalRooms()));
    }

    // ------------------------------------------------------------------ navigation

    public void navigateTo(String key) {
        Button button = navMap.entrySet().stream()
                .filter(e -> e.getValue().equals(key))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);

        if (button != null) {
            if (activeNavButton != null) {
                activeNavButton.getStyleClass().remove("nav-btn-active");
            }
            button.getStyleClass().add("nav-btn-active");
            activeNavButton = button;
        }

        try {
            // Always rebuild the screen so it is shown with fresh data.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lodge/view/" + key + ".fxml"));
            Parent root = loader.load();
            Object loaded = loader.getController();
            if (loaded instanceof ViewController) {
                loadedControllers.put(key, (ViewController) loaded);
                ((ViewController) loaded).onShown();
            }
            contentPane.getChildren().setAll(root);
            FadeTransition ft = new FadeTransition(Duration.millis(200), root);
            ft.setFromValue(0.3);
            ft.setToValue(1);
            ft.play();
            statusLabel.setText("Module: " + capitalize(key) + "   |   " + DataStore.getInstance().getDataDir());
            refreshBadges();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Could not open " + key + ": " + e.getMessage());
        }
    }

    private static String capitalize(String text) {
        return text == null ? "" : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    @Override
    public void onShown() {
        refreshBadges();
    }

    // ------------------------------------------------------------------ menus

    @FXML
    private void handleMenuNewBooking(ActionEvent event) {
        navigateTo("bookings");
    }

    @FXML
    private void handleMenuNewInvoice(ActionEvent event) {
        navigateTo("billing");
    }

    @FXML
    private void handleMenuRooms(ActionEvent event) {
        navigateTo("rooms");
    }

    @FXML
    private void handleMenuReports(ActionEvent event) {
        navigateTo("reports");
    }

    @FXML
    private void handlePricingRules(ActionEvent event) {
        StringBuilder text = new StringBuilder("ROOM RATES (per night)\n");
        for (RoomType type : RoomType.values()) {
            text.append(String.format("   • %-22s %s   (sleeps %d)%n", type.getDisplayName(),
                    Money.format(type.getRatePerNight()), type.getCapacity()));
        }
        text.append("\nBILLING RULES\n")
                .append("   • Stays of ").append(Bill.LONG_STAY_MIN_NIGHTS)
                .append(" nights or more earn ").append((int) (Bill.LONG_STAY_DISCOUNT_RATE * 100))
                .append("% off the room charge.\n")
                .append("   • Extra services are never discounted.\n")
                .append("   • 16% VAT is applied to the subtotal.\n")
                .append("   • All money is expressed in Zambian Kwacha (K).\n")
                .append("\nAll bills, guests, rooms and bookings are saved to\n")
                .append(DataStore.getInstance().getDataDir());

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Rates & Billing Rules");
        alert.setHeaderText("How the lodge prices a stay");
        alert.setContentText(text.toString());
        alert.showAndWait();
    }

    @FXML
    private void handleAuditTrail(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Audit Trail");
        alert.setHeaderText("Latest system activity");
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (var entry : AuthService.getInstance().getAuditTrail()) {
            sb.append(entry.getFormattedTimestamp()).append("  ")
                    .append(entry.getUsername()).append("  ")
                    .append(entry.getAction()).append(" — ")
                    .append(entry.getDetail()).append("\n");
            if (++shown >= 15) {
                break;
            }
        }
        alert.setContentText(sb.length() == 0 ? "No activity recorded yet." : sb.toString());
        alert.getDialogPane().setMinWidth(620);
        alert.showAndWait();
    }

    @FXML
    private void handleAbout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About SkyRidge Lodge Billing System");
        alert.setHeaderText("Full Lodge Management & Billing Suite");
        VBox content = new VBox(6);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().addAll(
                new Label("SkyRidge Lodge Billing System  —  v2.0"),
                new Label("Originally developed for ICT261 (OOP) • Mr E. Nyirenda • Mulungushi University"),
                new Separator(),
                new Label("Modules: Dashboard, Rooms, Reservations, Guests, Billing, Reports"),
                new Label("Features:"),
                new Label("  • Room inventory with live occupancy and photographs"),
                new Label("  • Reservations with check-in / check-out workflow"),
                new Label("  • Automatic invoicing with long-stay discount and 16% VAT"),
                new Label("  • Extras price list, part payments and printable receipts"),
                new Label("  • Role-based access, audit trail and JSON data persistence")
        );
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }

    @FXML
    private void handleSignOut(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Sign Out");
        confirm.setHeaderText("Sign out of the Lodge Billing System?");
        confirm.setContentText("Unsaved changes are kept on disk. You can sign in again at any time.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            AuthService.getInstance().logout();
            loadedControllers.clear();
            active = null;
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lodge/view/login.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) contentPane.getScene().getWindow();
                Scene scene = new Scene(root, 1080, 720);
                scene.getStylesheets().add(getClass().getResource("/com/lodge/css/style.css").toExternalForm());
                root.setOpacity(0);
                stage.setScene(scene);
                stage.setTitle("Sign In — SkyRidge Lodge Billing System");
                stage.centerOnScreen();
                FadeTransition ft = new FadeTransition(Duration.millis(300), root);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleExit(ActionEvent event) {
        Platform.exit();
    }

    @Override
    public String toString() {
        return "MainController";
    }
}
