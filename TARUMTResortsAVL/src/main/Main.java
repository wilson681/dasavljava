package main;

import entity.VipPriorityKey;

public class Main {

    public static void main(String[] args) {
        VipPriorityKey dEarly = new VipPriorityKey(5, 20, "00000001");
        VipPriorityKey dLate  = new VipPriorityKey(5, 50, "00000002");
        VipPriorityKey plat   = new VipPriorityKey(4, 10, "00000003");
        VipPriorityKey elite  = new VipPriorityKey(3, 5, "00000004");
        VipPriorityKey silver = new VipPriorityKey(1, 1, "00000005");

        System.out.println("--- expect true ---");
        System.out.println("1. DIAMOND < PLATINUM : " + (dEarly.compareTo(plat) < 0));
        System.out.println("2. PLATINUM < ELITE   : " + (plat.compareTo(elite) < 0));
        System.out.println("3. ELITE < SILVER     : " + (elite.compareTo(silver) < 0));
        System.out.println("4. same tier morning < night     : " + (dEarly.compareTo(dLate) < 0));
        System.out.println("5. SILVER largest        : " + (silver.compareTo(dEarly) > 0));
    }
}