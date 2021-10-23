package openjdk.jcov.data.arguments.jreinstr.filepermission;

import openjdk.jcov.data.Env;
import openjdk.jcov.data.JREInstr;
import openjdk.jcov.data.arguments.runtime.Coverage;
import openjdk.jcov.data.arguments.runtime.Saver;
import openjdk.jcov.data.lib.Util;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static openjdk.jcov.data.Instrument.JCOV_TEMPLATE;
import static openjdk.jcov.data.arguments.instrument.Plugin.METHOD_FILTER;
import static openjdk.jcov.data.arguments.runtime.Collect.TEMPLATE_FILE;
import static openjdk.jcov.data.arguments.runtime.Saver.RESULT_FILE;
import static openjdk.jcov.data.arguments.runtime.Saver.SERIALIZER;
import static openjdk.jcov.data.lib.Util.copyJRE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Listeners({Test.StatusListener.class})
public class Test {
    private Path data_dir;
    private Path template;
    private Path result;
    private Path jcov_template;
    private Path jcov_result;
    private Path jre;
    private Path main;
    private static boolean status = true;

    @BeforeClass
    public void setup() throws IOException {
        jre = copyJRE(Paths.get(System.getProperty("test.jre", System.getProperty("java.home"))));
        data_dir = Paths.get(System.getProperty("user.dir"));
        template = data_dir.resolve("template.lst");
        result = data_dir.resolve("result.lst");
        jcov_template = data_dir.resolve("template.xml");
        jcov_result = data_dir.resolve("result.xml");
        main = data_dir.resolve("Main.java");
    }
    @org.testng.annotations.Test
    public void instrument() throws IOException, InterruptedException {
        Env.properties(Map.of(
                TEMPLATE_FILE, template.toString(),
                METHOD_FILTER, PermissionMethodFilter.class.getName(),
                SERIALIZER, Serializer.class.getName()));
        Files.deleteIfExists(jcov_template);
        Files.deleteIfExists(template);
        int status = new JREInstr()
                .pluginClass(Plugin.class.getName())
                .jcovRuntime(System.getProperty("jcov.file.saver.jar"))
                .jcovTemplate(jcov_template.toString())
                .instrument(jre.toString());
        assertEquals(status, 0);
        assertTrue(Files.exists(jcov_template), "Template file: " + jcov_template);
        assertTrue(Files.exists(template), "Template file: " + template);
    }

    @org.testng.annotations.Test(dependsOnMethods = "instrument")
    public void testInstrumentation() throws IOException, InterruptedException {
        Files.write(main, List.of(
                "package openjdk.jcov.data.arguments.jreinstr.filepermission;",
                "import java.io.FilePermission;",
                "import java.io.IOException;",
                "import java.nio.file.Files;",
                "import java.nio.file.Path;",
                "public class Main {",
                "    public static void main(String[] args) throws IOException {",
                "        Path file = Files.createTempFile(\"test\", \".txt\");",
                "        FilePermission filePermission = new FilePermission(file.toString(), \"read\");",
                "        FilePermission dirPermission = new FilePermission(file.getParent().toString() + \"/-\", \"read,write\");",
                "        System.out.println(dirPermission.implies(filePermission));",
                "        Files.delete(file);",
                "    }",
                "}"
            ));
        Files.deleteIfExists(result);
        Files.deleteIfExists(jcov_result);
        //no classpath necessary for the next call because the class is implanted
        List<String> command = List.of(
                jre.toString() + File.separator + "bin" + File.separator + "java",
                "-D" + TEMPLATE_FILE + "=" + template.toString(),
                "-D" + RESULT_FILE + "=" + result.toString(),
                "-D" + SERIALIZER + "=" + Serializer.class.getName(),
                "-Djcov.data-saver=" + Saver.class.getName(),
                main.toString());
        System.out.println(command.stream().collect(Collectors.joining(" ")));
        Process p = new ProcessBuilder()
                .directory(data_dir.toFile())
                .command(command)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .start();
        int status = p.waitFor();
        assertEquals(status, 0);
        assertTrue(Files.exists(jcov_result), "Result file: " + jcov_result);
    }
    
    @org.testng.annotations.Test(dependsOnMethods = "testInstrumentation")
    public void testCoverage() throws IOException, InterruptedException {
        Coverage cov = Coverage.read(result, a -> a);
        assertEquals(cov.coverage().get(FilePermission.class.getName().replace('.', '/'))
                .entrySet().stream().filter(e -> e.getKey().startsWith("impliesIgnoreMask"))
                .findAny().get().getValue().get(0).get(0), "read");
    }

    @AfterClass
    public void tearDown() throws IOException {
        List<Path> artifacts = List.of(template, jcov_template, result, jcov_result, jre, main);
        if(status)
            for(Path file : artifacts) Util.rfrm(file);
        else {
            System.out.println("Test failed, keeping the artifacts:");
            artifacts.forEach(System.out::println);
        }
    }

    public static class StatusListener implements ITestListener {

        @Override
        public void onTestStart(ITestResult result) { }

        @Override
        public void onTestSuccess(ITestResult result) { }

        @Override
        public void onTestFailure(ITestResult result) {
            status = false;
        }

        @Override
        public void onTestSkipped(ITestResult result) { }

        @Override
        public void onTestFailedButWithinSuccessPercentage(ITestResult result) { }

        @Override
        public void onStart(ITestContext context) { }

        @Override
        public void onFinish(ITestContext context) { }
    }
}
