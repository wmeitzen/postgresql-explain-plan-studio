package com.williammeitzen.peps.gui;

public interface ZoomToolbarListener {
	public void zoomPlusEventOccurred();
	public void zoomMinusEventOccurred();
	public void zoomFitEventOccurred();
	public void zoom100EventOccurred();
	/** Absolute zoom percentage from the vertical zoom slider (typically 10–400). */
	public void zoomPercentEventOccurred(int zoomPercentage);
}
