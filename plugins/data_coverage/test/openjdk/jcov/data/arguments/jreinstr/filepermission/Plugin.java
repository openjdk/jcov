package openjdk.jcov.data.arguments.jreinstr.filepermission;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class Plugin extends openjdk.jcov.data.arguments.instrument.Plugin {
    public Plugin() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        super();
    }

    @Override
    protected List<Class> runtimeClasses() {
        ArrayList<Class> result =  new ArrayList<>(super.runtimeClasses());
        result.add(Serializer.class);
        return result;
    }
}
