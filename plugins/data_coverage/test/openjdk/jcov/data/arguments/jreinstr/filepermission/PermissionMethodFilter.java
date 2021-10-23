package openjdk.jcov.data.arguments.jreinstr.filepermission;

import openjdk.jcov.data.arguments.instrument.MethodFilter;

import java.io.FilePermission;

public class PermissionMethodFilter implements MethodFilter {
    @Override
    public boolean accept(int access, String owner, String name, String desc) throws Exception {
//        return false;
        return openjdk.jcov.data.arguments.instrument.MethodFilter.parseDesc(desc).stream()
                .anyMatch(td -> td.cls().equals(FilePermission.class.getName().replace('.', '/')));
    }
}
