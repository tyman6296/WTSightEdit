package com.ruegnerlukas.wtsights.ui.main;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import com.ruegnerlukas.simpleutils.logging.logger.Logger;
import com.ruegnerlukas.wtsights.WTSights;
import com.ruegnerlukas.wtsights.data.DataLoader;
import com.ruegnerlukas.wtsights.data.calibration.CalibrationData;
import com.ruegnerlukas.wtsights.ui.Workflow;
import com.ruegnerlukas.wtsights.ui.Workflow.Step;
import com.ruegnerlukas.wtsights.ui.about.UIAbout;
import com.ruegnerlukas.wtsights.ui.calibrationeditor.UICalibrationEditor;
import com.ruegnerlukas.wtsights.ui.calibrationselect.UICalibrationSelect;
import com.ruegnerlukas.wtsights.ui.screenshotupload.UIScreenshotUpload;
import com.ruegnerlukas.wtsights.ui.vehicleselection.UIVehicleSelect;
import com.ruegnerlukas.wtutils.Config2;
import com.ruegnerlukas.wtutils.FXUtils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class UIMainMenu {


	@FXML private ResourceBundle resources;
	@FXML private URL location;
	
	@FXML private AnchorPane contentPane;
	@FXML private Label labelVersion;
	
	
	
	
	public static void openNew(Stage stage) {
		Logger.get().info("Navigate to 'MainMenu'");
		Object[] sceneObjects = FXUtils.openFXScene(stage, "/ui/layout_main.fxml", 500, 550, "WT Sight Editor");
		UIMainMenu controller = (UIMainMenu)sceneObjects[0];
		controller.create(stage);
	}

	
	
	
	private void create(Stage stage) {
		labelVersion.setText("Version " + Config2.build_version);
	}
	
	


	@FXML
	void onNewCalibration(ActionEvent event) {
		Workflow.steps.clear();
		Workflow.steps.add(Step.CREATE_CALIBRATION);
		UIVehicleSelect.openNew();
	}




	@FXML
	void onLoadCalibration(ActionEvent event) {
		Workflow.steps.clear();
		Workflow.steps.add(Step.LOAD_CALIBRATION);

		FileChooser fc = new FileChooser();
		fc.setTitle("Open Calibration");
		fc.getExtensionFilters().add(new ExtensionFilter("Calibration (*.xml)", "*.xml"));

		File fileCalib = fc.showOpenDialog(WTSights.getPrimaryStage());
		if (fileCalib != null) {
			CalibrationData data = DataLoader.loadExternalCalibFile(fileCalib);
			UICalibrationEditor.openNew(data);
		}
		
	}




	@FXML
	void onNewSight(ActionEvent event) {
		Workflow.steps.clear();
		Workflow.steps.add(Step.CREATE_SIGHT);
		UICalibrationSelect.openNew();
	}




	@FXML
	void onLoadSight(ActionEvent event) {
		Workflow.steps.clear();
		Workflow.steps.add(Step.LOAD_SIGHT);
		
		FileChooser fc = new FileChooser();
		fc.setTitle("Open Sight");
		fc.getExtensionFilters().add(new ExtensionFilter("Sight (*.blk)", "*.blk"));

		File fileSight = fc.showOpenDialog(WTSights.getPrimaryStage());
		if (fileSight != null) {
			UICalibrationSelect.openNew(fileSight);
		}
	}


	
	
	@FXML
	void onSettings(ActionEvent event) {
	}

	
	

	@FXML
	void onAbout(ActionEvent event) {
		Workflow.steps.clear();
		Workflow.steps.add(Step.ABOUT);
		UIAbout.openNew();
	}




	@FXML
	void onHelp(ActionEvent event) {
	}


}