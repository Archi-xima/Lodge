package com.lodge.controller;

/**
 * Implemented by every screen shown inside the main window so it can
 * refresh its data whenever the user navigates to it.
 */
public interface ViewController {

    /** Called each time the view becomes visible. */
    void onShown();
}
