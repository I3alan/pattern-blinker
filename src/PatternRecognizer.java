import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.shape.Circle;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class PatternRecognizer extends Application{

	final private static int MAX_RECORDING_TIME 		= 5000; 	//milliseconds
	final private static int RECORDING_UPDATE_TIME 		= 500000; 	//nanoseconds
	
	final private static int STAGE_WIDTH 				= 500;
	final private static int STAGE_HEIGHT 				= 300;
	
	final private static Color LED_ON = new Color(0, 1, 0, 1);
	final private static Color LED_OFF = new Color(0, 0, 0, 0.5);
	
	private static ExecutorService thread_pool = Executors.newFixedThreadPool(2);
	
	private static Runnable recording;
	private static Runnable playback;
	private static Runnable drawStrokeImmediate;
	private static Runnable drawStrokeHeld;
	private static Runnable updateCanvas;
	private static Runnable ledTurnON;
	private static Runnable ledTurnOFF;
	
	private static EventHandler<MouseEvent> preview_btn_handler;
	private static EventHandler<MouseEvent> record_btn_handler;
	private static EventHandler<MouseEvent> generate_btn_handler;
	
	private static EventType<KeyEvent> lastKey = KeyEvent.KEY_RELEASED;
	
	private static ArrayList<int []> timeline_data = new ArrayList<>();
	private static ArrayList<int []> generated_timeline = new ArrayList<>();
	
	private HBox top_row = new HBox();
	private static VBox wrapper = new VBox();
	private Scene scene = new Scene(wrapper);
	
	private static TrackController track_controller = new TrackController();
	private static TrackDisplay track_display = new TrackDisplay(MAX_RECORDING_TIME);
	
	private Spinner<Integer> preview_repetition_counter;
	
	private static volatile Circle LED = new Circle(12);
	
	private Button generate_btn = new Button("Generate Pseudocode");
	private Button preview_btn = new Button("Preview");
	private Button record_btn = new Button("Record");
	
	private static SimpleIntegerProperty repeatPlayback = new SimpleIntegerProperty(1);
	
	private static SimpleFloatProperty startingPlace;
	private static SimpleFloatProperty endingPlace;
	
	private static long start_time;
	private static volatile long elapsed = 0;
	private static boolean isRecording = false;
	private static boolean isPlaying = false;
	
	private static DirectoryChooser dir_chooser = new DirectoryChooser();
	private static File generated_file;
	private String generated_name = "pseudo_blink.txt";
	private static PrintWriter writer;
	
	public static void main(String[] args) {
		
		drawStrokeImmediate = new Runnable() {
			@Override
			public void run() {
				timeline_data.add(new int[] {(int)(System.currentTimeMillis() - start_time), 1});
				track_display.drawBar();
			}
		};
		
		drawStrokeHeld = new Runnable() {
			@Override
			public void run() {
				track_display.drawBar(getLastTimestamp());
				lastKey = KeyEvent.KEY_RELEASED;
				timeline_data.add(new int[] {(int)(System.currentTimeMillis() - start_time), 0});
			}
		};

		updateCanvas = new Runnable() {
			@Override 
			public void run() {
				track_display.PlaybackPosition.set((double)elapsed / MAX_RECORDING_TIME);
			}
		};
		
		recording = new Runnable() {
		    @Override
		    public void run() {
		    	while(isRecording && System.currentTimeMillis() - start_time < MAX_RECORDING_TIME) {
		    		
		    		elapsed = System.currentTimeMillis() - start_time;
		    		Platform.runLater(updateCanvas);
		    		
					try {
						Thread.sleep(0, RECORDING_UPDATE_TIME);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}		
				isRecording = false;
		    }
		 };		
	
		playback = new Runnable() {
			    @Override 
			    public void run() {
			    	int startingLimit = (int)(MAX_RECORDING_TIME * startingPlace.floatValue());
			    	int endingLimit = (int)(MAX_RECORDING_TIME * endingPlace.floatValue());
			    	
			    	for(int j = 0; j < repeatPlayback.intValue(); j++) {
			    		for(int i = startingLimit; i <= endingLimit; i++) {
			    			elapsed = i;
			    			Platform.runLater(updateCanvas);
			    			
			    			for(int k = 0; k < generated_timeline.size(); k++) {
			    				
			    				if (generated_timeline.get(k)[0] == i) {
			    					if (generated_timeline.get(k)[1] == 1) {
			    						Platform.runLater(ledTurnON);
			    					}else {
			    						Platform.runLater(ledTurnOFF);
			    					}
			    				}
			    			}
			    			
				    		try {
								Thread.sleep(1);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
			    		}
			    	}
			    	
			    	isPlaying = false;
			    }
			};
			
		ledTurnON = new Runnable() {
			@Override 
			public void run() {
	   			LED.setFill(LED_ON);
			}
		};
		
		ledTurnOFF = new Runnable() {
			@Override 
			public void run() {
	   			LED.setFill(LED_OFF);
			}
		};


	    launch(args);
	    thread_pool.shutdown();
	}

	@Override
	public void start(Stage stage) {
		LED.setFill(LED_OFF);
		
		startingPlace = new SimpleFloatProperty(0);
		startingPlace.bind(track_controller.startPosition);
		
		endingPlace = new SimpleFloatProperty(1);
		endingPlace.bind(track_controller.endPosition);
		
		preview_repetition_counter = new Spinner<>(1, 10, 0, 1);
		preview_repetition_counter.setMaxWidth(52);
		repeatPlayback.bind(preview_repetition_counter.valueProperty());
		
		track_display.leftLimit.bind(track_controller.endPosition);
		track_display.rightLimit.bind(track_controller.startPosition);
			
		preview_btn_handler = new EventHandler<MouseEvent>() { 
	         @Override 
	         public void handle(MouseEvent e) {
	        	if (!isRecording && !isPlaying) {
					generate_timeline();
					isPlaying = true;
	        		thread_pool.execute(playback);
	        	}
	         } 
	    };
		
	    record_btn_handler = new EventHandler<MouseEvent>() { 
	         @Override 
	         public void handle(MouseEvent e) {
	        	 if (!isRecording) {
	        		 isRecording = true;
	        		 track_controller.reset_handles();
		        	 timeline_data.clear();
		        	 generated_timeline.clear();
		        	 track_display.reset();
		        	 
		        	 start_time = System.currentTimeMillis();
		        	 thread_pool.execute(recording);
		         }
	         } 
	    };
		
	    generate_btn_handler = new EventHandler<MouseEvent>() { 
	         @Override 
	         public void handle(MouseEvent e) { 
	        	
	        	if (!generate_timeline()) {
	        		return;
	        	}
	        	
	        	dir_chooser.setTitle("Generated File Destination");
	        	generated_file = new File(dir_chooser.showDialog(stage).getAbsolutePath()+"\\"+generated_name);
	        	generate_pseudo_code();
	         } 
	    };
		
		generate_btn.setOnMouseClicked(generate_btn_handler);
		preview_btn.setOnMouseClicked(preview_btn_handler);
		record_btn.setOnMouseClicked(record_btn_handler);
		
		
		
		top_row.getChildren().addAll(record_btn,preview_btn,preview_repetition_counter,LED);
		
		//top_row children positioning
		HBox.setMargin(LED, new Insets(0,0,40,10));
		HBox.setMargin(preview_btn, new Insets(0,0,0,220));
		HBox.setMargin(preview_repetition_counter, new Insets(0,0,0,5));
		
		wrapper.getChildren().addAll(track_display.pane,track_controller.get_pane(),top_row,generate_btn);
		wrapper.setPadding(new Insets(40,0,0,35));
		
		//wrapper children positioning
		VBox.setMargin(track_display.pane, new Insets(0,0,0,5));
		VBox.setMargin(top_row, new Insets(10,0,0,0));
		VBox.setMargin(generate_btn, new Insets(30,0,0,0));
		
		scene.addEventFilter(KeyEvent.ANY,event -> {
			if (isRecording) {
				if (event.getEventType() == KeyEvent.KEY_PRESSED) {
					
					if (lastKey != KeyEvent.KEY_PRESSED) {
						Platform.runLater(drawStrokeImmediate);
					}
					
					lastKey = KeyEvent.KEY_PRESSED;
				}else if (event.getEventType() == KeyEvent.KEY_RELEASED) {
					Platform.runLater(drawStrokeHeld);
				}
			}
		});
		
		stage.setTitle("Pattern Recognizer");
		stage.setResizable(false);
		stage.setScene(scene);
		stage.setWidth(STAGE_WIDTH);
		stage.setHeight(STAGE_HEIGHT);
		stage.show();
	}
	
	public static synchronized int getLastTimestamp() {
		return timeline_data.get(timeline_data.size() - 1)[0];
	}	
	
	public static ArrayList<Integer> get_included_indices(int start, int end) {
		ArrayList<Integer> included_indices = null;
		for (int i = 0; i < timeline_data.size(); i++){
			
			if(timeline_data.get(i)[0] >= start && timeline_data.get(i)[0] <= end) {
				if(included_indices == null) {
					included_indices = new ArrayList<>();
				}
				included_indices.add(i);
			}
		}
		
		if(included_indices == null) {
			return null;
		}else {
			return included_indices;
		}
	}

	public static boolean generate_timeline(){
   	 
	   	int startingLimit = (int)(MAX_RECORDING_TIME * startingPlace.floatValue());
	   	int endingLimit = (int)(MAX_RECORDING_TIME * endingPlace.floatValue());
	   
	   	ArrayList<Integer> working_range;
	   	working_range = get_included_indices(startingLimit,endingLimit);
	   	
	   	if (working_range == null || working_range.isEmpty()) {
	   		return false;
	   	}
	   	
	   	generated_timeline = new ArrayList<>();
	   	for(int i : working_range){
	   		generated_timeline.add(timeline_data.get(i));
	   	}
	   	
	   	//Resolve Starting Boundary Case
	   	if (generated_timeline.get(0)[0] > startingLimit) {
	   		if (working_range.get(0) > 0 ) {
	   			generated_timeline.add(0, new int[]{startingLimit,timeline_data.get(working_range.get(0) - 1)[1]});
	   		} else {
	   			generated_timeline.add(0, new int[]{startingLimit,0});
	   		}
	   	}
	   	
	   	//Resolve Ending Boundary Case
	   	if(generated_timeline.get(generated_timeline.size() - 1)[0] < endingLimit) {
	   		generated_timeline.add(new int[]{endingLimit,generated_timeline.get(generated_timeline.size() - 1)[1]});
	   	}
	   	
	   	return true;
	}

	public static void generate_pseudo_code() {
		int [] temp;
		int delay = 0;
		int [][] generated_array = new int[generated_timeline.size() - 1][2];
		
		try {
			writer = new PrintWriter(generated_file);
			writer.println("IMPORTANT NOTICE:");
			writer.println("This is an auto generated pseudo-code for the selected blinking pattern.");
			writer.println("\n");
			
			writer.println("Snippet A: Toggling and Wait Statements\n");
			
			for (int i = 0; i < generated_timeline.size() - 1; i++) {
				temp = generated_timeline.get(i);
				writer.println(temp[1] == 1 ? "LED ON" : "LED OFF");
				
				if ( i < generated_timeline.size()) {
					delay = generated_timeline.get(i+1)[0] - temp[0];
					writer.println("WAIT "+ delay + " ms");
				}
				generated_array[i]= new int[] {temp[1],delay};
			}
			
			writer.println("\n\n\n");
			writer.println("Snippet B: Corresponding Array { Current State (ON = 1, Off = 0), Delay Until Next }\n");
			writer.println(Arrays.deepToString(generated_array).replaceAll("\\[", "{").replaceAll("\\]", "}"));
			
			writer.println("\n\n\n");
			writer.println("HOW-TO-USE: insert the snippet into a loop and replace the toggling and wait statements by corresponding functions\n");
			writer.println("Example:\n");
			writer.println("while (count < 10) {");
			writer.println("\tfor(int i = 0; i < array.length; i++) {");
			writer.println("\t\tif array[i][0] == 1 {");
			writer.println("\t\t\ttoggle_led_on();");
			writer.println("\t\t} else {");
			writer.println("\t\t\ttoggle_led_off();");
			writer.println("\t\t}\n\tsleep(array[i][1]);\n\t}\n}");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			
		} finally {
			if(writer != null) {
				writer.flush();
				writer.close();
			}
		}
	}
	
}
