package com.oracle.demoapp.figures;

import com.oracle.demoapp.figures.abstractpack.Figure;

public class Disk extends Figure {

    private int length;

    public Disk(int l, String color){
        length = l;
        setColorName(color);
    }

    @Override
    public double getArea() {
        return Math.PI*length*length;
    }
}