package com.oracle.demoapp;

import com.oracle.demoapp.figures.abstractpack.Figure;

import java.util.ArrayList;

public class Plane {

    ArrayList<Figure> figures;

    public Plane(){
        figures = new ArrayList<Figure>();
    }

    public void addFigure(Figure figure){
        figures.add(figure);
    }

    public double countRedFiguresArea(){
        double result = 0;
        for (Figure figure : figures){
            if ("red".equals(figure.getColorName().toLowerCase())){
                result+=figure.getArea();
            }
        }
        return result;
    }

    public double countGreenFiguresArea(){
        double result = 0;
        for (Figure figure : figures){
            if ("green".equals(figure.getColorName().toLowerCase())){
                result+=figure.getArea();
            }
        }
        return result;
    }

}
