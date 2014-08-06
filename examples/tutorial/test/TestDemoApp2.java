import com.oracle.demoapp.Plane;

public class TestDemoApp2{

    public static void main(String[] args){
        Plane plane = new Plane();

        if (plane.countGreenFiguresArea() != 0) {
            throw new RuntimeException("TestDemoApp2 test failed!");
        } else {
            System.out.println("TestDemoApp2 test passed");
        }

    }

}