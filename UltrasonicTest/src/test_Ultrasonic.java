import com.phidget22.*;
public class test_Ultrasonic{  
    protected static final int SensorValue = 0;

	public static void main(String[] args) throws Exception{  
    	VoltageRatioInput voltageRatioInput1 = new VoltageRatioInput();

		voltageRatioInput1.setChannel(1);

		voltageRatioInput1.addSensorChangeListener(new VoltageRatioInputSensorChangeListener() {
			public void onSensorChange(VoltageRatioInputSensorChangeEvent e) {
				if(e.getSensorValue()<=100) {
				System.out.println("SensorValue: " + e.getSensorValue());
				System.out.println("SensorUnit: " + e.getSensorUnit().symbol);
				System.out.println("close object");
				} 				else  {
					System.out.println("SensorValue: " + e.getSensorValue());
					System.out.println("SensorUnit: " + e.getSensorUnit().symbol);
					System.out.println("away object");
					
				}
				
			}
			
		});

		voltageRatioInput1.open(5000);

		voltageRatioInput1.setSensorType(VoltageRatioSensorType.PN_1128);

	
		System.in.read();

		voltageRatioInput1.close();
}
}