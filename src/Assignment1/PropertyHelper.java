package Assignment1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class PropertyHelper {
	
	private Properties prop;
	
	public PropertyHelper() {
		try (InputStream input = new FileInputStream(new File(System.getProperty("user.dir") + "/config/config.properties"))) {

	        this.prop = new Properties();

	        // load a properties file
            this.prop.load(input);

	    } catch (IOException io) {
	        io.printStackTrace();
	    }
		
	}

	/**
	 * Helper function to read from config.properties
	 * @param propName, string format property name
	 * @return String format property value
	 */
	public String getProperty(String propName) {
		try (InputStream input = new FileInputStream(new File(System.getProperty("user.dir") + "/config/config.properties"))) {

	        this.prop = new Properties();

	        // load a properties file
            this.prop.load(input);

	    } catch (IOException io) {
	        io.printStackTrace();
	    }
		
		if (this.prop.getProperty(propName) == null) {
			return "";
		}
		return this.prop.getProperty(propName);
	}
	
	/**
	 * Helper function to read from config.properties
	 * @param propName, string format property name
	 * @return String format property value
	 */
	public String setProperty(String propName, String newVal) {
		
		try (OutputStream output = new FileOutputStream(new File(System.getProperty("user.dir") + "/config/config.properties"))) {

	        this.prop.setProperty(propName, newVal);
	        

	        // load a properties file
            this.prop.store(output, "");

	    } catch (IOException io) {
	        io.printStackTrace();
	    }
		
		if (this.prop.getProperty(propName) == null) {
			return "";
		}
		return this.prop.getProperty(propName);
	}
}
