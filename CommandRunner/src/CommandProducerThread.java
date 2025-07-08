import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

class CommandProducerThread implements Runnable {
    private SharedSerialPort resource;
    private boolean commandDone = false;
    private String name = "CommandProducer";

    private Thread worker;
    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(false);

	// Define pattern for detecting motion control commands, single letter commands.
	private Pattern commandPattern = Pattern.compile("[+-hfm]");

    public void interrupt() {
        running.set(false);
        worker.interrupt();
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public CommandProducerThread(SharedSerialPort resource) {
        this.resource = resource;
    }

	private void runCommand(String command) {	
        Matcher matcher = commandPattern.matcher(command);
        Stack<String> lineStack = null;
        String terminator = null;
        String commandResult = null;
        //String line = null;
        
        int timeOut = 4000;        

    	if ( command.contains("u") || command.contains("o") ) {
   			terminator = "command";
   			timeOut = 4000;
    	}
    	else if ( matcher.find() ) { 
    		terminator = "MotionMgr: motor";
   			timeOut = 60000;
    	}
    	else
    	{
    		System.out.printf("%s: failed to find command keyword\n", name);
    	}
    	
		resource.produceData(command, terminator, timeOut);
		lineStack = resource.getLineStack();
		System.out.printf("%s: processing command result, response count = %d\n", name, lineStack.size());
		int commandEnterCount = 0;
				
        while ( commandResult == null ) {
        	for (String line : lineStack) {
            	//line = lineStack.pop();
	        	
	            if ( line.contains("Motor: position") ) {
	            	commandResult = line;        	
	            }
	            else if ( line.contains("Motor: no move") ) {
	            	commandResult = line;        	
	            }
	            else if (line.contains("MotionMgr: motor position")) {
	            	commandResult = line;		                			                	
	            }
	            else if (line.contains("MotionMgr: motor error")) {
	            	// Motor position is last field in string, find start of field.
	            	int firstIndex = line.indexOf("position");
	            	String substring = line.substring(firstIndex);
	            	commandResult = "MotionMgr: motor "+substring;		                			                	
	            }
	            else if (line.contains("Enter command")) {
	        		System.out.printf("%s: processing results, response = %s\n", name, line);
	        		commandEnterCount += 1;
	        		
	        		if (commandEnterCount > 3) {
		        		System.out.printf("%s: processing results, too many: = %d, stack size: %d\n",
		        				name, commandEnterCount , lineStack.size());
		        		commandResult = "Command error";
		        		break;
	        		}
	            }
            }
        }
		        
        resource.setCommandResult(commandResult);
    	System.out.printf("%s: command done\n", name);
    	commandDone = true;
	}
	
	private void runFromCommand(String command) {
    	System.out.printf("%s: running command %s", name, command);
    	commandDone = false;
		runCommand(command);		
	}
	
	public String getCommandResult() {
		return resource.commandResult;		
	}
	
	public boolean getCommandDone() {
		return commandDone;		
	}

    @Override
    public void run() {
    	// Command producer waits for other thread to request command and then executes command.
    	System.out.printf("%s running\n", name);
        running.set(true);
        stopped.set(false);
 
    	try {
    		while ( running.get() ) { 
	    		if ( resource.command != null ) {
	            	runFromCommand(resource.command);  
	            	commandDone = true;
	    		}
		        Thread.sleep(500);
    		}
    	}
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.printf("%s: interrupted\n", name);
        }
    	
    	//resource.closePort();
    	stopped.set(true);    	
        System.out.printf("%s: stopped\n", name);
    }
}
