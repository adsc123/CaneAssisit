import com.phidget22.*;

public class InputVoltage {

	public static void main(String[] args) throws Exception {
		RCServo rcServo0 = new RCServo();


		VoltageRatioInput voltageRatioInput0 = new VoltageRatioInput();
		voltageRatioInput0.addSensorChangeListener(new VoltageRatioInputSensorChangeListener() {
			public void onSensorChange(VoltageRatioInputSensorChangeEvent e) {
	        double sliderOutput = e.getSensorValue();
			System.out.println("SensorValue: " + sliderOutput);
			System.out.println("SensorUnit: " + e.getSensorUnit().symbol);
			System.out.println("----------");
			double motorTarget =  sliderOutput * 36 + 90;
			try {			rcServo0.setTargetPosition(motorTarget);
}catch(PhidgetException x) {System.out.println("not worked");}


//			return sliderOutput;
						}
					});


					//Open your Phidgets and wait for attachment
					rcServo0.open(5000);
					rcServo0.setDeviceSerialNumber(20130);
					rcServo0.setTargetPosition(90);

					rcServo0.setEngaged(true);
				
					voltageRatioInput0.open(5000);
					voltageRatioInput0.setDeviceSerialNumber(432454);

					

					//Do stuff with your Phidgets here or in your event handlers.
					//Set the sensor type to match the analog sensor you are using after opening the Phidget
					voltageRatioInput0.setSensorType(VoltageRatioSensorType.PN_1112);

					Thread.sleep(50000);
					
	
						


						rcServo0.close();
						}}

		
					
			
		
			

		
		// TODO Auto-generated method stub

	
	


