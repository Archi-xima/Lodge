package com.lodge.controller;

import com.lodge.model.BillStatus;
import com.lodge.model.PaymentMethod;
import com.lodge.model.RoomType;
import com.lodge.service.BillingService;
import com.lodge.service.ReportService;
import com.lodge.service.AccountingService;
import com.lodge.util.Money;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Management reporting: revenue, payment mix, occupancy and top extras.
 */
public class ReportsController implements ViewController {

    @FXML private ComboBox<YearMonth> monthCombo;
    @FXML private Label billedLabel;
    @FXML private Label collectedLabel;
    @FXML private Label outstandingLabel;
    @FXML private Label occupancyLabel;
    @FXML private Label invoicesLabel;

    @FXML private BarChart<String, Number> revenueChart;
    @FXML private BarChart<String, Number> trendChart;
    @FXML private PieChart paymentsPie;

    @FXML private TableView<ReportService.ServiceSummary> servicesTable;
    @FXML private TableColumn<ReportService.ServiceSummary, String> colServiceName;
    @FXML private TableColumn<ReportService.ServiceSummary, String> colServiceCategory;
    @FXML private TableColumn<ReportService.ServiceSummary, Number> colServiceQty;
    @FXML private TableColumn<ReportService.ServiceSummary, String> colServiceRevenue;

    @FXML
    public void initialize() {
        AccountingService.getInstance().requireAccess();
        colServiceName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name));
        colServiceCategory.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().category));
        colServiceQty.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(d.getValue().quantity));
        colServiceRevenue.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedRevenue()));

        List<YearMonth> months = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 11; i >= 0; i--) {
            months.add(current.minusMonths(i));
        }
        monthCombo.setItems(FXCollections.observableArrayList(months));
        monthCombo.setValue(current);
        monthCombo.valueProperty().addListener((obs, o, n) -> refresh());
    }

    @Override
    public void onShown() {
        AccountingService.getInstance().requireAccess();
        refresh();
    }

    private void refresh() {
        AccountingService.getInstance().requireAccess();
        ReportService reports = ReportService.getInstance();
        YearMonth month = monthCombo.getValue() == null ? YearMonth.now() : monthCombo.getValue();

        billedLabel.setText(Money.format(BillingService.getInstance().billedInMonth(month)));
        collectedLabel.setText(Money.format(BillingService.getInstance().revenueInMonth(month)));
        outstandingLabel.setText(Money.format(reports.outstandingBalance()));
        occupancyLabel.setText(String.format("%.0f%%", reports.occupancyRate()));
        long invoices = BillingService.getInstance().getBills().stream()
                .filter(b -> b.getStatus() != BillStatus.VOID && b.getCreatedAt() != null
                        && YearMonth.from(b.getCreatedAt()).equals(month))
                .count();
        invoicesLabel.setText(String.valueOf(invoices));

        buildRevenueChart(month);
        buildTrendChart();
        buildPaymentsPie(month);
        servicesTable.getItems().setAll(reports.topServices(8));
    }

    private void buildRevenueChart(YearMonth month) {
        revenueChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Billed in " + month);
        for (Map.Entry<RoomType, Double> entry : ReportService.getInstance().billedByRoomType(month).entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey().getDisplayName(), entry.getValue()));
        }
        if (series.getData().isEmpty()) {
            series.getData().add(new XYChart.Data<>("No invoices", 0));
        }
        revenueChart.getData().add(series);
    }

    private void buildTrendChart() {
        trendChart.getData().clear();
        XYChart.Series<String, Number> billed = new XYChart.Series<>();
        billed.setName("Billed");
        XYChart.Series<String, Number> collected = new XYChart.Series<>();
        collected.setName("Collected");
        for (ReportService.MonthlySummary summary : ReportService.getInstance().monthlySummaries(6)) {
            billed.getData().add(new XYChart.Data<>(summary.getLabel(), summary.billed));
            collected.getData().add(new XYChart.Data<>(summary.getLabel(), summary.collected));
        }
        trendChart.getData().addAll(billed, collected);
    }

    private void buildPaymentsPie(YearMonth month) {
        paymentsPie.getData().clear();
        Map<PaymentMethod, Double> byMethod = ReportService.getInstance().paymentsByMethod(month);
        if (byMethod.isEmpty()) {
            paymentsPie.getData().add(new PieChart.Data("No payments", 1));
            return;
        }
        byMethod.forEach((method, amount) ->
                paymentsPie.getData().add(new PieChart.Data(method.getLabel() + "  " + Money.format(amount), amount)));
    }
}
