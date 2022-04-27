package openjdk.jcov.data.arguments.jreinstr.filepermission;

import openjdk.jcov.data.arguments.runtime.Collect;
import openjdk.jcov.data.arguments.runtime.Implantable;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public class Serializer implements openjdk.jcov.data.arguments.runtime.Serializer {
    @Override
    public String apply(Object o) {
        if(o instanceof FilePermission) {
            return ((FilePermission)o).getActions();
        } else return null;
    }

    @Override
    public Collection<Class> runtime() {
        return Set.of(Serializer.class,
                Implantable.class);
    }

    public static void main(String[] args) throws IOException {
        File file = File.createTempFile("test", ".txt");
        file.delete();
    }
}
