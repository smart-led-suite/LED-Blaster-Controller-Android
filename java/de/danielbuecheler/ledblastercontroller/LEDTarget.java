package de.danielbuecheler.ledblastercontroller;

/**
 * Created by daniel on 29.11.15.
 */
public class LEDTarget {
    String shortName;
    int targetValue;

    public LEDTarget(String shortName, int targetValue) {
        this.targetValue = targetValue;
        this.shortName = shortName;
    }

    public String getShortName() {
        return shortName;
    }

    public int getTargetValue() {
        return targetValue;
    }
}
