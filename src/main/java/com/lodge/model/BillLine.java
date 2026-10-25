package com.lodge.model;

import com.lodge.util.Money;

/** One chargeable row on a bill (an extra service such as dinner or laundry). */
public class BillLine {

    private String description;
    private int quantity;
    private double unitPrice;
    private ServiceCategory category;

    public BillLine() {
        this.quantity = 1;
    }

    public BillLine(String description, int quantity, double unitPrice, ServiceCategory category) {
        this();
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.category = category;
    }

    public static BillLine fromService(ServiceItem item, int quantity) {
        return new BillLine(item.getName(), quantity, item.getUnitPrice(), item.getCategory());
    }

    public double getLineTotal() {
        return Money.round(Math.max(0, quantity) * unitPrice);
    }

    public String getFormattedLineTotal() {
        return Money.format(getLineTotal());
    }

    public String getFormattedUnitPrice() {
        return Money.format(unitPrice);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public ServiceCategory getCategory() {
        return category;
    }

    public void setCategory(ServiceCategory category) {
        this.category = category;
    }

    public String getCategoryLabel() {
        return category == null ? "Other" : category.getLabel();
    }
}
