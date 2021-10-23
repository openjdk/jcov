package openjdk.jcov.data.arguments.jreinstr.filepermission;

import openjdk.jcov.data.Env;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static java.util.stream.Collectors.toMap;

public class Main {
    public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        System.getProperties().storeToXML(System.out, "");
        System.out.println("" + Env.getSPIEnv(openjdk.jcov.data.arguments.runtime.Saver.SERIALIZER, null));
        System.getProperties().entrySet().stream()
                .forEach(e -> System.out.println(e.getKey() + " -> " + e.getValue()));
        System.out.println(openjdk.jcov.data.arguments.runtime.Saver.SERIALIZER + " = " + System.getProperties().entrySet().stream()
                .collect(toMap(Object::toString, Object::toString)).get(openjdk.jcov.data.arguments.runtime.Saver.SERIALIZER));
        Path file = Files.createTempFile("test", ".txt");
        FilePermission filePermission = new FilePermission(file.toString(), "read");
        FilePermission dirPermission = new FilePermission(file.getParent().toString() + "/-", "read,write");
        System.out.println(dirPermission.implies(filePermission));
        Files.delete(file);
    }
}
