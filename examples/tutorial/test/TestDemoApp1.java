import com.oracle.demoapp.Plane;
import com.oracle.demoapp.figures.Disk;
import com.oracle.demoapp.figures.Square;

public class TestDemoApp1{

    public static void main(String[] args){
        Plane plane = new Plane();
        Square square = new Square(2, "Red");
        Disk disc = new Disk(3, "Green");
        plane.addFigure(square);
        plane.addFigure(disc);

        if (plane.countRedFiguresArea() != 4) {
            throw new RuntimeException("TestDemoApp1 test failed!");
        } else {
            System.out.println("TestDemoApp1 test passed");
        }

    }

}