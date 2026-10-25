package com.lodge.model;

import com.lodge.util.JsonAdapters;
import com.lodge.util.Money;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A guest bill (invoice).
 *
 * <h2>Pricing rules</h2>
 * <ul>
 *   <li>Room charge = nightly rate x nights.</li>
 *   <li>Stays of {@value #LONG_STAY_MIN_NIGHTS} nights or more earn
 *       {@code LONG_STAY_DISCOUNT_RATE} off the <b>room charge only</b>.</li>
 *   <li>Extra services are added at full price (never discounted).</li>
 *   <li>16% VAT is applied to the discounted subtotal.</li>
 * </ul>
 */
public class Bill {

    /** Business rule: stays of 5 nights or more earn 5% off the room charge. */
    public static final int LONG_STAY_MIN_NIGHTS = 5;
    public static final double LONG_STAY_DISCOUNT_RATE = 0.05;
    /** Zambia VAT rate applied to the subtotal. */
    public static final double VAT_RATE = 0.16;

    private String billId;
    private String bookingId;
    private String guestName;
    private String phone;
    private String roomNumber;
    private String roomTypeName;
    private int nights;
    private double nightlyRate;
    private double roomCharge;
    private double longStayDiscount;
    private List<BillLine> lines = new ArrayList<>();
    private List<Payment> payments = new ArrayList<>();
    private double subtotal;
    private double taxAmount;
    private double total;
    private double amountPaid;
    private double balance;
    private BillStatus status;
    private String notes;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDate dueDate;

    public Bill() {
        this.createdAt = LocalDateTime.now();
        this.status = BillStatus.UNPAID;
        this.notes = "";
    }

    /** A bill produced from a reservation at check-out. */
    public static Bill fromBooking(Booking booking, String billId, String createdBy) {
        Bill bill = new Bill();
        bill.billId = billId;
        bill.bookingId = booking.getBookingId();
        bill.guestName = booking.getGuestName();
        bill.phone = booking.getPhone();
        bill.roomNumber = booking.getRoomNumber();
        bill.roomTypeName = booking.getRoomTypeName();
        bill.nights = booking.getNights();
        bill.nightlyRate = booking.getNightlyRate();
        bill.notes = booking.getNotes();
        bill.createdBy = createdBy;
        bill.recalculate();
        return bill;
    }

    /** Convenience factory for a walk-in guest (no reservation). */
    public static Bill walkIn(String billId, String guestName, String phone, Room room,
                              int nights, double extrasAmount, String notes, String createdBy) {
        Bill bill = new Bill();
        bill.billId = billId;
        bill.guestName = guestName;
        bill.phone = phone;
        if (room != null) {
            bill.roomNumber = room.getRoomNumber();
            bill.roomTypeName = room.getTypeName();
            bill.nightlyRate = room.getEffectiveRate();
        }
        bill.nights = nights;
        bill.notes = notes;
        bill.createdBy = createdBy;
        if (extrasAmount > 0) {
            bill.lines.add(new BillLine("Other extras", 1, extrasAmount, ServiceCategory.DINING));
        }
        bill.recalculate();
        return bill;
    }

    /** Recomputes every derived amount from the base inputs. */
    public void recalculate() {
        this.roomCharge = Money.round(nightlyRate * Math.max(0, nights));
        this.longStayDiscount = nights >= LONG_STAY_MIN_NIGHTS
                ? Money.round(roomCharge * LONG_STAY_DISCOUNT_RATE)
                : 0.0;

        double extras = 0.0;
        for (BillLine line : lines) {
            extras += line.getLineTotal();
        }
        this.subtotal = Money.round(roomCharge - longStayDiscount + extras);
        this.taxAmount = Money.round(subtotal * VAT_RATE);
        this.total = Money.round(subtotal + taxAmount);

        double paid = 0.0;
        for (Payment payment : payments) {
            paid += payment.getAmount();
        }
        this.amountPaid = Money.round(paid);
        this.balance = Money.round(total - amountPaid);

        if (status != BillStatus.VOID) {
            if (amountPaid <= 0.009) {
                this.status = BillStatus.UNPAID;
            } else if (balance <= 0.009) {
                this.status = BillStatus.PAID;
            } else {
                this.status = BillStatus.PART_PAID;
            }
        }
    }

    public void addLine(BillLine line) {
        lines.add(line);
        recalculate();
    }

    public boolean removeLine(BillLine line) {
        boolean removed = lines.remove(line);
        if (removed) {
            recalculate();
        }
        return removed;
    }

    public void addPayment(Payment payment) {
        payments.add(payment);
        recalculate();
    }

    public void voidBill() {
        this.status = BillStatus.VOID;
    }

    public boolean isPaidInFull() {
        return status == BillStatus.PAID;
    }

    public double getExtrasTotal() {
        double extras = 0.0;
        for (BillLine line : lines) {
            extras += line.getLineTotal();
        }
        return Money.round(extras);
    }

    // ------------------------------------------------------------------ getters / setters

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public int getNights() {
        return nights;
    }

    public void setNights(int nights) {
        this.nights = nights;
        recalculate();
    }

    public double getNightlyRate() {
        return nightlyRate;
    }

    public void setNightlyRate(double nightlyRate) {
        this.nightlyRate = nightlyRate;
        recalculate();
    }

    public double getRoomCharge() {
        return roomCharge;
    }

    public double getLongStayDiscount() {
        return longStayDiscount;
    }

    public List<BillLine> getLines() {
        return lines;
    }

    public void setLines(List<BillLine> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
        recalculate();
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments != null ? payments : new ArrayList<>();
        recalculate();
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public double getTotal() {
        return total;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public double getBalance() {
        return balance;
    }

    public BillStatus getStatus() {
        return status;
    }

    public void setStatus(BillStatus status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return status == null ? "" : status.getLabel();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** Existing invoices are payable on their issue date unless terms are recorded. */
    public LocalDate getDueDate() {
        return dueDate != null ? dueDate : (createdAt == null ? null : createdAt.toLocalDate());
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    // ------------------------------------------------------------------ formatted strings

    public String getFormattedNightlyRate() {
        return Money.format(nightlyRate);
    }

    public String getFormattedRoomCharge() {
        return Money.format(roomCharge);
    }

    public String getFormattedLongStayDiscount() {
        return "- " + Money.format(longStayDiscount);
    }

    public String getFormattedExtras() {
        return Money.format(getExtrasTotal());
    }

    public String getFormattedSubtotal() {
        return Money.format(subtotal);
    }

    public String getFormattedTax() {
        return Money.format(taxAmount);
    }

    public String getFormattedTotal() {
        return Money.format(total);
    }

    public String getFormattedPaid() {
        return Money.format(amountPaid);
    }

    public String getFormattedBalance() {
        return Money.format(balance);
    }

    public String getFormattedDate() {
        return createdAt == null ? "" : createdAt.format(JsonAdapters.DATE_TIME);
    }

    @Override
    public String toString() {
        return billId + " — " + guestName + " (" + getFormattedTotal() + ")";
    }
}
