package model;

import java.time.Period;

/**
 * Категории товаров.
 * Содержат температурный режим и срок хранения по умолчанию.
 */
public enum Category {
    FROZEN(-18, -10, Period.ofDays(180)),
    REFRIGERATED(2, 6, Period.ofDays(14)),
    NORMAL(15, 25, Period.ofDays(365));

    private final int minTemp;
    private final int maxTemp;
    private final Period defaultShelfLife;

    Category(int minTemp, int maxTemp, Period defaultShelfLife) {
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.defaultShelfLife = defaultShelfLife;
    }

    public int getMinTemp() { return minTemp; }
    public int getMaxTemp() { return maxTemp; }
    public Period getDefaultShelfLife() { return defaultShelfLife; }
}
