package openjdk.jcov.data.arguments.analysis;

import openjdk.jcov.data.Instrument;

import static openjdk.jcov.data.arguments.instrument.Plugin.ARGUMENTS_PREFIX;

public class Reader {
    public static final String DESERIALIZER =
            Instrument.JCOV_DATA_ENV_PREFIX + ARGUMENTS_PREFIX + ".deserializer";
}
