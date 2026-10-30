package com.lodge.controller;

import com.lodge.model.Room;
import com.lodge.model.RoomAvailability;
import com.lodge.model.RoomStatus;
import com.lodge.model.RoomType;
import com.lodge.service.RoomAvailabilityService;
import com.lodge.service.RoomService;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Room inventory: filterable photo cards, availability overview,
 * and a form to add, edit and remove rooms.
 */
public class RoomsController implements ViewController {

    @FXML private ComboBox<RoomType> filterTypeCombo;
    @FXML private ComboBox<RoomStatus> filterStatusCombo;
    @FXML private TextField searchField;
    @FXML private FlowPane roomsFlow;
    @FXML private FlowPane availabilitySummaryFlow;
    @FXML private Label roomsCountLabel;

    @FXML private Label formTitleLabel;
    @FXML private TextField numberField;
    @FXML private ComboBox<RoomType> typeCombo;
    @FXML private TextField floorField;
    @FXML private ComboBox<RoomStatus> statusCombo;
    @FXML private TextField rateField;
    @FXML private TextField featuresField;
    @FXML private Button saveRoomBtn;
    @FXML private Button deleteRoomBtn;

    private Room selectedRoom;

    @FXML
    public void initialize() {

        filterTypeCombo.getItems().add(null);
        filterTypeCombo.getItems().addAll(RoomType.values());

        filterTypeCombo.setConverter(
                new StringConverter<>(null, "All types")
        );

        filterTypeCombo.valueProperty().addListener(
                (obs, oldValue, newValue) -> renderCards()
        );

        filterStatusCombo.getItems().add(null);
        filterStatusCombo.getItems().addAll(RoomStatus.values());

        filterStatusCombo.setConverter(
                new StringConverter<>(null, "All statuses")
        );

        filterStatusCombo.valueProperty().addListener(
                (obs, oldValue, newValue) -> renderCards()
        );

        typeCombo.getItems().addAll(RoomType.values());
        typeCombo.setValue(RoomType.STANDARD);

        statusCombo.getItems().addAll(RoomStatus.values());
        statusCombo.setValue(RoomStatus.AVAILABLE);

        searchField.textProperty().addListener(
                (obs, oldValue, newValue) -> renderCards()
        );

        clearForm();
    }

    /**
     * ComboBox converter that displays a friendly label
     * for the null "All" entry.
     */
    private static class StringConverter<T>
            extends javafx.util.StringConverter<T> {

        private final T nullValue;
        private final String nullLabel;

        StringConverter(T nullValue, String nullLabel) {
            this.nullValue = nullValue;
            this.nullLabel = nullLabel;
        }

        @Override
        public String toString(T object) {
            return object == null ? nullLabel : object.toString();
        }

        @Override
        public T fromString(String string) {
            return nullValue;
        }
    }

    @Override
    public void onShown() {
        renderCards();
    }

    /**
     * Refreshes the room cards and availability overview.
     */
    private void renderCards() {

        RoomType type = filterTypeCombo.getValue();
        RoomStatus status = filterStatusCombo.getValue();

        String needle = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase();

        roomsFlow.getChildren().clear();

        int count = 0;

        for (Room room : RoomService.getInstance().getRooms()) {

            if (type != null && room.getType() != type) {
                continue;
            }

            if (status != null && room.getStatus() != status) {
                continue;
            }

            if (!needle.isEmpty()
                    && !room.getRoomNumber().toLowerCase().contains(needle)
                    && !room.getTypeName().toLowerCase().contains(needle)) {
                continue;
            }

            roomsFlow.getChildren().add(buildCard(room));
            count++;
        }

        roomsCountLabel.setText(count + " room(s) shown");

        // Refresh availability independently of the selected filters.
        refreshAvailabilitySummary();
    }

    /**
     * Displays overall room statistics and availability
     * information for each room type.
     */
    private void refreshAvailabilitySummary() {

        System.out.println("Availability summary refresh is running...");

        System.out.println("Availability summary pane found: "
                + (availabilitySummaryFlow != null));

        // Stop if the FXML pane was not injected.
        if (availabilitySummaryFlow == null) {
            System.out.println("ERROR: availabilitySummaryFlow is null!");
            return;
        }

        RoomAvailabilityService service =
                RoomAvailabilityService.getInstance();

        availabilitySummaryFlow.getChildren().clear();

        // Overall room statistics
        addSummaryCard(
                "TOTAL ROOMS",
                String.valueOf(service.getTotalRooms()),
                "Rooms in the lodge",
                "stat-card"
        );

        addSummaryCard(
                "AVAILABLE",
                String.valueOf(service.getTotalAvailableRooms()),
                "Ready for guests",
                "stat-card-green"
        );

        addSummaryCard(
                "OCCUPIED",
                String.valueOf(service.getTotalOccupiedRooms()),
                String.format("%.1f%% occupancy",
                        service.getOverallOccupancyPercentage()),
                "stat-card"
        );

        addSummaryCard(
                "RESERVED",
                String.valueOf(service.getTotalReservedRooms()),
                "Reserved rooms",
                "stat-card-amber"
        );

        addSummaryCard(
                "MAINTENANCE",
                String.valueOf(service.getTotalMaintenanceRooms()),
                "Unavailable for use",
                "stat-card"
        );

        // Availability breakdown by room type
        for (RoomAvailability availability : service.getAllAvailability()) {

            VBox card = new VBox(6);
            card.getStyleClass().add("stat-card");
            card.setPrefWidth(230);
            card.setMinWidth(210);

            Label roomType = new Label(
                    availability.getRoomType().getDisplayName()
            );
            roomType.getStyleClass().add("stat-label");

            Label available = new Label(
                    availability.getAvailabilityText()
            );
            available.getStyleClass().add("stat-value");

            Label occupancy = new Label(
                    String.format("Occupancy: %.1f%%",
                            availability.getOccupancyPercentage())
            );
            occupancy.getStyleClass().add("stat-hint");

            String state = availability.getAvailabilityState();

            Label status = new Label(state);
            status.getStyleClass().addAll(
                    "status-pill",
                    availabilityStatusStyle(state)
            );

            Label details = new Label(
                    "Occupied: " + availability.getOccupiedRooms()
                            + "  |  Reserved: " + availability.getReservedRooms()
                            + "  |  Maintenance: "
                            + availability.getMaintenanceRooms()
            );

            details.getStyleClass().add("stat-hint");
            details.setWrapText(true);

            card.getChildren().addAll(
                    roomType,
                    available,
                    occupancy,
                    status,
                    details
            );

            availabilitySummaryFlow.getChildren().add(card);
        }

        System.out.println("Availability cards displayed: "
                + availabilitySummaryFlow.getChildren().size());
    }

    /**
     * Creates a summary card for an overall room statistic.
     */
    private void addSummaryCard(
            String heading,
            String value,
            String hint,
            String styleClass) {

        VBox card = new VBox(6);

        card.getStyleClass().add("stat-card");

        if (!"stat-card".equals(styleClass)) {
            card.getStyleClass().add(styleClass);
        }

        card.setPrefWidth(160);
        card.setMinWidth(145);

        Label title = new Label(heading);
        title.getStyleClass().add("stat-label");

        Label number = new Label(value);
        number.getStyleClass().add("stat-value");

        Label description = new Label(hint);
        description.getStyleClass().add("stat-hint");
        description.setWrapText(true);

        card.getChildren().addAll(
                title,
                number,
                description
        );

        availabilitySummaryFlow.getChildren().add(card);
    }

    /**
     * Selects the appropriate CSS status class
     * for an availability state.
     */
    private String availabilityStatusStyle(String state) {

        switch (state) {

            case "Available":
                return "status-green";

            case "Limited":
                return "status-amber";

            case "No Rooms":
                return "status-grey";

            case "Under Maintenance":
                return "status-red";

            case "Fully Booked":
            default:
                return "status-red";
        }
    }

    /**
     * Builds an individual room card.
     */
    private VBox buildCard(Room room) {

        VBox card = new VBox(6);
        card.getStyleClass().add("room-card");
        card.setAlignment(Pos.TOP_CENTER);

        card.getChildren().add(
                com.lodge.util.FxImages.box(
                        room.getImageResource(), 220, 130
                )
        );

        Label number = new Label(
                "Room " + room.getRoomNumber()
        );
        number.getStyleClass().add("room-card-title");

        Label type = new Label(
                room.getTypeName() + "  •  Floor " + room.getFloor()
        );
        type.getStyleClass().add("room-card-sub");

        Label rate = new Label(
                room.getFormattedRate() + " / night"
        );
        rate.getStyleClass().add("room-card-rate");

        Label features = new Label(
                room.getFeaturesText()
        );
        features.setWrapText(true);
        features.setMaxWidth(210);
        features.setAlignment(Pos.CENTER);
        features.getStyleClass().add("room-card-features");

        Label status = new Label(
                room.getStatusLabel()
        );
        status.getStyleClass().addAll(
                "status-pill",
                statusStyle(room.getStatus())
        );

        card.getChildren().addAll(
                number,
                type,
                rate,
                features,
                status
        );

        card.setOnMouseClicked(e -> selectRoom(room));

        if (selectedRoom == room) {
            card.getStyleClass().add("room-card-selected");
        }

        return card;
    }

    /**
     * Returns the CSS class for a room's status.
     */
    private String statusStyle(RoomStatus status) {

        if (status == null) {
            return "status-grey";
        }

        switch (status) {

            case AVAILABLE:
                return "status-green";

            case RESERVED:
                return "status-blue";

            case OCCUPIED:
                return "status-amber";

            case MAINTENANCE:
                return "status-red";

            default:
                return "status-grey";
        }
    }

    /**
     * Selects a room and loads its details into the form.
     */
    private void selectRoom(Room room) {

        selectedRoom = room;

        formTitleLabel.setText(
                "Edit room " + room.getRoomNumber()
        );

        numberField.setText(room.getRoomNumber());
        typeCombo.setValue(room.getType());
        floorField.setText(String.valueOf(room.getFloor()));
        statusCombo.setValue(room.getStatus());

        rateField.setText(
                room.getRatePerNight() > 0
                        ? String.valueOf(room.getRatePerNight())
                        : ""
        );

        featuresField.setText(
                String.join(", ", room.getFeatures())
        );

        deleteRoomBtn.setDisable(false);

        renderCards();
    }

    /**
     * Saves a new room or updates an existing room.
     */
    @FXML
    private void handleSaveRoom() {

        String number = numberField.getText() == null
                ? ""
                : numberField.getText().trim();

        if (number.isEmpty()) {
            warn("Please enter a room number.");
            return;
        }

        Room existing = RoomService.getInstance().findByNumber(number);

        if (existing != null && existing != selectedRoom) {
            warn("Room " + number + " already exists.");
            return;
        }

        int floor = 0;

        try {

            String floorText = floorField.getText();

            if (floorText != null && !floorText.trim().isEmpty()) {
                floor = Integer.parseInt(floorText.trim());
            }

        } catch (NumberFormatException e) {

            warn("Floor must be a whole number.");
            return;
        }

        double rate = 0;

        try {

            String rateText = rateField.getText();

            if (rateText != null && !rateText.trim().isEmpty()) {
                rate = com.lodge.util.Money.parse(rateText);
            }

        } catch (NumberFormatException e) {

            warn("Rate must be a valid amount, e.g. 450 or 450.00");
            return;
        }

        Room room = selectedRoom != null
                ? selectedRoom
                : new Room();

        room.setRoomNumber(number);
        room.setType(typeCombo.getValue());
        room.setFloor(floor);
        room.setStatus(statusCombo.getValue());
        room.setRatePerNight(rate);

        room.getFeatures().clear();

        if (featuresField.getText() != null
                && !featuresField.getText().trim().isEmpty()) {

            for (String feature : featuresField.getText().split(",")) {

                if (!feature.trim().isEmpty()) {
                    room.getFeatures().add(feature.trim());
                }
            }
        }

        if (selectedRoom == null) {
            RoomService.getInstance().addRoom(room);
        } else {
            RoomService.getInstance().updateRoom(room);
        }

        selectedRoom = room;

        clearForm();
        renderCards();
    }

    /**
     * Deletes the selected room after confirmation.
     */
    @FXML
    private void handleDeleteRoom() {

        if (selectedRoom == null) {
            return;
        }

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        confirm.setTitle("Delete room");

        confirm.setHeaderText(
                "Delete room " + selectedRoom.getRoomNumber() + "?"
        );

        confirm.setContentText(
                "Existing bills keep their own copy of the room details."
        );

        if (confirm.showAndWait().orElse(ButtonType.CANCEL)
                == ButtonType.OK) {

            RoomService.getInstance().removeRoom(selectedRoom);

            clearForm();
            renderCards();
        }
    }

    /**
     * Clears the room form.
     */
    @FXML
    private void handleClearForm() {

        clearForm();
        renderCards();
    }

    private void clearForm() {

        selectedRoom = null;

        formTitleLabel.setText("Add a new room");

        numberField.clear();
        typeCombo.setValue(RoomType.STANDARD);
        floorField.setText("1");
        statusCombo.setValue(RoomStatus.AVAILABLE);
        rateField.clear();
        featuresField.clear();

        deleteRoomBtn.setDisable(true);
    }

    /**
     * Displays a warning message.
     */
    private void warn(String message) {

        Alert alert = new Alert(
                Alert.AlertType.WARNING
        );

        alert.setTitle("Room form");
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }
}