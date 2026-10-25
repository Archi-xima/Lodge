package com.lodge.model;

import com.lodge.util.Money;

/** An item from the extras price list (meals, laundry, transfers, spa ...). */
public class ServiceItem {

    private String serviceId;
    private String name;
    private ServiceCategory category;
    private double unitPrice;
    private String unit;

    public ServiceItem() {
        this.unit = "each";
    }

    public ServiceItem(String serviceId, String name, ServiceCategory category, double unitPrice, String unit) {
        this();
        this.serviceId = serviceId;
        this.name = name;
        this.category = category;
        this.unitPrice = unitPrice;
        if (unit != null) {
            this.unit = unit;
        }
    }

    public String getFormattedPrice() {
        return Money.format(unitPrice) + " / " + unit;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServiceCategory getCategory() {
        return category;
    }

    public void setCategory(ServiceCategory category) {
        this.category = category;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        return name + " — " + getFormattedPrice();
    }
}
