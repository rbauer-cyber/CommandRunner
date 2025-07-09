import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Stack;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;

import com.fazecast.jSerialComm.SerialPort;
//import com.fazecast.jSerialComm.SerialPortDataListener;
//import com.fazecast.jSerialComm.SerialPortEvent;

public class SharedSerialPort {
    private boolean dataReady = false;
    private SerialPort port;
	private OutputStream writer;
	private InputStreamReader inputStream;
	private BufferedReader reader;
    private Stack<String> lineStack = new Stack<String>();

	public boolean runFromPrompt = true;
	public boolean portReady = false;
	public boolean running = false;
	private boolean stop = false;
	private final int readTimeOut = 1000;
	private long timeOutCount = 0;
	public String commandResult = null;
	public String command = null;

	private static void delay(int timeMs) {	
		try {
	        Thread.sleep(timeMs);
	    } catch (InterruptedException e) {
	        System.err.format("InterruptedException : %s%n", e);
	    }
	}

	private void writeCommand(String command) throws IOException {
		byte[] dataOut = command.getBytes();
		//writer.write(dataOut,0,dataOut.length);
		
    	for (Byte b : dataOut ) {
        	writer.write(b);
	        delay(100);
    	}

		String strOut = new String(dataOut, StandardCharsets.UTF_8);
        System.out.printf("SerialPort:Wrote: %s: size: %d\n", strOut, dataOut.length);
	}

	public synchronized void setCommand(String command) {
		// Notify the Command thread that a new command has been requested.
		this.dataReady = false;
		this.stop = false;
		this.command = command;
	}

	public Stack<String> getLineStack()
	{
		return lineStack;
	}
	
	public void setCommandResult(String result) {
		this.commandResult = result;
	}

	public void setStopCommand(boolean state) {
		this.stop = state;
		System.out.printf("SerialPort: stop = %b\n", this.stop);
	}

	public boolean getStopCommand(int count) {
//		int remainder = count % 5000000; 
//		
//		if ( remainder == 0 ) {
//			System.out.printf("SerialPort: count: %d, stop = %b\n", count, this.stop);
//		}
		return this.stop;
	}

	private void stopCommand() {
		this.stop = false;
		String command = "s\r";
		try {
			writeCommand(command);
		}
        catch (IOException e) {
        	System.err.println("Error writing to serial port: " + e.getMessage());	            		
        }
	}
	
	public void setTimeOutCount(int timeOutCount) {
		this.timeOutCount = timeOutCount;
	}
	
	public long getTimeOut() {
		return (timeOutCount+2) * 1000;
	}
	
	public boolean getDataReady() {
		return this.dataReady;
	}

	public boolean getRunning() {
		return this.running;
	}

	public synchronized void produceData(String command, String successTerminator, String failTerminator, long timeOut) {
        // ... produce data ... 
    	dataReady = false;
    	this.command = command;
		//this.timeOutCount = timeOut / this.readTimeOut;    	
		this.timeOutCount = 30;    	
    	this.running = true;
    	lineStack.clear();
//    	boolean gotCommand = false;
//    	String commandToRun = "";
    	
        try {
        	// Send the command to the serial port and collect
        	// all the responses until the command prompt is received
        	// All the motion commands are single letter commands [+|-|h|f|m]
        	int count = 0;
        	writeCommand(command);
        	
            boolean foundPrompt = false;
            String line = "";

            // Motor move commands take a long time to execute
            // Error count of 20 indicates 20 seconds elapsed.
            while ( !foundPrompt && timeOutCount > 1 ) {
	            try {
	            	if ( port.bytesAvailable() > 0 ) {
			            if ( (line = reader.readLine()) != null ) {
			            	lineStack.push(line);
			            	// Comment out next line and display received line.
			            	// from linestack after prompt received. 
			                //System.out.println("Received: " + line);
			                
			                if (line.contains(successTerminator) || line.contains(failTerminator)) {
			                	foundPrompt = true;
			                }
			            }
	            	}
	                if ( this.getStopCommand(count) ) {
		                System.out.println("SerialPort: sending stop command");		                
	                	this.stopCommand();
	                }
	                else {
	                	count += 1;
	                }
	            }
	            catch (IOException e) {
	            	if ( timeOutCount-- < 1 ) {
		                System.err.println("Error reading from serial port: " + e.getMessage());	            		
	            	}
	            }
            }
            //System.out.printf("SerialPort: command terminated, count = %d\n", count);
            System.out.println("SerialPort: command terminated");
        }
        catch (Exception e) {
            // This block will catch any exception that is a subclass of Exception
            System.out.println("An exception occurred: " + e.getMessage());
            // Optionally, log the full stack trace for debugging
            e.printStackTrace();
        }

        dataReady = true;
        this.command = null;
    	this.running = false;

        notify(); // Notify waiting consumer thread
    }

    public synchronized void consumeData(String command) throws InterruptedException {
        this.dataReady = false; // Reset for next production cycle
        this.command = command;
    	while (!dataReady) {
            wait(); // Wait until data is ready
        }
        // ... consume data ...
        //dataReady = false; // Reset for next production cycle
        this.command = null;
    }
    
    public synchronized void consumeData() throws InterruptedException {
    	while (!dataReady) {
            wait(); // Wait until data is ready
        }
        // ... consume data ...
    	
        for (String line : lineStack) {
        	System.out.println(line);
        }

        this.command = null;
    }

    public void closePort() {
    	this.running = false;
    	this.port.closePort();
        System.out.println("Serial port closed.");
    }
    
    public SharedSerialPort(boolean runFromPrompt)
    {
    	// Create all the resources required to communicate with the serial port attached
    	// to the arduino board or the nucleo stm32.
    	this.runFromPrompt = runFromPrompt;
    	this.running = true;
        System.out.println("Opening serial port.");
    	port = SerialPort.getCommPort("/dev/ttyACM0"); // Replace with your Arduino's port

        // Open the serial port
        if (port.openPort()) {
            // Set serial port parameters (baud rate, data bits, stop bits, parity)
            port.setComPortParameters(115200, 8, 1, 0); // Must match Arduino's Serial.begin()
            int portTimeOuts = SerialPort.TIMEOUT_READ_SEMI_BLOCKING|SerialPort.TIMEOUT_WRITE_BLOCKING;
            port.setComPortTimeouts(portTimeOuts, readTimeOut, 0);

        	writer = port.getOutputStream();
        	inputStream = new InputStreamReader(port.getInputStream());
        	reader = new BufferedReader(inputStream);
        	this.portReady = true;
            System.out.println("Serial port opened successfully.");
        }
        else {
            System.out.println("Failed to open serial port.");        	
        }
    }
}
