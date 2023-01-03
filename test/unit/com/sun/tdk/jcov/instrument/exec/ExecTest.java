package com.sun.tdk.jcov.instrument.exec;

import com.sun.tdk.jcov.Exec;
import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.instrument.DataPackage;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.instrument.util.Util;
import com.sun.tdk.jcov.instrument.instr.UserCode;
import com.sun.tdk.jcov.io.Reader;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ExecTest {
    Path test_dir;
    Path output_dir;
    Path result;
    Path template;
    Path log;
    Path data_dir;

    @BeforeClass
    public void clean() throws IOException {
        System.setProperty("jcov.selftest", "true");
        data_dir = Files.createTempDirectory("instr_test");
        result = data_dir.resolve("result.xml");
        template = data_dir.resolve("template.xml");
        log = data_dir.resolve("log");
        test_dir = data_dir.resolve("instr_test");
        output_dir = data_dir.resolve("instr_test_output");
        System.out.println("data dir = " + data_dir);
    }
    @Test
    public void testExec() throws IOException, FileFormatException {
        String runtime = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .peek(System.out::println)
                .filter(s -> s.endsWith("jcov_file_saver.jar")).findAny().get();
        new Util(test_dir).copyBytecode(UserCode.class.getName());
        //java -jar JCOV_BUILD/jcov_3.0/jcov.jar Exec -product cl_test -productOutput cl_test_instr -rt JCOV_BUILD/jcov_3.0/jcov_network_saver.jar -command "java -cp cl_test com.sun.tdk.jcov.instrument.instr.UserCode 1"
        String javaCommand = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java" +
                (System.getProperty("os.name").toLowerCase().contains("windows") ? ".exe" : "") +
                " -cp " + output_dir.toString() + //" -output " + result.toString()  +
                " com.sun.tdk.jcov.instrument.instr.UserCode 1";
        List<String> params = List.of("-product", test_dir.toString(), "-productOutput", output_dir.toString(),
                "-rt", runtime,
                "-output", result.toString(),
                "-t", template.toString(),
                "-out.file", log.toString(),
                "-command", javaCommand);
        System.out.println("Running Exec with");
        System.out.println(params.stream().collect(Collectors.joining(" ")));
        new Exec().run(params.toArray(new String[0]));
        DataRoot data = Reader.readXML(result.toString());
        DataPackage dp =
                data.getPackages().stream()
                        .filter(p -> p.getName().equals("com/sun/tdk/jcov/instrument/instr")).findAny().get();
        DataMethod dm = dp
                .getClasses().stream().filter(c -> c.getName().equals("UserCode")).findAny().get()
                .getMethods().stream().filter(m -> m.getName().equals("main")).findAny().get();
        assertTrue(dm.getSlot() > 0);
        assertFalse(dp
                .getClasses().stream().filter(c -> c.getName().equals("InstrTest")).findAny().isPresent());
    }
    @AfterClass
    public void tearDown() throws IOException {
        Util.rmRF(data_dir);
    }
}
