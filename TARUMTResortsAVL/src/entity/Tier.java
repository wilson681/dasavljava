/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package entity;

public enum Tier {
    SILVER(1),
    GOLD(2),
    ELITE(3),
    PLATINUM(4),
    DIAMOND(5);

    private final int rank;

    Tier(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public boolean isVip() {
        return rank >= 3;
    }
}
