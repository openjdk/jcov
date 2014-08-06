package com.oracle.demoapp.figures.abstractpack;

public abstract class Figure{

    private String colorName;

    public Figure(){
    }

    public String getColorName(){
        return colorName;
    }

    public void setColorName(String colorName){
        this.colorName = colorName;
    }

    public abstract double getArea();

}