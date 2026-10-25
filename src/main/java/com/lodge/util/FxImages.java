package com.lodge.util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;

/** Loads the bundled lodge photographs into JavaFX image views. */
public final class FxImages {

    private FxImages() {
    }

    public static Image load(String resourcePath) {
        InputStream stream = FxImages.class.getClassLoader().getResourceAsStream(resourcePath);
        return stream == null ? null : new Image(stream);
    }

    /** Creates an image view that fills the given box while keeping proportions. */
    public static ImageView box(String resourcePath, double width, double height) {
        ImageView view = new ImageView();
        view.setFitWidth(width);
        view.setFitHeight(height);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.getStyleClass().add("photo");
        Image image = load(resourcePath);
        if (image != null) {
            view.setImage(image);
        }
        return view;
    }
}
