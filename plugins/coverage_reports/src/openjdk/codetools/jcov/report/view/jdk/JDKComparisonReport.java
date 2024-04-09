package openjdk.codetools.jcov.report.view.jdk;

import com.sun.tdk.jcov.instrument.DataRoot;
import openjdk.codetools.jcov.report.FileItems;
import openjdk.codetools.jcov.report.FileSet;
import openjdk.codetools.jcov.report.jcov.JCovCoverageComparison;
import openjdk.codetools.jcov.report.jcov.JCovMethodCoverageComparison;
import openjdk.codetools.jcov.report.source.SourceHierarchy;
import openjdk.codetools.jcov.report.view.MultiHTMLReport;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static openjdk.codetools.jcov.report.view.jdk.JDKLostKeptReport.DESCRIPTION_HTML;

public class JDKComparisonReport {
    private final static String USAGE = """
        java ... %s \\
            <left coverage> <right coverage> \\
            <source> \\
            <report directory>
        Where:
            (left|right)_coverage - JCov XML coverage files to compare
            source - JDK source hierarchies corresponding to the coverage files
            <report directory> - must not exist or be empty
    """.formatted(JDKComparisonReport.class.getName());
    private final DataRoot oldCov;
    private final DataRoot newCov;
    private final SourceHierarchy source;
    private final Path reportDir;
    private final BiFunction<Boolean, Boolean, FileItems.Quality> coloring = (o, n) -> {
        if (o)
            if (n) return FileItems.Quality.BOTH;
            else return FileItems.Quality.LEFT;
        else
        if (n) return FileItems.Quality.RIGHT;
        else return FileItems.Quality.NONE;
    };
    private final Map <FileItems.Quality, String> legend =  Map.of(
            FileItems.Quality.LEFT, "left",
            FileItems.Quality.RIGHT, "right",
            FileItems.Quality.BOTH, "both",
            FileItems.Quality.NONE, "neither");

    public JDKComparisonReport(DataRoot oldCov, DataRoot newCov, SourceHierarchy source, Path dir) {
        this.source = source;
        reportDir = dir;
        this.oldCov = oldCov;
        this.newCov = newCov;
    }

    public void report() {
        try {
            var reportLegendLink = "<a style=\"float:right\" href=\"" + DESCRIPTION_HTML + "\">What am I looking at?</a>";
            var noSourceClasses = new HashSet<String>();
            var report = new MultiHTMLReport.Builder()
                    .setSource(source)
                    .setCoverage(new JCovCoverageComparison(
                            oldCov, null,
                            newCov, this.source, coloring))
                    .setItems(new JCovMethodCoverageComparison(
                            oldCov, newCov,
                            f -> source.toFile(f),
                            legend,
                            coloring))
                    .setFolderHeader(s -> "<h1>Lost/kept coverage</h1>" + reportLegendLink)
                    .setFileHeader(s -> "<h1>Lost/kept coverage</h1>" + reportLegendLink)
                    .setTitle("Lost/kept method coverage")
                    .setFiles(new FileSet(newCov.getClasses().stream()
                            .map(dc -> {
                                var cn = dc.getFullname();
                                cn = cn.contains("$") ? cn.substring(0, cn.indexOf("$")) : cn;
                                if (!noSourceClasses.contains(cn)) {
                                    var res = source.toFile(cn.replace('.', '/') + ".java");
                                    if (res == null) noSourceClasses.add(cn);
                                    return res;
                                }
                                return null;
                            })
                            .filter(c -> c != null)
                            .collect(Collectors.toSet())))
                    .report();
            noSourceClasses.stream().sorted().forEach(cn -> System.err.println("No source file for " + cn));
            report.report(reportDir);
            MultiHTMLReport.copyToReport(JDKComparisonReport.class, "JDKComparisonReport.html",
                    DESCRIPTION_HTML, reportDir);
        } catch (Throwable e) {
            System.err.println(USAGE);
            System.exit(1);
        }
    }

    public static void main(String[] args) throws Exception {
        new JDKComparisonReport(DataRoot.read(args[0]), DataRoot.read(args[1]),
                JDKLostKeptReport.jdkSourceHierarchy(args[2]), Path.of(args[3])).report();
//        SourceHierarchy source = JDKLostKeptReport.jdkSourceHierarchy(args[2]);
//        Path repoDir = Path.of(args[3]);
//        JDKLostKeptReport.JDKComparisonReportBuilder builder = new JDKLostKeptReport.JDKComparisonReportBuilder()
//                .setOldSource(null).setNewSource(source)
//                .setDir(repoDir);
//        builder.setOldCov(DataRoot.read(args[0])).setNewCov(DataRoot.read(args[1]));
//        var legend =  Map.of(
//                FileItems.Quality.LEFT, "left",
//                FileItems.Quality.RIGHT, "right",
//                FileItems.Quality.BOTH, "both",
//                FileItems.Quality.NONE, "neither");
//        BiFunction<Boolean, Boolean, FileItems.Quality> coloring = (o, n) -> {
//            if (o)
//                if (n) return FileItems.Quality.BOTH;
//                else return FileItems.Quality.LEFT;
//            else
//            if (n) return FileItems.Quality.RIGHT;
//            else return FileItems.Quality.NONE;
//        };
//        builder.setItems(new JCovMethodCoverageComparison(
//                builder.getOldCov(), builder.getNewCov(),
//                f -> builder.getNewSource().toFile(f),
//                legend,
//                coloring));
//        builder.setComparison(new JCovCoverageComparison(
//                builder.getOldCov(), builder.getOldSource(),
//                builder.getNewCov(), builder.getNewSource(),
//                coloring));
//        builder.createJDKComparisonReport().report();
//        MultiHTMLReport.copyToReport(JDKComparisonReport.class, "/test/CompRep.html",
//                DESCRIPTION_HTML, repoDir);
    }
}
