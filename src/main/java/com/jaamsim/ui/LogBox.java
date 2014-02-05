package com.jaamsim.ui;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class LogBox extends FrameBox {

	private static final int LOGBOX_WIDTH = 800;
	private static final int LOGBOX_HEIGHT = 600;

	private static LogBox myInstance;

	private static Object logLock = new Object();
	private static StringBuilder logBuilder = new StringBuilder();

	private JTextArea logArea;

	public LogBox() {
		super( "Log Viewer" );
		setDefaultCloseOperation(FrameBox.DISPOSE_ON_CLOSE);

		synchronized(logLock) {
			logArea = new JTextArea(logBuilder.toString());
			logArea.setEditable(false);
		}

		JScrollPane scrollPane = new JScrollPane(logArea);

		getContentPane().add( scrollPane );

		setSize(LOGBOX_WIDTH, LOGBOX_HEIGHT);
	}

	/**
	 * Returns the only instance of the property box
	 */
	public synchronized static LogBox getInstance() {
		if (myInstance == null)
			myInstance = new LogBox();

		return myInstance;
	}

	private synchronized static void killInstance() {
		myInstance = null;
	}

	@Override
	public void dispose() {
		killInstance();
		super.dispose();
	}

	/**
	 * log a formated string, effectively wrapping String.format
	 * @param format
	 * @param args
	 */
	public static void logLinef(String format, Object... args) {
		logLine(String.format(format, args));
	}

	public static void logLine(final String logLine) {
		synchronized(logLock) {
			logBuilder.append(logLine + "\n");
		}

		System.out.println(logLine);

		// Append to the existing log area if it exists
		if (myInstance != null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					myInstance.logArea.append(logLine + "\n");
				}
			});
		}
	}

}