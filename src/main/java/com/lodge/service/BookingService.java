package com.lodge.service;

import com.lodge.model.*;
import com.lodge.util.Money;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reservations: creating bookings, check-in, check-out (which raises the bill)
 * and cancellation. Room status is kept in sync with the booking lifecycle.
 */
public class BookingService {

    private static BookingService instance;

    private final ObservableList<Booking> bookings = FXCollections.observableArrayList();

    private BookingService() {
        bookings.setAll(DataStore.getInstance().loadBookings());
        if (bookings.isEmpty()) {
            seedBookings();
        }
    }

    public static synchronized BookingService getInstance() {
        if (instance == null) {
            instance = new BookingService();
        }
        return instance;
    }

    private void seedBookings() {
        RoomService rooms = RoomService.getInstance();
        GuestService guests = GuestService.getInstance();

        Booking b1 = createBooking(guests.findById("G001"), rooms.findByNumber("104"),
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(2), "Anniversary stay", "Reception");
        checkIn(b1);

        Booking b2 = createBooking(guests.findById("G004"), rooms.findByNumber("202"),
                LocalDate.now().minusDays(2), LocalDate.now(), "Corporate booking", "Reception");
        checkIn(b2);

        createBooking(guests.findById("G002"), rooms.findByNumber("105"),
                LocalDate.now(), LocalDate.now().plusDays(3), "Arriving this afternoon", "Reception");

        createBooking(guests.findById("G003"), rooms.findByNumber("C1"),
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(9), "Diving trip", "Reception");
    }

    public ObservableList<Booking> getBookings() {
        return bookings;
    }

    public Booking findById(String bookingId) {
        for (Booking booking : bookings) {
            if (bookingId != null && bookingId.equals(booking.getBookingId())) {
                return booking;
            }
        }
        return null;
    }

    public String nextBookingId() {
        List<String> ids = new ArrayList<>();
        for (Booking booking : bookings) {
            ids.add(booking.getBookingId());
        }
        return DataStore.getInstance().nextId("BK", ids);
    }

    /**
     * Checks whether a room is free for the requested range.
     *
     * @param excludeBooking a booking that may be re-dated (ignored in the scan)
     */
    public boolean isRoomFree(String roomNumber, LocalDate checkIn, LocalDate checkOut, Booking excludeBooking) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            return false;
        }
        for (Booking booking : bookings) {
            if (booking == excludeBooking) {
                continue;
            }
            if (!roomNumber.equalsIgnoreCase(booking.getRoomNumber())) {
                continue;
            }
            if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.CHECKED_OUT) {
                continue;
            }
            boolean overlaps = checkIn.isBefore(booking.getCheckOut()) && checkOut.isAfter(booking.getCheckIn());
            if (overlaps) {
                return false;
            }
        }
        return true;
    }

    public Booking createBooking(Guest guest, Room room, LocalDate checkIn, LocalDate checkOut,
                                 String notes, String createdBy) {
        Booking booking = new Booking(nextBookingId(), guest, room, checkIn, checkOut, notes, createdBy);
        bookings.add(booking);
        if (room.getStatus() == RoomStatus.AVAILABLE) {
            RoomService.getInstance().setStatus(room, RoomStatus.RESERVED);
        }
        save();
        AuthService.getInstance().log("BOOKING", booking.getBookingId() + " — " + guest.getFullName()
                + " in room " + room.getRoomNumber() + " (" + booking.getNights() + " nights)");
        return booking;
    }

    public void checkIn(Booking booking) {
        booking.setStatus(BookingStatus.CHECKED_IN);
        Room room = RoomService.getInstance().findByNumber(booking.getRoomNumber());
        if (room != null) {
            RoomService.getInstance().setStatus(room, RoomStatus.OCCUPIED);
        }
        save();
        AuthService.getInstance().log("CHECK IN", booking.getBookingId() + " — " + booking.getGuestName()
                + " into room " + booking.getRoomNumber());
    }

    /** Checks the guest out and raises their bill. Returns the new bill. */
    public Bill checkOut(Booking booking, double extrasAmount) {
        BillingService billing = BillingService.getInstance();
        Room room = RoomService.getInstance().findByNumber(booking.getRoomNumber());

        Bill bill = Bill.fromBooking(booking, billing.nextBillId(), AuthService.getInstance().currentUserName());
        if (extrasAmount > 0) {
            bill.addLine(new BillLine("Extras (as charged)", 1, extrasAmount, ServiceCategory.DINING));
        }
        billing.addBill(bill);

        booking.setStatus(BookingStatus.CHECKED_OUT);
        booking.setBillId(bill.getBillId());
        if (room != null) {
            RoomService.getInstance().setStatus(room, RoomStatus.AVAILABLE);
        }
        save();
        AuthService.getInstance().log("CHECK OUT", booking.getBookingId() + " — bill " + bill.getBillId()
                + " (" + Money.format(bill.getTotal()) + ")");
        return bill;
    }

    public void cancel(Booking booking) {
        booking.setStatus(BookingStatus.CANCELLED);
        Room room = RoomService.getInstance().findByNumber(booking.getRoomNumber());
        if (room != null && room.getStatus() == RoomStatus.RESERVED) {
            RoomService.getInstance().setStatus(room, RoomStatus.AVAILABLE);
        }
        save();
        AuthService.getInstance().log("CANCEL BOOKING", booking.getBookingId() + " — " + booking.getGuestName());
    }

    public boolean removeBooking(Booking booking) {
        boolean removed = bookings.remove(booking);
        if (removed) {
            save();
        }
        return removed;
    }

    public List<Booking> arrivalsOn(LocalDate date) {
        List<Booking> result = new ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.isArrivingOn(date)) {
                result.add(booking);
            }
        }
        return result;
    }

    public List<Booking> departuresOn(LocalDate date) {
        List<Booking> result = new ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.isDepartingOn(date)) {
                result.add(booking);
            }
        }
        return result;
    }

    public List<Booking> inHouse() {
        List<Booking> result = new ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.isInHouse()) {
                result.add(booking);
            }
        }
        return result;
    }

    public void save() {
        DataStore.getInstance().saveBookings(new ArrayList<>(bookings));
    }
}
