package neil.demo.devoxxma2017;

import java.awt.BorderLayout;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>A panel displaying time-series data, with speed on the X-axis against
 * time on the Y-axis. A new data points arrive the panel scrolls from right
 * to left.
 * </p>
 * <p><a href="http://www.jfree.org/jfreechart/">JFreeChart</a> does most of the hard work here, with its
 * {@link org.jfree.data.time.DynamicTimeSeriesCollection} data container.
 * This holds arrays of {@code float} (our data is {@code double}), one
 * for each person, that we can display in a panel.
 * </p>
 */
@SuppressWarnings("serial")
@Slf4j
public class SpeedPanel extends JPanel {

	private final DecimalFormat speedFormatter = new DecimalFormat("0.000");
    private final DynamicTimeSeriesCollection dynamicTimeSeriesCollection;
    private final float[] lastData;

    /**
     * <p>Create the speed display panel, with the provided timestamp
     * as the beginning legend on the X-axis.
     * </p>
     *
     * @param start
     */
    public SpeedPanel(long start) {
    		
    		/* Define the time-series data container, sized to hold one time-series
    		 * per input file, retaining 2000 "per second" samples for each, starting
    		 * from the given time
    		 */
    		Second sample = new Second(new Date(start));
        this.dynamicTimeSeriesCollection = new DynamicTimeSeriesCollection(Constants.FILE_NAMES_GPX.length, 2000, sample);
        this.dynamicTimeSeriesCollection.setTimeBase(sample); 
        for (int i=0; i < Constants.FILE_NAMES_GPX.length; i++) {
        		this.dynamicTimeSeriesCollection.addSeries(new float[1], i, Constants.FILE_NAMES_GPX[i]);
        }

        /* Initialize the last data points received.
         */
        this.lastData = new float[Constants.FILE_NAMES_GPX.length];

        /* Create a chart that tracks the time-series data container.
         */
        String title = Arrays.asList(Constants.FILE_NAMES_GPX).toString().toUpperCase().replaceAll(",", " v ");
        JFreeChart jFreeChart = ChartFactory.createTimeSeriesChart(title,
        									Constants.SPEEDO_PANEL_X_AXIS,
        									Constants.SPEEDO_PANEL_Y_AXIS,
        									this.dynamicTimeSeriesCollection);
        
        /* Configure axises/axes, to autoscale to fit at 5m/s speed
         * amd 10 minutes (in milliseconds) initially
         */
        ValueAxis yAxis = jFreeChart.getXYPlot().getRangeAxis();
        yAxis.setAutoRange(true);
        yAxis.setFixedAutoRange(5);
        ValueAxis xAxis = jFreeChart.getXYPlot().getDomainAxis();
        xAxis.setAutoRange(true);
        xAxis.setFixedAutoRange(10 * 60 *1000);

        /* Add all displayed components to the panel. As only one the layout
         * of the components is not challenging.
         */
        this.setLayout(new BorderLayout());
        this.add(new ChartPanel(jFreeChart));
                
    }
	
    /**
     * <p>A datapoint has been received, update the panel. We hold a
     * collection of the last datapoint for each file, and update the
     * one that has been changed.
     * </p>
     * 
     * @param name Which source file
     * @param metresPerSecond for Y-axis
     * @param time of each measurement
     */
	public void update(String name, double metresPerSecond, long time) {
		log.trace("{} => {}m/s => {}", name, this.speedFormatter.format(metresPerSecond), new Date(time));
		
		// Update the stored set of observations
		for (int i = 0 ; i < Constants.FILE_NAMES_GPX.length ; i++) {
			if (Constants.FILE_NAMES_GPX[i].equals(name)) {
				this.lastData[i] = new Float(metresPerSecond);
			}
		}

		// Update the panel with a new set of observations
        this.dynamicTimeSeriesCollection.advanceTime();
        this.dynamicTimeSeriesCollection.appendData(this.lastData);
	}

}
