package com.lodge.service;

import com.lodge.model.*;
import com.lodge.util.Money;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Read-only analytics used by the dashboard and the reports screen.
 */
public class ReportService {

    private static ReportService instance;

    private ReportService() {
    }

    public static synchronized ReportService getInstance() {
        if (instance == null) {
            instance = new ReportService();
        }
        return instance;
    }

    // ------------------------------------------------------------------ occupancy

    public long totalRooms() {
        return RoomService.getInstance().getRooms().size();
    }

    public long occupiedRooms() {
        return RoomService.getInstance().countWithStatus(RoomStatus.OCCUPIED);
    }

    public long availableRooms() {
        return RoomService.getInstance().countWithStatus(RoomStatus.AVAILABLE);
    }

    /** Occupancy as a percentage, 0 when there are no rooms at all. */
    public double occupancyRate() {
        long total = totalRooms();
        return total == 0 ? 0 : Math.round(occupiedRooms() * 1000.0 / total) / 10.0;
    }

    /** Occupied rooms broken down by room type. */
    public Map<RoomType, Long> occupancyByType() {
        Map<RoomType, Long> map = new EnumMap<>(RoomType.class);
        for (Room room : RoomService.getInstance().getRooms()) {
            map.merge(room.getType(), 1L, Long::sum);
        }
        return map;
    }

    // ------------------------------------------------------------------ money

    public double revenueToday() {
        double sum = 0;
        for (Bill bill : BillingService.getInstance().billsCreatedOn(LocalDate.now())) {
            if (bill.getStatus() != BillStatus.VOID) {
                sum += bill.getAmountPaid();
            }
        }
        return Money.round(sum);
    }

    public double outstandingBalance() {
        return BillingService.getInstance().totalOutstanding();
    }

    /** Room revenue billed in a month, grouped by room type. */
    public Map<RoomType, Double> billedByRoomType(YearMonth month) {
        Map<RoomType, Double> map = new EnumMap<>(RoomType.class);
        for (Bill bill : BillingService.getInstance().getBills()) {
            if (bill.getStatus() == BillStatus.VOID || bill.getCreatedAt() == null
                    || !YearMonth.from(bill.getCreatedAt()).equals(month)) {
                continue;
            }
            RoomType type = RoomType.fromDisplayName(bill.getRoomTypeName());
            map.merge(type, bill.getTotal(), Double::sum);
        }
        Map<RoomType, Double> rounded = new EnumMap<>(RoomType.class);
        map.forEach((type, value) -> rounded.put(type, Money.round(value)));
        return rounded;
    }

    /** Amounts received in a month grouped by payment method. */
    public Map<PaymentMethod, Double> paymentsByMethod(YearMonth month) {
        Map<PaymentMethod, Double> map = new EnumMap<>(PaymentMethod.class);
        for (Bill bill : BillingService.getInstance().getBills()) {
            for (Payment payment : bill.getPayments()) {
                if (payment.getTimestamp() != null && YearMonth.from(payment.getTimestamp()).equals(month)) {
                    map.merge(payment.getMethod(), payment.getAmount(), Double::sum);
                }
            }
        }
        Map<PaymentMethod, Double> rounded = new EnumMap<>(PaymentMethod.class);
        map.forEach((method, value) -> rounded.put(method, Money.round(value)));
        return rounded;
    }

    /** Billed vs collected totals for the last {@code months} months. */
    public List<MonthlySummary> monthlySummaries(int months) {
        List<MonthlySummary> list = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            list.add(new MonthlySummary(month,
                    BillingService.getInstance().billedInMonth(month),
                    BillingService.getInstance().revenueInMonth(month)));
        }
        return list;
    }

    /** Best selling extras across all bills. */
    public List<ServiceSummary> topServices(int limit) {
        Map<String, ServiceSummary> map = new LinkedHashMap<>();
        for (Bill bill : BillingService.getInstance().getBills()) {
            for (BillLine line : bill.getLines()) {
                ServiceSummary summary = map.computeIfAbsent(line.getDescription(),
                        name -> new ServiceSummary(name, line.getCategoryLabel()));
                summary.quantity += line.getQuantity();
                summary.revenue += line.getLineTotal();
            }
        }
        List<ServiceSummary> list = new ArrayList<>(map.values());
        list.sort((a, b) -> Double.compare(b.revenue, a.revenue));
        return list.size() > limit ? new ArrayList<>(list.subList(0, limit)) : list;
    }

    // ------------------------------------------------------------------ value types

    public static class MonthlySummary {
        public final YearMonth month;
        public final double billed;
        public final double collected;

        public MonthlySummary(YearMonth month, double billed, double collected) {
            this.month = month;
            this.billed = billed;
            this.collected = collected;
        }

        public String getLabel() {
            return month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + month.getYear() % 100;
        }

        public String getFormattedBilled() {
            return Money.format(billed);
        }

        public String getFormattedCollected() {
            return Money.format(collected);
        }
    }

    public static class ServiceSummary {
        public final String name;
        public final String category;
        public int quantity;
        public double revenue;

        public ServiceSummary(String name, String category) {
            this.name = name;
            this.category = category;
        }

        public String getFormattedRevenue() {
            return Money.format(revenue);
        }
    }
}
