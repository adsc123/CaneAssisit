import com.phidget22.*;
public class test_Ultrasonic{  
    public static void main(String[] args) throws Exception{  
    	VoltageRatioInput voltageRatioInput1 = new VoltageRatioInput();

		voltageRatioInput1.setChannel(1);

		voltageRatioInput1.addSensorChangeListener(new VoltageRatioInputSensorChangeListener() {
			public void onSensorChange(VoltageRatioInputSensorChangeEvent e) {
				System.out.println("SensorValue: " + e.getSensorValue());
				System.out.println("SensorUnit: " + e.getSensorUnit().symbol);
				System.out.println("----------");
			}
		});

		voltageRatioInput1.open(5000);

		voltageRatioInput1.setSensorType(VoltageRatioSensorType.PN_1128);

		//Wait until Enter has been pressed before exiting
		System.in.read();

		voltageRatioInput1.close();
}
}
