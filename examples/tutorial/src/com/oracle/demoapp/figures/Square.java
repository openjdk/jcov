package com.oracle.demoapp.figures;

import com.oracle.demoapp.figures.abstractpack.Figure;

public class Square extends Figure {

    private int length;

    public Square(int l, String color){
        length = l;
        setColorName(color);
    }

    @Override
    public double getArea() {
        return length * length;
    }
}