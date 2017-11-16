package neil.demo.devoxxma2017;

import javax.swing.JFrame;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryUpdatedListener;

/**
 * <P>A map listener than runs on the client side, listening
 * for changes on the server side. So you can listen to
 * events occurring anywhere in the grid.
 * </p>
 * <p>Take the incoming event, and pass it to {@link SpeedPanel}
 * to update the displayed panel on screen.
 * </p>
 * <p>We listen here only for updates not updated and creation,
 * as we are interested in the speed changing -- acceleration
 * or deceleration.
 * </p>
 */
public class SpeedPanelListener implements EntryUpdatedListener<String, Speed> {

	// Singleton, although there can be multiple listener threads
	private static SpeedPanel speedPanel = null;
	
	/**
	 * <p>Create a panel in a window frame, and size ("<i>pack</i>")
	 * the window to fit around it. The panel will be blank til
	 * data starts arriving.
	 * </p>
	 */
	public void activateDisplay(long start) {
		speedPanel = new SpeedPanel(start);
		
		JFrame frame = new JFrame(Constants.SPEEDO_PANEL_TITLE);
		
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(speedPanel);
        frame.pack();
        frame.setVisible(true);
	}
	

	/**
	 * <p>Pass the revelant parts of the map event to the display panel.
	 * </p>
	 * <p>The first point to arrive initializes the panel with the
	 * timestamp.
	 * </p>
	 */
	@Override
	public void entryUpdated(EntryEvent<String, Speed> entryEvent) {
		synchronized (this) {
			if (speedPanel==null) {
				this.activateDisplay(entryEvent.getValue().getTime());
			}
		}
		speedPanel.update(entryEvent.getKey(), entryEvent.getValue().getMetresPerSecond(), entryEvent.getValue().getTime());
	}

}
