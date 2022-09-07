package openjdk.jcov.data.arguments.jreinstr.filepermission;

import openjdk.jcov.data.JREInstr;
import openjdk.jcov.data.arguments.runtime.Coverage;
import openjdk.jcov.data.arguments.runtime.Saver;
import openjdk.jcov.data.lib.TestStatusListener;
import openjdk.jcov.data.lib.Util;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static openjdk.jcov.data.arguments.instrument.Plugin.METHOD_FILTER;
import static openjdk.jcov.data.arguments.runtime.Collect.COVERAGE_OUT;
import static openjdk.jcov.data.arguments.runtime.Collect.SERIALIZER;
import static openjdk.jcov.data.lib.Util.copyJRE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Listeners({TestStatusListener.class})
public class Test {
    private Path data_dir;
    private Path template;
    private Path jcov_template;
    private Path jcov_result;
    private Path jre;
    private Path main;
    private FileTime templateCreated;

    @BeforeClass
    public void setup() throws IOException {
        jre = copyJRE(Paths.get(System.getProperty("test.jre", System.getProperty("java.home"))));
        data_dir = Paths.get(System.getProperty("user.dir"));
        template = data_dir.resolve("template.lst");
        jcov_template = data_dir.resolve("template.xml");
        jcov_result = data_dir.resolve("result.xml");
        main = data_dir.resolve("Main.java");
    }
    @org.testng.annotations.Test
    public void instrument() throws IOException, InterruptedException {
        Files.deleteIfExists(jcov_template);
        Files.deleteIfExists(template);
        String runtime = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .filter(s -> s.endsWith("jcov_file_saver.jar")).findAny().get();
        int status = new JREInstr()
                .clearEnv()
                .setEnv(Map.of(
                        COVERAGE_OUT, template.toString(),
                        METHOD_FILTER, PermissionMethodFilter.class.getName(),
                        SERIALIZER, Serializer.class.getName()))
                .pluginClass(Plugin.class.getName())
                .jcovRuntime(runtime)
                .jcovTemplate(jcov_template.toString())
                .instrument(jre.toString());
        assertEquals(status, 0);
        assertTrue(Files.exists(jcov_template), "Template file: " + jcov_template);
        assertTrue(Files.exists(template), "Template file: " + template);
        templateCreated = Files.readAttributes(template, BasicFileAttributes.class).lastModifiedTime();
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
        Files.deleteIfExists(jcov_result);
        //no classpath necessary for the next call because the class is implanted
        List<String> command = List.of(
                jre.toString() + File.separator + "bin" + File.separator + "java",
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
        assertTrue(Files.readAttributes(template, BasicFileAttributes.class).lastModifiedTime()
                .compareTo(templateCreated) > 0);
    }

    @org.testng.annotations.Test(dependsOnMethods = "testInstrumentation")
    public void testCoverage() throws IOException, InterruptedException {
        Coverage cov = Coverage.read(template, a -> a);
        assertEquals(cov.coverage().get(FilePermission.class.getName().replace('.', '/'))
                .entrySet().stream().filter(e -> e.getKey().startsWith("impliesIgnoreMask"))
                .findAny().get().getValue().get(0).get(0), "read");
    }

    @AfterClass
    public void tearDown() throws IOException {
        List<Path> artifacts = List.of(template, jcov_template, template, jcov_result, jre, main);
        if(TestStatusListener.status)
            for(Path file : artifacts) Util.rfrm(file);
        else {
            System.out.println("Test failed, keeping the artifacts:");
            artifacts.forEach(System.out::println);
        }
    }
}
