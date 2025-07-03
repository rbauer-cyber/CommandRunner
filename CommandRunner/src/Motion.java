import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.FormSpecs;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.event.ActionListener;
import java.util.Stack;
//import javax.swing.ImageIcon;
//import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.event.ActionEvent;
import javax.swing.JTextArea;
//import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.Timer;
//import javax.swing.JTextPane;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.util.regex.*;


public class Motion extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField positionText;
	private JTextField statusText;
	private Color backgroundColor;
    private Timer timer;
    private long startTime;
    //private Scanner scanner; 
    private static SharedSerialPort resource;
    private static CommandProducerThread commandProducer;
	private static Thread producerThread;

    JButton btnActive;
    JButton btnUpdate;
    JButton btnForward;
    JButton btnHome;
    JButton btnBackward;
    JButton btnFindHome;
    JButton btnStop;
    
	private static Pattern numberPattern = Pattern.compile("\\d+"); 
	private JTextArea textArea;
	private JTextField destinationText;
    
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				boolean runFromPrompt = false;
				resource = new SharedSerialPort(runFromPrompt);

				try {
					while ( !resource.running ) {
				        Thread.sleep(1000);			
					}
					System.out.println("SerialPort running");
				}
			    catch (Exception e) {
			        // This block will catch any exception that is a subclass of Exception
			        System.out.println("An exception occurred: " + e.getMessage());
			        // Optionally, log the full stack trace for debugging
			        e.printStackTrace();
			    }
				
				System.out.println("Starting Command producer thread");
				commandProducer = new CommandProducerThread(resource);
				producerThread = new Thread(commandProducer);
				producerThread.start();	
							
				try {
					Motion frame = new Motion();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void stopCommand(String command) {
		try {
			if (  command != null ) {
				resource.stopCommand(command);
			}
		}
	    catch (Exception e) {
	        // This block will catch any exception that is a subclass of Exception
	        System.out.println("An exception occurred: " + e.getMessage());
	        // Optionally, log the full stack trace for debugging
	        e.printStackTrace();
	    }
	}

	private void runCommand(String command) {
		try {
			if (  command != null ) {
				textArea.setText("");
				// After requesting the new command, the timer callback monitors  
				// the command execution and determines when the command is finished.
				resource.setCommand(command);
				runTimer();				
			}
		}
	    catch (Exception e) {
	        // This block will catch any exception that is a subclass of Exception
	        System.out.println("An exception occurred: " + e.getMessage());
	        // Optionally, log the full stack trace for debugging
	        e.printStackTrace();
	    }
	}

	private int numberExtractor(String line) {
        Matcher matcher = numberPattern.matcher(line);
        int number = 0;
        
        if (matcher.find()) {
        	number = Integer.parseInt(matcher.group());
            //System.out.println("Found number: " + matcher.group());
        }
        
        return number;
	}
	
	private void updateTime() {
		long elapsedTime = System.currentTimeMillis() - startTime;
        //System.out.println("updateTimer");
		if (elapsedTime >= resource.getTimeOut() || !resource.getRunning())
		{
			// parse command results using input scanner
			stopTimer();
			if (resource.commandResult != null) {
		        Stack<String> lineStack = resource.getLineStack();
		        //String line = null;
				int number = numberExtractor(resource.commandResult); // For integer input
		        System.out.println("motor position = "+number);
		        positionText.setText(Integer.toString(number));
		        statusText.setText("");
		        System.out.println("resource running = "+resource.running);	
		        btnActive.setBackground(backgroundColor);

		        for (String line : lineStack) {
		        	if ( !line.isBlank() && !line.contains("Enter") ) {		        		
		        		textArea.append(line+"\n");		        				        	
			        } 
	            }
			}
			else {
		        System.out.println("command result is null");				
			}
				
		}
	}

	private void stopTimer() {
        timer.stop(); 		
	}
	
	private void runTimer() {
       // Start the timer
		resource.setTimeOutCount(4);
		startTime = System.currentTimeMillis();
        timer.start(); 
	}
	
	private void closeResources() {
    	// Stop the command producer thread.
        resource.closePort();
        producerThread.interrupt();

    	// Wait for the command producer thread to finish.
        try {
	        while ( producerThread.isAlive() )
	        	Thread.sleep(200);
        }
	    catch (Exception e) {
	        // This block will catch any exception that is a subclass of Exception
	        System.out.println("An exception occurred: " + e.getMessage());
	        // Optionally, log the full stack trace for debugging
	        e.printStackTrace();
	    }
        
        System.out.println("Gui resources closed");
	}
	
 	private void runButtonCommand(JButton button, String text, String command ) {
		statusText.setText(text);
		button.setBackground(Color.CYAN);
	    btnActive = button;
		runCommand(command);
	}	

	/**
	 * Create the frame.
	 */
	public Motion() {
		setTitle("Motor Controller");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 402, 318);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,},
			new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				RowSpec.decode("default:grow"),})		
        );

		addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.out.println("Motion Gui window closing");
                closeResources();
            }
        });

		//this.setIconImage(new ImageIcon(getClass().getResource("/images/coffee_icon.png")).getImage());
		
		timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTime();
            }
         });
		
		btnForward = new JButton("Forward");
		btnForward.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				runButtonCommand(btnForward, "Move Forward", "+\r");
			}
		});
		contentPane.add(btnForward, "2, 2");
		
		btnHome = new JButton("Home");
		btnHome.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				runButtonCommand(btnHome, "Move Home", "h\r");
			}
		});
		contentPane.add(btnHome, "4, 2");
		
		btnBackward = new JButton("Reverse");
		btnBackward.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				runButtonCommand(btnBackward, "Move Reverse", "-\r");
			}
		});
		
		btnUpdate = new JButton("Update");
		btnUpdate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				runButtonCommand(btnUpdate, "Update Status", "u\r");
			}
		});
		contentPane.add(btnUpdate, "6, 2");
		contentPane.add(btnBackward, "2, 4");
		
		btnFindHome = new JButton("Find Home");
		btnFindHome.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				runButtonCommand(btnFindHome, "Find Home", "f\r");
			}
		});
		contentPane.add(btnFindHome, "4, 4");
		
		btnStop = new JButton("Stop");
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				statusText.setText("Stop");
				btnStop.setBackground(Color.GREEN);
			    stopCommand("s\r");
			    btnStop.setBackground(backgroundColor);
			}
		});
		contentPane.add(btnStop, "6, 4");
		
		JLabel lblPosition = new JLabel("Position");
		lblPosition.setHorizontalAlignment(SwingConstants.CENTER);
		contentPane.add(lblPosition, "2, 6");
		
		JButton btnGoToPosition = new JButton("Go to Position");
		btnGoToPosition.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				statusText.setText("Go to Position");
				String enteredText = destinationText.getText();
				String command = "m " + enteredText + "\r";
				if (!enteredText.isEmpty()) {
					runButtonCommand(btnGoToPosition, "Goto Position", command);
					//System.out.println(command);
					//runButtonCommand(btnFindHome, "Go to Position", "f\r");
				}
			}
		});
		contentPane.add(btnGoToPosition, "4, 6");
		
		JLabel lblStatus = new JLabel("Status");
		lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
		contentPane.add(lblStatus, "6, 6");
		
		positionText = new JTextField();
		positionText.setBackground(new Color(255, 255, 255));
		positionText.setEditable(false);
		contentPane.add(positionText, "2, 8, fill, default");
		positionText.setColumns(10);
		
		destinationText = new JTextField();
		contentPane.add(destinationText, "4, 8, fill, default");
		destinationText.setColumns(10);
		
		statusText = new JTextField();
		statusText.setBackground(new Color(255, 255, 255));
		statusText.setEditable(false);
		contentPane.add(statusText, "6, 8, fill, default");
		statusText.setColumns(10);
		
		textArea = new JTextArea();
		textArea.setColumns(1);
		textArea.setRows(40);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		contentPane.add(textArea, "2, 10, 5, 1, fill, fill");
		
		JScrollBar scrollBar = new JScrollBar();
		contentPane.add(scrollBar, "8, 10");
		
		backgroundColor = btnUpdate.getBackground();
	}

}
