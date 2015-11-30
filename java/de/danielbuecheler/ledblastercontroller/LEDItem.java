package de.danielbuecheler.ledblastercontroller;

public class LEDItem {
    private String name;
    private String shortName;
    private String description;

    private int color;

    public LEDItem(String shortName, String name, int color) {
        this.name = name;
        this.shortName = shortName;
        this.color = color;
    }

    public LEDItem(String shortName, String name, String description, int color) {
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.color = color;

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

    public int getColor() {
        return color;
    }
}
