package com.wasted.domesurvival.forge.airlock.gate;

import net.minecraft.util.StringRepresentable;

public enum AirlockGatePart implements StringRepresentable {
    P00(0, 0, "p00"),
    P10(1, 0, "p10"),
    P20(2, 0, "p20"),
    P30(3, 0, "p30"),
    P40(4, 0, "p40"),
    P01(0, 1, "p01"),
    P11(1, 1, "p11"),
    P21(2, 1, "p21"),
    P31(3, 1, "p31"),
    P41(4, 1, "p41"),
    P02(0, 2, "p02"),
    P12(1, 2, "p12"),
    P22(2, 2, "p22"),
    P32(3, 2, "p32"),
    P42(4, 2, "p42"),
    P03(0, 3, "p03"),
    P13(1, 3, "p13"),
    P23(2, 3, "p23"),
    P33(3, 3, "p33"),
    P43(4, 3, "p43"),
    P04(0, 4, "p04"),
    P14(1, 4, "p14"),
    P24(2, 4, "p24"),
    P34(3, 4, "p34"),
    P44(4, 4, "p44");

    private final int column;
    private final int row;
    private final String serializedName;

    AirlockGatePart(int column, int row, String serializedName) {
        this.column = column;
        this.row = row;
        this.serializedName = serializedName;
    }

    public int column() {
        return column;
    }

    public int row() {
        return row;
    }

    public static AirlockGatePart at(int column, int row) {
        if (column < 0 || column >= 5 || row < 0 || row >= 5) {
            return P22;
        }
        return values()[row * 5 + column];
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
