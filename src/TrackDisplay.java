
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;


public class TrackDisplay {
	
	private final float WIDTH = 400;
	private final float HEIGHT = 50;
	private final float AXIS_POSITION = 45;
	private final float SHADING_AREA_HEIGHT = 45;
	
	private Canvas canvas;
	private volatile GraphicsContext gc;
	public StackPane pane;
	
	public SimpleDoubleProperty rightLimit;
	public SimpleDoubleProperty leftLimit;
	public SimpleDoubleProperty PlaybackPosition;
	
	private Rectangle shadingRect_r;
	private Rectangle shadingRect_l;
	
	private Line startingLine;
	private Line endingLine;
	private Line playbackLine;
	
	private float maxPlaybackTime = 0;
	private Color shadingColor = new Color(0, 0, 0, 0.3);
	
	public TrackDisplay (int MaxRecordingTime){
		maxPlaybackTime = MaxRecordingTime;
        
		canvas = new Canvas(WIDTH, HEIGHT);
		gc = canvas.getGraphicsContext2D();
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(2);
        gc.strokeLine(0, AXIS_POSITION, WIDTH, AXIS_POSITION);
		
		rightLimit = new SimpleDoubleProperty(0);
		leftLimit = new SimpleDoubleProperty(1);
		PlaybackPosition = new SimpleDoubleProperty(0);
		
		shadingRect_r = new Rectangle(0, SHADING_AREA_HEIGHT);
		shadingRect_r.setFill(shadingColor);
		shadingRect_r.widthProperty().bind(rightLimit.multiply(WIDTH));
		
		shadingRect_l = new Rectangle(0, SHADING_AREA_HEIGHT);
		shadingRect_l.setFill(shadingColor);
		shadingRect_l.translateXProperty().bind(leftLimit.multiply(WIDTH));
		shadingRect_l.widthProperty().bind(leftLimit.subtract(1).multiply(-WIDTH));
		
		startingLine = new Line(0, 0, 1, AXIS_POSITION);
		startingLine.translateXProperty().bind(rightLimit.multiply(WIDTH));
		startingLine.setStroke(Color.AQUA);
		
		endingLine = new Line(0, 0, 1, AXIS_POSITION);
		endingLine.translateXProperty().bind(leftLimit.multiply(WIDTH));
		endingLine.setStroke(Color.AQUA);
		
		playbackLine = new Line(0, 0, 1, AXIS_POSITION);
		playbackLine.translateXProperty().bind(PlaybackPosition.multiply(WIDTH));
		playbackLine.setStroke(Color.RED);
		
		pane = new StackPane();
		pane.setAlignment(Pos.BOTTOM_LEFT);
		pane.getChildren().addAll(canvas,shadingRect_r,shadingRect_l,startingLine,endingLine,playbackLine);
	}
	
	public synchronized void drawBar() {
		gc.setStroke(Color.CADETBLUE);
		gc.setLineWidth(1);
		gc.strokeLine(WIDTH * PlaybackPosition.doubleValue(), 5, WIDTH * PlaybackPosition.doubleValue(), AXIS_POSITION);
	}
	
	public void drawBar(int startTimeOfContinuousSignal) {
		gc.setFill(Color.CADETBLUE);
		gc.setLineWidth(2);
		double starting_point = WIDTH * (double)(startTimeOfContinuousSignal) / maxPlaybackTime;
		double ending_point = WIDTH * PlaybackPosition.doubleValue();
		gc.fillRect(starting_point, 5 , ending_point - starting_point, AXIS_POSITION);
	
	}
	
	public void reset() {
		gc.clearRect(0, 0 , WIDTH , HEIGHT);
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(2);
        gc.strokeLine(0, AXIS_POSITION, WIDTH, AXIS_POSITION);

	}
}
