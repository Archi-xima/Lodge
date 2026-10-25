package com.lodge.controller;

import com.lodge.model.Guest;
import com.lodge.service.GuestService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 * Guest book: search, create, edit and delete guest profiles.
 */
public class GuestsController implements ViewController {

    @FXML private TextField searchField;
    @FXML private TableView<Guest> guestsTable;
    @FXML private TableColumn<Guest, String> colGuestId;
    @FXML private TableColumn<Guest, String> colName;
    @FXML private TableColumn<Guest, String> colPhone;
    @FXML private TableColumn<Guest, String> colEmail;
    @FXML private TableColumn<Guest, String> colIdPassport;
    @FXML private TableColumn<Guest, String> colNationality;

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField idField;
    @FXML private TextField nationalityField;
    @FXML private TextField notesField;
    @FXML private Button saveGuestBtn;
    @FXML private Button deleteGuestBtn;

    private Guest selectedGuest;

    @FXML
    public void initialize() {
        colGuestId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getGuestId()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        colPhone.setCellValueFactory(d -> new SimpleStringProperty(emptySafe(d.getValue().getPhone())));
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(emptySafe(d.getValue().getEmail())));
        colIdPassport.setCellValueFactory(d -> new SimpleStringProperty(emptySafe(d.getValue().getIdPassport())));
        colNationality.setCellValueFactory(d -> new SimpleStringProperty(emptySafe(d.getValue().getNationality())));

        searchField.textProperty().addListener((obs, o, n) -> refreshTable());
        guestsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                fillForm(n);
            }
        });
        deleteGuestBtn.setDisable(true);
    }

    private String emptySafe(String text) {
        return text == null || text.trim().isEmpty() ? "—" : text;
    }

    @Override
    public void onShown() {
        refreshTable();
    }

    private void refreshTable() {
        Guest selected = guestsTable.getSelectionModel().getSelectedItem();
        String keepId = selected == null ? null : selected.getGuestId();
        guestsTable.setItems(GuestService.getInstance().search(searchField.getText()));
        if (keepId != null) {
            for (Guest guest : guestsTable.getItems()) {
                if (keepId.equals(guest.getGuestId())) {
                    guestsTable.getSelectionModel().select(guest);
                    break;
                }
            }
        }
    }

    private void fillForm(Guest guest) {
        selectedGuest = guest;
        nameField.setText(guest.getFullName());
        phoneField.setText(guest.getPhone());
        emailField.setText(guest.getEmail());
        idField.setText(guest.getIdPassport());
        nationalityField.setText(guest.getNationality());
        notesField.setText(guest.getNotes());
        deleteGuestBtn.setDisable(false);
    }

    @FXML
    private void handleSaveGuest() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) {
            warn("Guest name is required.");
            return;
        }
        Guest guest = selectedGuest != null ? selectedGuest : new Guest();
        guest.setFullName(name);
        guest.setPhone(text(phoneField));
        guest.setEmail(text(emailField));
        guest.setIdPassport(text(idField));
        guest.setNationality(text(nationalityField));
        guest.setNotes(text(notesField));
        GuestService.getInstance().saveGuest(guest);
        selectedGuest = guest;
        refreshTable();
        guestsTable.getSelectionModel().select(guest);
    }

    @FXML
    private void handleDeleteGuest() {
        if (selectedGuest == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete guest");
        confirm.setHeaderText("Remove " + selectedGuest.getFullName() + " from the guest book?");
        confirm.setContentText("Previous bookings and bills keep their own copy of the guest details.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            GuestService.getInstance().removeGuest(selectedGuest);
            handleClearForm();
            refreshTable();
        }
    }

    @FXML
    private void handleClearForm() {
        selectedGuest = null;
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        idField.clear();
        nationalityField.clear();
        notesField.clear();
        deleteGuestBtn.setDisable(true);
        guestsTable.getSelectionModel().clearSelection();
    }

    private String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void warn(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Guest book");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
