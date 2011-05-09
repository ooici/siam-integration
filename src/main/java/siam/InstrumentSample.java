package siam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.mbari.siam.distributed.PacketParser;

/**
 * An ad hoc sample object to capture sample information from SIAM, in
 * particular, for the getPortLastSample operation. Here, we encapsulate both
 * the "metadata" for each sample, as well as the actual datum value. The
 * related concept in SIAM is the inner class {@link PacketParser.Field}.
 * However, that inner class is attached to the PacketParser (ie., it is a
 * *non-static* inner class.
 * 
 * @author carueda
 */
public class InstrumentSample {

    /**
     * An individual datum belonging to a sample.
     */
    public static class SampleDatum {
        final private String name;
        final private Object value;
        final private String units;

        SampleDatum(String name, Object value, String units) {
            this.name = name;
            this.value = value;
            this.units = units;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }

        public String getUnits() {
            return units;
        }

    }

    /** sample metadata */
    final private Map<String, String> md;

    /** sample data */
    private List<SampleDatum> data;

    InstrumentSample(Map<String, String> md) {
        this.md = md;
    }

    public Map<String, String> getMd() {
        return md;
    }

    public void addDatum(SampleDatum datum) {
        if (data == null) {
            data = new ArrayList<SampleDatum>();
        }
        data.add(datum);
    }

    /**
     * @return never null
     */
    public List<SampleDatum> getData() {
        if (data != null) {
            return data;
        }
        return Collections.emptyList();
    }
}
