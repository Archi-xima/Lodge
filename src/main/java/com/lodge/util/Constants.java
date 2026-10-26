package com.lodge.util;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Single source of truth for the values shared across the whole lodge
 * management system: application metadata, on-disk file layout, validation
 * limits, room rates and the billing / discount rules.
 *
 * <p><b>Team convention:</b> read these values from here instead of repeating
 * literals in controllers, services or tests. If a rule changes, change it once
 * — in this class.</p>
 *
 * <p>Values mirror the documented business rules in {@code README.md} and the
 * limits already enforced by {@code AuthService}, {@code MainApp} and
 * {@code model.Bill}.</p>
 */
public final class Constants {

    private Constants() {
        // constants holder
    }

    /* ==================================================================== */
    /* Application metadata                                                 */
    /* ==================================================================== */

    /** Trading name of the property the software manages. */
    public static final String APP_NAME = "SkyRidge Lodge";
    /** What the product does, shown next to the property name. */
    public static final String APP_SUBTITLE = "Billing & Management System";
    /** Full product title used in About boxes and reports. */
    public static final String APP_TITLE = APP_NAME + " " + APP_SUBTITLE;
    /** Title of the sign-in window. */
    public static final String SIGN_IN_TITLE = "Sign In — " + APP_TITLE;
    /** Bumped by the team whenever a deliverable is merged. */
    public static final String APP_VERSION = "1.0.0";
    /** Currency the whole system prices in (Zambian Kwacha). */
    public static final String CURRENCY_CODE = "ZMW";
    public static final String CURRENCY_SYMBOL = "K";

    /* ==================================================================== */
    /* File paths                                                           */
    /* ==================================================================== */

    /** Folder (under the user's home) where all persistent data lives. */
    public static final String DATA_DIR_NAME = ".lodge-billing";
    /** Absolute path of the data directory: {@code ~/.lodge-billing}. */
    public static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), DATA_DIR_NAME);

    // JSON files inside DATA_DIR (mirrors the names in service.DataStore)
    public static final String FILE_USERS = "users.json";
    public static final String FILE_ROOMS = "rooms.json";
    public static final String FILE_GUESTS = "guests.json";
    public static final String FILE_BOOKINGS = "bookings.json";
    public static final String FILE_BILLS = "bills.json";
    public static final String FILE_SERVICES = "services.json";
    public static final String FILE_AUDIT = "audit.json";

    // Log files inside DATA_DIR (see logback.xml)
    public static final String LOG_DIR_NAME = "logs";
    public static final String LOG_FILE_NAME = "lodge.log";
    public static final Path LOG_DIR = DATA_DIR.resolve(LOG_DIR_NAME);

    // Classpath resource locations (leading slash = absolute resource path)
    public static final String RESOURCE_VIEW_DIR = "/com/lodge/view";
    public static final String RESOURCE_CSS = "/com/lodge/css/style.css";
    public static final String RESOURCE_IMAGE_DIR = "/com/lodge/images";
    public static final String LOGIN_VIEW = RESOURCE_VIEW_DIR + "/login.fxml";
    public static final String MAIN_VIEW = RESOURCE_VIEW_DIR + "/main.fxml";

    /* ==================================================================== */
    /* Validation limits                                                    */
    /* ==================================================================== */

    /** Passwords must be at least this many characters (AuthService, LoginController). */
    public static final int MIN_PASSWORD_LENGTH = 8;
    /** Recommended maximum so a single field cannot swallow the form. */
    public static final int MAX_PASSWORD_LENGTH = 64;
    /** Usernames and full names must not be empty after trimming. */
    public static final int MIN_TEXT_LENGTH = 1;
    /** Longest free-text note (guest notes, booking notes, bill notes). */
    public static final int MAX_NOTES_LENGTH = 500;
    /** Simple email shape used for customer registration. */
    public static final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    /** Default sign-in window size (MainApp). */
    public static final int DEFAULT_WINDOW_WIDTH = 1080;
    public static final int DEFAULT_WINDOW_HEIGHT = 720;
    /** The window can never be shrunk below this. */
    public static final int MIN_WINDOW_WIDTH = 960;
    public static final int MIN_WINDOW_HEIGHT = 640;

    /* ==================================================================== */
    /* Room rates (per night, in Kwacha)                                    */
    /* ==================================================================== */

    public static final double RATE_STANDARD_ROOM = 350.00;
    public static final double RATE_DELUXE_ROOM = 550.00;
    public static final double RATE_FAMILY_ROOM = 750.00;
    public static final double RATE_EXECUTIVE_SUITE = 1200.00;
    public static final double RATE_VIP_COTTAGE = 1500.00;
    /** Cheapest and most expensive nightly rate — handy for validation and charts. */
    public static final double ROOM_RATE_MIN = RATE_STANDARD_ROOM;
    public static final double ROOM_RATE_MAX = RATE_VIP_COTTAGE;

    /* ==================================================================== */
    /* Discount & tax rules                                                 */
    /* ==================================================================== */

    /** Stays of this many nights or more earn the long-stay discount. */
    public static final int LONG_STAY_MIN_NIGHTS = 5;
    /** 5% off the <b>room charge only</b>; extra services are never discounted. */
    public static final double LONG_STAY_DISCOUNT_RATE = 0.05;
    /** Zambian VAT applied to the discounted subtotal. */
    public static final double VAT_RATE = 0.16;
    /** Same VAT rate as a whole percentage, for display ("16% VAT"). */
    public static final int VAT_PERCENT = 16;
}
