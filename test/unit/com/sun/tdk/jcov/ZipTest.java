package com.sun.tdk.jcov;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class ZipTest {
    public static void main(String[] args) throws IOException {
        Path zip = Paths.get("/tmp/aaa.jar");
        if(Files.exists(zip)) Files.delete(zip);
        try(var out = new JarOutputStream(Files.newOutputStream(zip))) {}
        var fs = FileSystems.newFileSystem(zip, null);
        var root = fs.getRootDirectories().iterator().next();
        Path text_file = root.resolve("text.txt");
        try(var out = Files.newBufferedWriter(text_file)) {
            out.write("test\n");
        }
        fs.close();

        var fsin = FileSystems.newFileSystem(zip, null);
        var fsout = FileSystems.newFileSystem(zip, null);
        System.out.write(Files.readAllBytes(fsin.getPath("text.txt")));
        Files.write(fsout.getPath("text.txt"), "new test".getBytes());
        fsout.close();
        fsin.close();
    }
}
