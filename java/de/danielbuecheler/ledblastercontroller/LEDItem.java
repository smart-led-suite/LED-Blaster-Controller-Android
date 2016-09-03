package de.danielbuecheler.ledblastercontroller;

import java.io.Serializable;

public class LEDItem implements Serializable {
    private String name;
    private String shortName;
    private String description;

    private String colorHex;
    private String colorHexFont;

    public LEDItem(String shortName, String name, String colorHex, String colorHexFont) {
        this.name = name;
        this.shortName = shortName;
        this.colorHex = colorHex;
        this.colorHexFont = colorHexFont;
    }

    public LEDItem(String shortName, String name, String description, String colorHex, String colorHexFont) {
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.colorHex = colorHex;
        this.colorHexFont = colorHexFont;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getDescription() {
        return description;
    }

    public String getColorHex() {
        return colorHex;
    }

    public String getColorFontHex() {
        return colorHexFont;
    }


}
