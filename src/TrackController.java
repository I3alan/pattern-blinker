
import javafx.scene.paint.Color;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

public class TrackController{
	private final int WIDTH = 400;
	private final int HANDLE_RADIUS = 10;
	private final int BACKGROUND_STROKE_WIDTH = 15;
	private final int ACCENT_STROKE_WIDTH = 10;
	
	private boolean drag_entered = false;

	private SimpleDoubleProperty handle_start_center_X;
	private SimpleDoubleProperty handle_end_center_X ;
	
	private EventHandler<MouseEvent> startHandle_eventHandler;
	private EventHandler<MouseEvent> endHandle_eventHandler;

	private Circle handle_start;
	private Circle handle_end;
	private Line backround;
	private Line progress;
	private StackPane pane;
	
	public SimpleDoubleProperty startPosition;
	public SimpleDoubleProperty endPosition;
	
	public TrackController() {
		handle_start_center_X = new SimpleDoubleProperty(0);
		handle_end_center_X = new SimpleDoubleProperty(WIDTH);
		
		startPosition = new SimpleDoubleProperty(0);
		endPosition = new SimpleDoubleProperty(1);
		
		startPosition.bind(handle_start_center_X.divide(WIDTH));
		endPosition.bind(handle_end_center_X.divide(WIDTH));
		
		startHandle_eventHandler = new EventHandler<MouseEvent>() { 
	         @Override 
	         public void handle(MouseEvent e) {
	        	 update_mouse_dragging_state(e);
	        	
	            if(drag_entered && e.getX() >= 0 && e.getX() < handle_end_center_X.doubleValue()) {
	            	handle_start_center_X.set(e.getX());
	            }
	        	if(drag_entered && e.getX() <= 0) {
	        		 handle_start_center_X.set(0);
	        	}
	         } 
	    };
	    
		endHandle_eventHandler = new EventHandler<MouseEvent>() { 
	         @Override 
	         public void handle(MouseEvent e) { 
	        	 update_mouse_dragging_state(e);
	        	 
	        	 if(drag_entered && e.getX() <= WIDTH && e.getX() > handle_start_center_X.doubleValue()) {
		            handle_end_center_X.set(e.getX());
		         }
	        	 if(drag_entered && e.isDragDetect() && e.getX() >= WIDTH) {
	        		 handle_end_center_X.set(WIDTH);
	        	 }
	         } 
	    };
	      
		handle_start = new Circle(HANDLE_RADIUS);
		handle_start.setFill(Color.DARKGRAY);
		handle_start.setStroke(Color.BLACK);
		handle_start.centerXProperty().bind(handle_start_center_X.add(-HANDLE_RADIUS / 2));
		handle_start.translateXProperty().bind(handle_start_center_X.add(-HANDLE_RADIUS / 2));
		handle_start.setCenterY(0);
		handle_start.addEventHandler(MouseEvent.ANY, startHandle_eventHandler);
		
		handle_end = new Circle(HANDLE_RADIUS);
		handle_end.centerXProperty().bind(handle_end_center_X);
		handle_end.translateXProperty().bind(handle_end_center_X);
		handle_end.setCenterY(0);
		handle_end.setFill(Color.DARKGRAY);
		handle_end.setStroke(Color.BLACK);
		handle_end.addEventHandler(MouseEvent.ANY, endHandle_eventHandler);
		
		backround = new Line(0,0,WIDTH,0);
		backround.setStroke(Color.GRAY);
		backround.setStrokeWidth(BACKGROUND_STROKE_WIDTH);
		
		progress = new Line(0,0,0,0);
		progress.setStroke(Color.BLUE);
		progress.setStrokeWidth(ACCENT_STROKE_WIDTH);
		progress.startXProperty().bind(handle_start_center_X);
		progress.translateXProperty().bind(handle_start_center_X);
		progress.endXProperty().bind(handle_end_center_X);
		
		pane = new StackPane();
		pane.setAlignment(Pos.CENTER_LEFT);
		pane.getChildren().addAll(backround,progress,handle_start,handle_end);
	}
	private void update_mouse_dragging_state(MouseEvent e) {
    	if(e.isPrimaryButtonDown() && e.isDragDetect()){
    		drag_entered = true;
    	}else if (e.getEventType() == MouseEvent.MOUSE_RELEASED) {
    		drag_entered = false;
    	}
    }
	public void reset_handles() {
		handle_start_center_X.set(0);
		handle_end_center_X.set(WIDTH);
	}
	public StackPane get_pane() {
		return pane;
	}

}
