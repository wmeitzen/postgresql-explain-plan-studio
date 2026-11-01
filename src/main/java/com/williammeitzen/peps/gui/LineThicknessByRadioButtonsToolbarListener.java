package com.williammeitzen.peps.gui;

public interface LineThicknessByRadioButtonsToolbarListener {
	public void lineThicknessByActualMassEventOccurred();
	public void lineThicknessByEstimatedMassEventOccurred();
	public void lineThicknessByActualRowsEventOccurred();
	public void lineThicknessByEstimatedRowsEventOccurred();
	public void lineThicknessByActualVsEstimatedRowsRatioEventOccurred();
	public void lineThicknessByActualVsEstimatedMassRatioEventOccurred();
}
