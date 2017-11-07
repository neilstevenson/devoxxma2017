package neil.demo.devoxxma2017;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

/**
 * <p>Define the GPX data.
 * </p>
 * <p>Sections in the input, such as "{@code <metadata>}" that are not defined here
 * are ignored. Only define the parts we want, the GPS points themselves.
 * <p>
 * <p>All fields here are treated as Strings, even though some are numerics.
 * </p>
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "gpx", namespace = Gpx.NAMESPACE)
public class Gpx {
        public static final String NAMESPACE = "http://www.topografix.com/GPX/1/1";

        @XmlElement(name = "trk", namespace = NAMESPACE)
        private Trk trk;

        /**
         * <p>In the "{@code <trk>}" section we only care for the
         * "{@code <trkseg>}".
         * </p>
         */
        @Data
        @XmlAccessorType(XmlAccessType.FIELD)
        public static class Trk {
                @XmlElement(name = "trkseg", namespace = NAMESPACE)
                private TrkSeg trkseg;
        }

        /**
         * <p>In the "{@code <trkseg>}" section we find the "{@code <trkpt>}"
         * section recurring.
         * </p>
         */
        @Data
        @XmlAccessorType(XmlAccessType.FIELD)
        public static class TrkSeg {
                @XmlElement(name = "trkpt", namespace = NAMESPACE)
                private List<TrkPt> trkpt;
        }

        /**
         * <p>One tracking point, "{@code <trkpt>}" has latitude,
         * longitude, elevation and a timestamp.
         * </p>
         */
        @Data
        @XmlAccessorType(XmlAccessType.FIELD)
        public static class TrkPt implements Serializable {
                private static final long serialVersionUID = 1L;
                
                @XmlAttribute(name = "lat")
                private String latitude;
                @XmlAttribute(name = "lon")
                private String longitude;
                @XmlElement(name = "ele", namespace = NAMESPACE)
                private String elevation;
                @XmlElement(name = "time", namespace = NAMESPACE)
                protected String time;
        }

}
