package com.ruegnerlukas.wtutils;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;

import com.ruegnerlukas.simpleutils.logging.logger.Logger;
import com.ruegnerlukas.wtsights.WTSights;
import com.ruegnerlukas.wtsights.ui.main.UIMainMenu;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class FXUtils {


	static class StringConverterInt extends StringConverter<Integer> {
		
		@Override
		public String toString(Integer value) {
			if (value == null) {
				return "";
				}
			return ""+value.intValue();
		}
		
		@Override
		public Integer fromString(String value) {
			try {
				if (value == null) {
					return 0;
					}
				value = value.trim();
				if (value.length() < 1) {
					return 0;
				}
				return Integer.parseInt(value);
			} catch (Exception ex) {
				return 0;
			}
		}
		
	}
	
	
	static class StringConverterDouble extends StringConverter<Double> {

		private final DecimalFormat format;
		private final double defaultValue;
		
		
		public StringConverterDouble(int decPlaces) {
			this(decPlaces, 0.0);
		}
		
		public StringConverterDouble(int decPlaces, double defaultValue) {
			String strDecPlaces = "";
			for(int i=0; i<decPlaces; i++) {
				strDecPlaces += "#";
			}
			format = new DecimalFormat("#" + (strDecPlaces.isEmpty() ? "" : ".") + strDecPlaces);
			this.defaultValue = defaultValue;
		}
		
		
		@Override
		public String toString(Double value) {
			if(value == null) {
				return "NaN";
			} else {
				return format.format(value);
			}
		}

		@Override
		public Double fromString(String value) {
			try {
				
				if(value ==  null) {
					return defaultValue;
				} else {
					value = value.trim();
					if(value.length() < 1) {
						return defaultValue;
					} else {
						return format.parse(value).doubleValue();
					}
				}
				
			} catch(ParseException e) {
				return defaultValue;
			}
		}
		
	}
	
	
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void initSpinner(Spinner<?> spinner, double defaultValue, double min, double max, double step, int decPlaces, ChangeListener listener) {
		if(decPlaces <= 0) {
			SpinnerValueFactory valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory((int)min, (int)max, (int)defaultValue, (int)step);
			valueFactory.setConverter(new StringConverterInt());
			spinner.setValueFactory(valueFactory);
			
		} else {
			SpinnerValueFactory valueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, defaultValue, step);
			valueFactory.setConverter(new StringConverterDouble(decPlaces, defaultValue));
			spinner.setValueFactory(valueFactory);
		}
		
		if(listener != null) {
			spinner.valueProperty().addListener(listener);
		}
	}
	
	
	
	
	public static void addIcons(Stage stage) {
		stage.getIcons().add(new Image("/icons/wtseIcon256.png"));
		stage.getIcons().add(new Image("/icons/wtseIcon128.png"));
		stage.getIcons().add(new Image("/icons/wtseIcon48.png"));
	}
	
	
	
	public static Object[] openFXScene(Stage stage, String pathFXML, double width, double height, String title) {
		
		if(stage == null) {
			stage = new Stage();
			stage.initModality(Modality.WINDOW_MODAL);
			stage.initOwner(WTSights.getPrimaryStage());
			FXUtils.addIcons(stage);
		}

		FXMLLoader loader = new FXMLLoader(UIMainMenu.class.getResource(pathFXML));
		Parent root = null;
		try {
			root = (Parent) loader.load();
		} catch (IOException e) {
			Logger.get().error("Error loading fxmlScene: " + pathFXML, e);
			return null;
		}

		
		Scene scene = new Scene(root, width, height, true, SceneAntialiasing.BALANCED);
		stage.setTitle(title);
		stage.setScene(scene);
		stage.show();
		
		return new Object[]{loader.getController(), stage};
	}
	
}


















