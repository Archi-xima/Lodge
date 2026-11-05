package com.lodge.controller;

import com.lodge.model.Room;
import com.lodge.model.RoomType;
import com.lodge.service.RoomService;
import com.lodge.util.FxImages;
import com.lodge.util.Logger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Room selection screen (Member 8).
 *
 * <p>Shows one card per room tier with a photo, nightly rate, occupancy
 * percentage and an availability badge. Staff pick a tier; fully booked tiers
 * are disabled. On confirm, the first free room of that tier is returned so the
 * bill-creation workflow can carry on with a real {@link Room}.</p>
 *
 * <p><b>How other screens use it</b> (one line, no wiring needed):</p>
 * <pre>
 * RoomSelectionController.pick(ownerWindow).ifPresent(room -&gt; { ... });
 * </pre>
 * The returned {@code Optional} is empty when staff cancel or close the window.
 * Member 9 can chain room customization straight after this call.
 */
public class RoomSelectionController implements ViewController {

    private static final Logger LOG = Logger.getLogger(RoomSelectionController.class);

    private static final String VIEW = "/com/lodge/view/room_selection.fxml";
    /** Same order MainApp uses. Dialog scenes do not inherit the main window's styles. */
    private static final String[] STYLESHEETS = {
            "/com/lodge/css/colors.css",
            "/com/lodge/css/typography.css",
            "/com/lodge/css/components.css",
            "/com/lodge/css/style.css"
    };

    private static final double CARD_WIDTH = 240;
    private static final double PHOTO_WIDTH = 218;
    private static final double PHOTO_HEIGHT = 120;

    @FXML private Label subtitleLabel;
    @FXML private FlowPane tierFlow;
    @FXML private Label selectedTitleLabel;
    @FXML private Label selectedDetailLabel;
    @FXML private Label messageLabel;
    @FXML private Button confirmBtn;

    private final Map<RoomType, VBox> cards = new EnumMap<>(RoomType.class);
    private RoomType selectedType;
    private Room chosenRoom;
    private Consumer<Room> onRoomSelected;

    // ------------------------------------------------------------------ entry point

    /**
     * Opens the room selection dialog and waits for the choice.
     *
     * @param owner window the dialog belongs to (may be null)
     * @return the room to bill, or empty if staff cancelled
     */
    public static Optional<Room> pick(Window owner) {
        try {
            FXMLLoader loader = new FXMLLoader(RoomSelectionController.class.getResource(VIEW));
            Parent root = loader.load();
            RoomSelectionController controller = loader.getController();
            controller.onShown();

            Stage stage = new Stage();
            stage.setTitle("Select a room");
            if (owner != null) {
                stage.initOwner(owner);
                stage.initModality(Modality.WINDOW_MODAL);
            } else {
                stage.initModality(Modality.APPLICATION_MODAL);
            }
            Scene scene = new Scene(root);
            for (String css : STYLESHEETS) {
                java.net.URL url = RoomSelectionController.class.getResource(css);
                if (url != null) {
                    scene.getStylesheets().add(url.toExternalForm());
                } else {
                    LOG.warn("Stylesheet not found, skipping: {}", css);
                }
            }
            stage.setScene(scene);
            stage.setMinWidth(560);
            stage.setMinHeight(520);
            stage.showAndWait();
            return Optional.ofNullable(controller.chosenRoom);
        } catch (Exception e) {
            LOG.error("Could not open the room selection screen", e);
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Room selection");
            alert.setHeaderText("Could not open the room selection screen");
            alert.setContentText(String.valueOf(e));
            alert.showAndWait();
            return Optional.empty();
        }
    }

    /** Optional callback fired when staff confirm a room (alternative to {@link #pick}). */
    public void setOnRoomSelected(Consumer<Room> onRoomSelected) {
        this.onRoomSelected = onRoomSelected;
    }

    public Room getChosenRoom() {
        return chosenRoom;
    }

    // ------------------------------------------------------------------ lifecycle

    @FXML
    public void initialize() {
        confirmBtn.setDisable(true);
        messageLabel.setText("");
    }

    @Override
    public void onShown() {
        render();
    }

    // ------------------------------------------------------------------ data

    private List<Room> currentRooms() {
        return new ArrayList<>(RoomService.getInstance().getRooms());
    }

    /**
     * SWAP POINT for Member 7: once RoomAvailabilityService is merged, build the
     * tier from it here instead. Nothing else in this class needs to change.
     */
    private RoomTierViewModel loadTier(RoomType type, List<Room> rooms) {
        return RoomTierViewModel.of(type, rooms);
    }

    // ------------------------------------------------------------------ drawing

    private void render() {
        tierFlow.getChildren().clear();
        cards.clear();

        List<Room> rooms = currentRooms();
        int freeTotal = 0;
        for (RoomType type : RoomType.values()) {
            RoomTierViewModel tier = loadTier(type, rooms);
            freeTotal += tier.getAvailableRooms();
            VBox card = buildCard(tier);
            cards.put(type, card);
            tierFlow.getChildren().add(card);
        }
        subtitleLabel.setText(freeTotal + (freeTotal == 1 ? " room is" : " rooms are")
                + " free right now across " + RoomType.values().length + " room types.");

        // keep an earlier choice only if that tier can still be sold
        if (selectedType != null) {
            RoomTierViewModel tier = loadTier(selectedType, rooms);
            if (tier.isSelectable()) {
                highlight(selectedType);
                showSelection(tier);
                return;
            }
            selectedType = null;
        }
        showSelection(null);
    }

    private VBox buildCard(RoomTierViewModel tier) {
        RoomType type = tier.getType();

        VBox card = new VBox(8);
        card.getStyleClass().addAll("room-card", "tier-card");
        card.setPrefWidth(CARD_WIDTH);
        card.setMinWidth(CARD_WIDTH);
        card.setMaxWidth(CARD_WIDTH);
        card.setAccessibleText(type.getDisplayName() + ", " + tier.getRateText() + ", "
                + tier.getAvailabilityText() + ", " + tier.getLevel().getLabel());

        Label title = new Label(type.getDisplayName());
        title.getStyleClass().add("room-card-title");

        Label description = new Label(type.getDescription());
        description.getStyleClass().add("room-card-sub");
        description.setWrapText(true);
        description.setMinHeight(Region.USE_PREF_SIZE);

        Label rate = new Label(tier.getRateText());
        rate.getStyleClass().add("room-card-rate");
        Label capacity = new Label(tier.getCapacityText());
        capacity.getStyleClass().add("room-card-sub");
        HBox rateRow = new HBox(8, rate, spacer(), capacity);
        rateRow.setAlignment(Pos.CENTER_LEFT);

        ProgressBar occupancyBar = new ProgressBar(tier.getOccupancyPercent() / 100.0);
        occupancyBar.getStyleClass().add("tier-occupancy-bar");
        occupancyBar.setMaxWidth(Double.MAX_VALUE);

        Label badge = new Label(tier.getLevel().getLabel());
        badge.getStyleClass().addAll("tier-badge", tier.getLevel().getStyleClass());
        badge.setMinWidth(Region.USE_PREF_SIZE);   // never truncate "Fully Booked"
        Label occupancy = new Label(tier.getOccupancyPercent() + "% occupied");
        occupancy.getStyleClass().add("muted-text-small");
        HBox statusRow = new HBox(8, badge, spacer(), occupancy);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Label availability = new Label(tier.getAvailabilityText());
        availability.getStyleClass().add("room-card-sub");

        card.getChildren().addAll(photo(type), title, description, rateRow,
                occupancyBar, statusRow, availability);

        if (tier.isSelectable()) {
            card.setFocusTraversable(true);
            card.setOnMouseClicked(event -> {
                select(type);
                if (event.getClickCount() == 2) {
                    handleConfirm();
                }
            });
            card.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                    select(type);
                }
            });
        } else {
            // Spec: fully booked tiers cannot be selected.
            Label note = new Label("Fully booked. Please choose another room type.");
            note.getStyleClass().add("tier-full-note");
            note.setWrapText(true);
            card.getChildren().add(note);
            card.getStyleClass().add("tier-card-disabled");
            card.setDisable(true);
        }
        return card;
    }

    /** Room photo cropped to fill its frame, or a bed icon if the image file is missing. */
    private Region photo(RoomType type) {
        ImageView image = FxImages.box(type.getImageResource(), PHOTO_WIDTH, PHOTO_HEIGHT);
        StackPane frame = new StackPane();
        frame.getStyleClass().add("tier-photo-frame");
        frame.setPrefSize(PHOTO_WIDTH, PHOTO_HEIGHT);
        frame.setMinSize(PHOTO_WIDTH, PHOTO_HEIGHT);
        frame.setMaxSize(PHOTO_WIDTH, PHOTO_HEIGHT);

        Rectangle clip = new Rectangle(PHOTO_WIDTH, PHOTO_HEIGHT);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        frame.setClip(clip);

        if (image.getImage() != null) {
            // centre-crop so the photo fills the frame without being stretched
            double w = image.getImage().getWidth();
            double h = image.getImage().getHeight();
            double targetRatio = PHOTO_WIDTH / PHOTO_HEIGHT;
            double cropW = Math.min(w, h * targetRatio);
            double cropH = cropW / targetRatio;
            image.setPreserveRatio(false);
            image.setViewport(new Rectangle2D((w - cropW) / 2, (h - cropH) / 2, cropW, cropH));
            frame.getChildren().add(image);
        } else {
            Label icon = new Label("\uD83D\uDECF");
            icon.getStyleClass().add("tier-photo-icon");
            frame.getChildren().add(icon);
        }
        return frame;
    }

    private Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    // ------------------------------------------------------------------ selection

    private void select(RoomType type) {
        RoomTierViewModel tier = loadTier(type, currentRooms());
        if (!tier.isSelectable()) {
            // someone else took the last room while this screen was open
            showMessage(type.getDisplayName() + " just became fully booked. Please choose another room type.");
            if (selectedType == type) {
                selectedType = null;
            }
            render();
            return;
        }
        selectedType = type;
        highlight(type);
        showSelection(tier);
        showMessage("");
    }

    private void highlight(RoomType type) {
        cards.values().forEach(card -> card.getStyleClass().remove("tier-card-selected"));
        VBox card = cards.get(type);
        if (card != null) {
            card.getStyleClass().add("tier-card-selected");
        }
    }

    private void showSelection(RoomTierViewModel tier) {
        if (tier == null) {
            selectedTitleLabel.setText("No room type selected");
            selectedDetailLabel.setText("Click a room type above to choose it.");
            confirmBtn.setDisable(true);
            return;
        }
        selectedTitleLabel.setText(tier.getType().getDisplayName() + " selected");
        selectedDetailLabel.setText(tier.getRateText() + "  \u2022  " + tier.getAvailabilityText()
                + "  \u2022  " + tier.getOccupancyPercent() + "% occupied");
        confirmBtn.setDisable(false);
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }

    // ------------------------------------------------------------------ buttons

    @FXML
    private void handleConfirm() {
        if (selectedType == null) {
            return;
        }
        Optional<Room> room = RoomTierViewModel.firstAvailableRoom(selectedType, currentRooms());
        if (room.isEmpty()) {
            showMessage(selectedType.getDisplayName() + " just became fully booked. Please choose another room type.");
            selectedType = null;
            render();
            return;
        }
        chosenRoom = room.get();
        LOG.info("Room {} ({}) chosen on the room selection screen",
                chosenRoom.getRoomNumber(), chosenRoom.getTypeName());
        if (onRoomSelected != null) {
            onRoomSelected.accept(chosenRoom);
        }
        closeWindow();
    }

    @FXML
    private void handleCancel() {
        chosenRoom = null;
        closeWindow();
    }

    private void closeWindow() {
        if (tierFlow.getScene() != null && tierFlow.getScene().getWindow() instanceof Stage stage) {
            stage.close();
        }
    }
}
