package neil.demo.devoxxma2017;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>An object representing a speed at a particular
 * point in time.
 * </p>
 */
@Data
@SuppressWarnings("serial")
public class Speed implements Serializable {

    private double metresPerSecond;
    private long time;

}
