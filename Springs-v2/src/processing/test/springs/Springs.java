package processing.test.springs;
// This app is based on processing : http://processing.org/
// and pure-data : http://puredata.info/
// but more precisely libpd : http://libpd.cc/
// it consists of a simple spring simulation : http://processingjs.org/learning/topic/springs/
// a set of gui elements : http://processingjs.org/learning/topic/scrollbar/
// && http://processingjs.org/learning/topic/buttons/
// tweaked to do what I wanted ! So the simulation is running and we send the position of each springs to the
// audio engine built in pure data to  controll the volume of each voice.


//JAVA File management
import java.io.File;
import java.io.IOException;

// PURE DATA imports (including Service and Dispatcher not used)
import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.android.service.PdPreferences;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdListener;
import org.puredata.core.PdReceiver;
import org.puredata.core.utils.IoUtils;

// PROCESSING
import processing.core.PApplet;

//ANDROID STUFF
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

// MT management (https://github.com/rjmarsan/AndroidProcessingMultitouch -YEAH !)
import com.rj.processing.mt.Cursor;
import com.rj.processing.mt.MTManager;
import com.rj.processing.mt.TouchListener;

////////////////////////////////////////////////////////////////////////////////////
// Start coding
public class Springs extends PApplet implements TouchListener {

	/////////////////////////////////////////////////////////////////////////////////
	//PROCESSING STUFF - color theming
	int back = color (255, 25);
	int bubbles = color(0);
	int buttoncolor = color(0);
	int highlight = color(180);
	// variables to hold screen size from functions
	int myWidth;
	int myHeight;
	// ratio to resize gui according to sizes of screen
	float sratio;
	float slidRatio;
	// variable to handle time for some animations
	float time =0;
	
	///////////////////////////////////////////////////////////
	// SPRING SIMULTATION
	// Array to hold Springs 
	int num = 8; 
	Spring[] springs = new Spring[num];

	///////////////////////////////////////////////////////////////////
	// SOME AUDIO STUFF
	float [] ringmod = new float [num];
	float ringmod0, ringmod1, ringmod2, ringmod3, ringmod4, ringmod5, ringmod6, ringmod7;
	// maximum amount of modulation (used in a map object to wrap around position in an audible range)
	float mod=(float)0.1;
	
	///////////////////////////////////////////////////////////////////////////////////
	// MULTITOUCH - using andMT
	public MTManager mtManager;
	boolean touchupdated = false;
	
	///////////////////////////////////////////////////////////////////////////////////
	// ACCELEROMETER - using AccelerometerManager.java class
	AccelerometerManager accel;
	float ax, ay, az;
	
	////////////////////////////////////////////////////////////////////
	// GUI ELEMENTS
	// main frame
	Menu top_menu;
	boolean menuDisplay = false;
	// sliders for pannel 1
	HScrollbar index, ratio;
	HScrollbar damping, mass, springyness;
	HScrollbar backA;
	// and button matrix for musical scale selection anchored to pannel 4
	int numb = 24; 
	CircleButton[] buttons = new CircleButton[numb];
	int id =0;
	boolean locked = false;
	String scaleLabel; // display selected scale name
	// line of buttons for base note selection on pannel 2
	int numb2 = 12; 
	CircleButton[] buttons2 = new CircleButton[numb];
	int id2 =0;
	boolean locked2 = false;
	String scaleLabel2; // display selected base note name
	// line of buttons for base note selection on pannel 3 eg transposition factor
	int numb3 = 11; 
	CircleButton[] buttons3 = new CircleButton[numb3];
	int id3 =0;
	boolean locked3 = false;
	String scaleLabel3; // display selected base note name

	 
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// PROCESSING SETUP FUNCTION
	@TargetApi(5)
	@SuppressLint({ "ParserError", "ParserError" })
	public void setup() {
		
		///////////////////////////////////////////
		// PROCESSING
		// Size of the screen is size of the sketch
		myWidth=sketchWidth();
		myHeight=sketchHeight();	
		// define a ratio to display the spring at 
		// correct size according to the screen size
		sratio = myWidth/num;
		slidRatio =  myWidth/24;
		// Size of app, and render
		size(sketchWidth(),sketchHeight(),P2D);
		//force landscape mode
		orientation(LANDSCAPE);
		textMode(CENTER);
		textSize(sratio/10);
		noStroke(); 
		smooth();
		
		//////////////////////////////////////////////////////////////////////
		// SPRINGS 
		for (int i=0; i<num ; i++) {
			springs[i] = new Spring (i*sratio +sratio/2,myHeight/2, sratio-sratio/20,
					(float)0.99, (float)8.0,(float) 0.15, springs, i);
			ringmod[i] = 0;
		}
		
		//////////////////////////////////////////////////////////////////
		// PURE DATA
		openPatch("airsynth.pd");
		// adjust global app volume
		PdBase.sendFloat("pdvol", (float)1);
		
		/////////////////////////////////////////////////////////////////////
		// ACCELEROMETER
		accel = new AccelerometerManager(this);

		/////////////////////////////////////////////////////////////////////
		//MULTITOUCH
		mtManager = new MTManager();
		mtManager.addTouchListener((TouchListener) this);

		/////////////////////////////////////////////////////////////////////
		// GUI	 - a top menu with 4 tabs - each one withe differents params
		// init menu (width, height and handle Size)
		top_menu = new Menu(myWidth, 400, 35);	  
		// button matrix for scale selection
		for (int j=0; j<2 ; j++) {
			for (int i =0; i<12; i++) {
				buttons[id] = new CircleButton(i*sratio*9/20+sratio*9/20, j*sratio*9/20+myHeight*3/16,sratio*9/20,
						(int) buttoncolor, highlight, id);
				id +=1;
			}
		}
		// base note selection
		for (int j=0; j<1 ; j++) {
			for (int i =0; i<12; i++) {
				buttons2[id2] = new CircleButton(i*sratio*9/20+sratio*9/20, j*sratio*9/20+myHeight*3/16, sratio*9/20,
						(int) buttoncolor, highlight, id2);
				id2 +=1;
			}
		}
		// transposition factor
		for (int j=0; j<1 ; j++) {
			for (int i =0; i<11; i++) {
				buttons3[id3] = new CircleButton(i*sratio*9/20+sratio*9/20, j*sratio*9/20+myHeight*3/16, sratio*9/20,
						(int) buttoncolor, highlight, id3);
				id3 +=1;
			}
		}
		// initiate stuff and instances of classes
		menuDisplay = true;
		top_menu.pressed = true;
		scaleLabel = "Scale : major";
		scaleLabel2 = "Base note : C";
		scaleLabel3 = "Ocatve : 5";
		buttons[0].pressed = true;
		buttons2[0].pressed = true;
		buttons3[5].pressed = true;
		updateButtons3(); // for the line before to take effect
		// init sliders (xpos ypos lenght) in first tab
		// effect and mix
		index = new HScrollbar(75, slidRatio*3/4+40, myWidth*3/8, slidRatio*7/8, 10, "distortion"); // the label is right :) don't mind the name of the item
		ratio = new HScrollbar(75, slidRatio*9/4+40, myWidth*3/8, slidRatio*7/8, 10, "dry / wet");
		// slider for motionblur && reverb
		backA = new HScrollbar(75, slidRatio*15/4+40, myWidth*3/8, slidRatio*7/8, 10, "graphic blur - reverb"); // 0-1
		// spring simulation
		damping = new HScrollbar(myWidth/2 + 75, slidRatio*3/4+40, myWidth*3/8, slidRatio*7/8, 10, "spring damping"); // 0-1
		mass = new HScrollbar(myWidth/2 + 75, slidRatio*9/4+40, myWidth*3/8, slidRatio*7/8, 10, "spring mass"); // ~8
		springyness = new HScrollbar(myWidth/2 + 75, slidRatio*15/4+40, myWidth*3/8, slidRatio*7/8, 10, "spring constant"); //0-1
		// init some values
		index.newspos = index.xpos;
		ratio.newspos = ratio.xpos;
		backA.newspos = backA.xpos;
		damping.newspos = damping.xpos +2*damping.swidth/3;
		mass.newspos = mass.xpos +mass.swidth/4;
		springyness.newspos = springyness.xpos +3*springyness.swidth/4;

	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// USE the MENU BUTTON on the device to display our menu
	@Override
	public void keyPressed() {
		if (key == CODED) {
			
			if (keyCode == KeyEvent.KEYCODE_MENU && menuDisplay == false){
				menuDisplay = true;
			}
			else if (keyCode == KeyEvent.KEYCODE_MENU && menuDisplay == true){
				menuDisplay = false;
			}			
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// PROCESSING DRAW FUNCTION
	public void draw() {
		//println(frameRate);
		// BLUR EFFECT
		pushStyle();
		fill(back);
		rect(0, 0, myWidth, myHeight); 
		popStyle();
		
		////////////////////////////////////////////////////////
		// SPRINGS run / display / send value
		for (int i=0; i<num; i++) {
			springs[i].compute();
			springs[i].display();	
			// adjust value and send it to pd
			ringmod[i] = constrain(map(( springs[i].tempypos), 0, myHeight, -mod, mod), -mod, mod);
			PdBase.sendFloat("pdringmod"+i, ringmod[i]);
		}
		
		////////////////////////////////////////////////////////	
		// GUI STUFF / and send their values to pd too
		if (menuDisplay == true){
			top_menu.display();		
			// pannel 1
			if (top_menu.pressed){
				// DISPLAY SLIDERS	
				index.display();
				ratio.display();
				// spring simulation parameters
				damping.display();
				mass.display();
				springyness.display();
				// background transparency
				backA.display();
				// UPDATE THEIR VALUES
				// with slider to control. this just gets the value of slider and adjusts a variable see GUI for display and update 
				float tempbaA = map(backA.getPos(), backA.xpos, backA.xpos+backA.swidth, 255, 30);
				back = color (255, tempbaA); // white with variable transparency
				float verbness = map(backA.getPos(), backA.xpos, backA.xpos+backA.swidth,(float) 0.90,(float) 0.0);
				// when reverb is almost fully dry there is a loss of volume we want to compensate
				float volumeCompensation = map(pow(backA.getPos()/backA.swidth,4), 0, 1, 0,(float) 1);
				PdBase.sendFloat("pdvol", (float)1+volumeCompensation);
				PdBase.sendFloat("pdverbness", verbness);
				// get slider values to control distortion in pure data
				float tempindex = map(index.getPos(), index.xpos, index.xpos+index.swidth, 10, 80);
				float tempratio = map(ratio.getPos(), ratio.xpos, ratio.xpos+ratio.swidth, 0,(float) 1);
				PdBase.sendFloat("pdindex",tempindex);
				PdBase.sendFloat("pdratio", tempratio);			
				// SPRING SIMULATION PARAMETERS
				//adjust spring simulation parameters to slider values
				float mm = map(mass.getPos(), mass.xpos, mass.xpos+mass.swidth, 1, 25);
				float dm = map(damping.getPos(),damping.xpos, damping.xpos+damping.swidth, (float)0.7, (float)1.032);
				float km = map(springyness.getPos(), springyness.xpos, springyness.xpos+springyness.swidth,(float) 0.051,(float) 2);
				// PASS IT TO OUR OBJECTS
				for (int i=0; i<num; i++) {
					//modify values through modifyers_update function
					springs[i].modifyers_update(mm, dm, km);
				}										
			}
			// pannel 2
			if (top_menu.pressed2){				
				pushStyle();
				fill(100, 255);
				//text ( scaleLabel, myWidth+sratio*5/20, +myHeight*7/40);
				text ( scaleLabel2, sratio*6/20,+ myHeight*2/16);
				popStyle();	
				for (int k=0; k<numb2 ; k++) {
					buttons2[k].display();
				}
				// send a value for sliding to new note 
				float tempAccel = constrain(map(ay,(float)0.1,9,0,2000),0,2000);
				PdBase.sendFloat("pdslide",tempAccel);
			}			
			//panel 3
			if(top_menu.pressed3){				
				pushStyle();
				fill(100, 255);
				//text ( scaleLabel, myWidth+sratio*5/20, +myHeight*7/40);
				text ( scaleLabel3, sratio*6/20,+ myHeight*2/16);
				popStyle();				
				for (int k=0; k<numb3 ; k++) {
					buttons3[k].display();
				}
				float tempAccel = constrain(map(ay,(float)0.1,9,0,2000),0,2000);
				PdBase.sendFloat("pdslide",tempAccel);		
			}
			//pannel 4
			if(top_menu.pressed4){
				pushStyle();
				fill(100, 255);
				text ( scaleLabel, +sratio*6/20,myHeight*2/16);
				//text ( scaleLabel2, top_menu.xpos+myWidth+sratio*5/20, top_menu.ypos+ myHeight*1/21);
				popStyle();
				// deal with buttons
				for (int k=0; k<numb ; k++) {
					buttons[k].display();
				}	
				float tempAccel = constrain(map(ay,(float)0.1,9,0,2000),0,2000);
				PdBase.sendFloat("pdslide",tempAccel);
			}
		}		
	}
	
	//////////////////////////////////////////////////////////////////////////////////
	// SPRING : http://processingjs.org/learning/topic/springs/
	class Spring { 
		// Screen values 
		float xpos, ypos;
		float tempxpos, tempypos; 
		float size = 20; 
		boolean isOver = false; 
		boolean move = false; 
		// Spring simulation constants 
		float mass;       // Mass 
		float k =(float) 0.2;    // Spring constant 
		float damp;       // Damping 
		float rest_posx;  // Rest position X 
		float rest_posy;  // Rest position Y 
		// Spring simulation variables 
		//float pos = 20.0; // Position 
		float velx = (float)0.0;   // X Velocity 
		float vely = (float)0.0;   // Y Velocity 
		float accel = 0;    // Acceleration 
		float force = 0;    // Force 
		// other springs
		Spring[] friends;
		// id
		int me;
	
		// Constructor
		Spring(float x, float y, float s, float d, float m, float k_in, Spring[] others, int id) { 
			xpos = tempxpos = x; 
			ypos = tempypos = y;
			rest_posx = x;
			rest_posy = y;
			size = s;
			damp = d; 
			mass = m; 
			k = k_in;
			friends = others;
			me = id;
		} 
		// update mouse position if movable and test if isOver using boolean over
		void update(float touchx, float touchy) { 
			if (move) { 
				rest_posy = touchy; 
				//rest_posx = mouseX;
			} 
		} 
		// Test to see if a touch event is over this spring
		@SuppressLint("ParserError")
		boolean over( float touchx, float touchy) {
			float disX = tempxpos - touchx;
			float disY = tempypos - touchy;
			if (sqrt(sq(disX) + sq(disY)) < size/2 ) {
				return true;
			} 
			else {
				return false;
			}
		}
		// Make sure no other springs are active useless for multi touch
		boolean otherOver() {
			for (int i=0; i<num; i++) {
				if (i != me) {
					if (friends[i].isOver == true) {
						return true;
					}
				}
			}
			return false;
		}
		
		void compute() { 
			force = -k * (tempypos - rest_posy);  // f=-ky 
			accel = force / mass;                 // Set the acceleration, f=ma == a=f/m 
			vely = damp * (vely + accel);         // Set the velocity 
			tempypos = tempypos + vely;           // Updated position 
			force = -k * (tempxpos - rest_posx);  // f=-ky 
			accel = force / mass;                 // Set the acceleration, f=ma == a=f/m 
			velx = damp * (velx + accel);         // Set the velocity 
			tempxpos = tempxpos + velx;           // Updated position 
		}
		void display() { 
			pushStyle();
			// if is over change color
			if (isOver) { 
				fill(bubbles);
			} 
			else { 
				fill(bubbles);
			} 
			//tempypos = constrain(tempypos,-100,height+100);
			ellipse(tempxpos, tempypos, size, size);
			popStyle();
		} 
		// called whe touch down
		void pressed(float touchx, float touchy) { 
			if ((over(touchx,touchy) ) /*&& !otherOver()*/ ) { 
				isOver = true;
				//println(id);
			} 
			else { 
				isOver = false;
			}
			if (isOver) { 
				// set to movable if is over = tru
				move = true;
			} 
			else { 
				move = false;
			}
		} 
		void released() { 
			// set move to false
			move = false; 
			rest_posx = xpos;
			rest_posy = ypos;
		}
		// Modify spring simulation parameters in rt
		void modifyers_update(float mass_mult, float damping_mult, float springyness_mult) {
			mass =  mass_mult;
			damp = damping_mult;
			k = springyness_mult;
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	// FADING MENU with handle+ a second handle to manipulate menus
	class Menu {
		//implement a button that will not move but be used to toggle on and off movements
		float xwidth, yheight;
		float xpos, ypos, tempposx, tempposy; // when visible
		boolean over, over2,over3,over4;
		boolean pressed, pressed2,pressed3,pressed4;
		float pct =0;
		float handleSize =50;
		String label;
		String label2;
		boolean down = false;
		int tabColor, tabColor2,tabColor3,tabColor4;

		Menu(float xwidth0, float yheight0, float handleSize0) {
			handleSize = handleSize0;
			xwidth = xwidth0;
			yheight = yheight0;
			xpos = tempposx=  handleSize*2;
			ypos = tempposy= -yheight;
			label = "v";
			label2=">";
			over = false;
			over2 = false;
			over3 = false;
			over4 = false;
			pressed = false;
			pressed2 = false;
			pressed3= false;
			pressed4= false;
			tabColor = tabColor2= tabColor3=tabColor4= buttoncolor;
		}

		void display() {
			pushStyle();
			// actually we just draw the handles, the menu consists of interactions with buttons
			// we just use the position of the menu to display groups of elements
			pushMatrix();
			stroke(180);
			strokeWeight(2);
			
			// handle to display sliders
			fill(tabColor);
			rect (5, 4,-5+ myWidth/4 , handleSize,20);
			fill(255);
			// label to display if it's up or down
			text("Physics & Sound", myWidth*1/12 ,4+ handleSize*2/3);
			
			// handle to switch to base note + transposition
			fill(tabColor2);
			rect (5+myWidth/4, 4, -5+myWidth/4, handleSize,20);
			fill(255, 255);
			text("BaseNote", myWidth*4/12, 4+handleSize*2/3);
			popMatrix();   
			
			// handle to display sliders
			fill(tabColor3);
			rect (5+myWidth*2/4, 4,-5+ myWidth/4, handleSize,20);
			fill(255);
			// label to display if it's up or down
			text("Transposition", myWidth*7/12,  4+handleSize*2/3);
			
			// handle to display sliders
			fill(tabColor4);
			rect (5+myWidth*3/4, 4,-8+myWidth/4 , handleSize,20);
			fill(255);
			// label to display if it's up or down
			text("Scale", myWidth*10/12,  4+handleSize*2/3);
			popStyle();
		}
		
		// check mouse position regarding the handle
		boolean overme(float x, float y) {
			if (x> 0 && x< myWidth/4 && y> 0 && y < handleSize) {
				over = true;
				//pressed = true;
				pressed2= false;
				pressed3=false;
				pressed4=false;
			} 
			else { 
				over = false;
			}
			return over;
		}

		boolean overme2(float x, float y) {
			if (x> myWidth/4 && x< myWidth/2 && y> 0 && y < handleSize) {
				over2 = true;
				//pressed2=true;
				pressed =false;
				pressed3=false;
				pressed4=false;
			} 
			else { 
				over2 = false;
			}
			return over2;
		}
		
		boolean overme3(float x, float y) {
			if (x> myWidth/2 && x< myWidth*3/4 && y> 0 && y < handleSize) {
				over3 = true;
				//pressed3 = true;
				pressed2= false;
				pressed = false;
				pressed4= false;
			} 
			else { 
				over3 = false;
			}
			return over3;
		}
		
		boolean overme4(float x, float y) {
			if (x> myWidth*3/4 && x< myWidth && y> 0 && y < handleSize) {
				over4 = true;
				pressed3 = false;
				pressed2= false;
				pressed = false;
			} 
			else { 
				over4 = false;
			}

			return over4;
		}	
	}
	//////////////////////////////////////////////////////////////////////////////
	// SLIDERS - http://processingjs.org/learning/topic/scrollbar/
	class HScrollbar {
		float swidth, sheight;    // width and height of bar
		float xpos, ypos;       // x and y position of bar
		float spos, newspos;    // x position of slider
		float sposMin, sposMax; // max and min values of slider
		int loose;              // how loose/heavy
		boolean over;           // is the mouse over the slider?
		boolean locked;
		float ratio;
		String label;

		HScrollbar (float xp, float yp, float sw, float sh, int l, String label0) {
			swidth = sw;
			sheight = sh;
			float widthtoheight = sw - sh;
			ratio = (float)sw / (float)widthtoheight;
			xpos = xp;
			ypos = yp-sheight/2;
			spos = xpos + swidth/2 - sheight/2;
			newspos = spos;
			sposMin = xpos;
			sposMax = xpos + swidth - sheight;
			loose = l;
			label = label0;
		}

		void update( float touchx, float touchy) {
			if (overEvent(touchx, touchy)) {
				over = true;
			} 
			else {
				over = false;
			}
			if ( over) {
				locked = true;
			}
			else  {
				locked = false;
			}
			if (locked) {
				newspos = constrain(touchx, sposMin, sposMax);
			}
		}

		float constrain(float val, float minv, float maxv) {
			return min(max(val, minv), maxv);
		}

		boolean overEvent(float touchx, float touchy) {
			if (touchx > xpos && touchx < xpos+swidth &&
					touchy > ypos && touchy < ypos+sheight) {
				return true;
			} 
			else {
				return false;
			}
		}
		// draw
		void display() {
			if (abs(newspos - spos) > 1) {
				spos = spos + (newspos-spos)/loose;
			}
			pushMatrix();
			//translate (x, y);

			pushStyle();
			noStroke();
			fill(20, 255);
			textSize(14);
			text(label, xpos, ypos);
			fill(204, 200);
			rect(xpos, ypos+sheight, swidth, 1);
			if (over || locked) {
				fill(highlight);
			} 
			else {
				fill(buttoncolor);
			}
			ellipseMode(CORNER);
			ellipse(spos, ypos, sheight-1, sheight-1);
			popMatrix();
			popStyle();
		}

		float getPos() {
			// Convert spos to be values between
			// 0 and the total width of the scrollbar
			return spos  /** ratio*/;
		}
	}

	//////////////////////////////////////////////////////////////////////////
	// BUTTON - http://processingjs.org/learning/topic/buttons/
	class CircleButton {
		float x, y;
		float size;
		int basecolor, highlightcolor;
		int currentcolor;
		boolean over = false;
		boolean pressed = false;   
		int me;

		CircleButton(float ix, float iy, float isize, int icolor, int ihighlight, int id) {
			x = ix;
			y = iy;
			size = isize;
			basecolor = icolor;
			highlightcolor = ihighlight;
			currentcolor = basecolor;
			me = id;
		}

		void update( float tposx, float tposy) {
			if (over(tposx,tposy) ) {
				//currentcolor = highlightcolor;
			} 
			if (pressed){
				currentcolor = highlightcolor;	
			}
			if (!pressed){
				currentcolor = basecolor;
			}
		}
		boolean pressed() {
			if (over) {
				locked = true;				
				return true;
			} 
			else  {
				locked = false;				
				return false;
			}

		}

		boolean overCircle(float x, float y, float diameter, float tposx, float tposy) {
			float disX = x- tposx;
			float disY = y - tposy;
			if (sqrt(sq(disX) + sq(disY)) < diameter/2 ) {
				return true;
			} 
			else {
				return false;
			}
		}

		boolean over( float tposx, float tposy) {
			if ( overCircle(x, y, size,tposx,tposy) ) {
				over = true;
				return true;
			} 
			else {
				over = false;
				return false;
			}
		}

		void display() {
			pushMatrix();
			pushStyle();
			//translate(ex, wy);
			noStroke();
			fill(currentcolor);
			ellipse(x, y, size, size);
			popStyle();
			popMatrix();
		}
	}

	////////////////////////////////////////////////////////////
	// MULTITOUCH MANAGEMENT
	@SuppressLint("ParserError")
	public boolean surfaceTouchEvent(final MotionEvent me) {
		if (mtManager != null) mtManager.surfaceTouchEvent(me);
		return super.surfaceTouchEvent(me);
	}

	@Override
	public void touchAllUp(final Cursor c) {
		//if (inst!=null) inst.allUp();
		//for (int i=0; i<num; i++) { 
		//	springs[i].released();
	}
	@Override
	public void touchDown(final Cursor c) {
		if (top_menu.overme(c.currentPoint.x,c.currentPoint.y)) {
			if (  top_menu.pressed == false) {      
				top_menu.pressed = true;
				top_menu.tabColor = highlight;
				top_menu.tabColor2 = buttoncolor;
				top_menu.tabColor3 = buttoncolor;
				top_menu.tabColor4 = buttoncolor;
			}
		}
		else {
			top_menu.over =false;
		}
		
		// handle2
		if (top_menu.overme2(c.currentPoint.x,c.currentPoint.y)) {
			if ( top_menu.pressed2 == false) {      
				top_menu.pressed2 = true;
				top_menu.tabColor2 = highlight;
				top_menu.tabColor = buttoncolor;
				top_menu.tabColor3 = buttoncolor;
				top_menu.tabColor4 = buttoncolor;
			}
		}
		else {
			top_menu.over2 =false;
		}
		// handle3
		if (top_menu.overme3(c.currentPoint.x,c.currentPoint.y)) {
			if ( top_menu.pressed3 == false) {      
				top_menu.pressed3 = true;
				top_menu.tabColor3 = highlight;
				top_menu.tabColor = buttoncolor;
				top_menu.tabColor2 = buttoncolor;
				top_menu.tabColor4 = buttoncolor;
			}
		}
		else {
				top_menu.over3 =false;
		}
		// handle4
		if (top_menu.overme4(c.currentPoint.x,c.currentPoint.y)) {
			if ( top_menu.pressed4 == false) {      
				top_menu.pressed4 = true;
				top_menu.tabColor4 = highlight;
				top_menu.tabColor = buttoncolor;
				top_menu.tabColor2 = buttoncolor;
				top_menu.tabColor3 = buttoncolor;
			}	
		}
		else {
			top_menu.over4 =false;
		}
		
		// buttons
		if (top_menu.pressed4){
		for (int i=0; i<numb;i++) {
			buttons[i].update(c.currentPoint.x,c.currentPoint.y);
					if (buttons[i].pressed()){
						for (int j =0 ; j<numb;j++){
							if (j!=i){
								buttons[j].pressed = false;
								buttons[j].currentcolor = buttons[j].basecolor;
							} 
							buttons[i].pressed = true;
							buttons[i].currentcolor = buttons[i].highlightcolor;
						}
					}
		}
		updateButtons();
		}
		if (top_menu.pressed2){
			for (int i=0; i<numb2;i++) {
				buttons2[i].update(c.currentPoint.x,c.currentPoint.y);
					if (buttons2[i].pressed()){
						for (int j =0 ; j<numb2;j++){
							if (j!=i){
								buttons2[j].pressed = false;
								buttons2[j].currentcolor = buttons2[j].basecolor;
							} 
							buttons2[i].pressed = true;
							buttons2[i].currentcolor = buttons2[i].highlightcolor;
						}
					}		
			}
		updateButtons2();
		}
		
		if (top_menu.pressed3){
			for (int i=0; i<numb3;i++) {
				buttons3[i].update(c.currentPoint.x,c.currentPoint.y);			
						if (buttons3[i].pressed()){
							for (int j =0 ; j<numb3;j++){
								if (j!=i){
									buttons3[j].pressed = false;
									buttons3[j].currentcolor = buttons3[j].basecolor;
								} 
								buttons3[i].pressed = true;
								buttons3[i].currentcolor = buttons3[i].highlightcolor;
							}
						}					
			}
			updateButtons3();	
		}
	}
	@Override
	public void touchMoved(final Cursor c) {

		for (int i=0; i<num; i++) {

			if (menuDisplay == false){
				if (springs[i].over(c.currentPoint.x,c.currentPoint.y)){
					springs[i].pressed(c.currentPoint.x,c.currentPoint.y);
					springs[i].update(c.currentPoint.x,c.currentPoint.y); 
				}
			}
			if (menuDisplay == true){
				if (c.currentPoint.y>height/2-10){
					//println(c.currentPoint.y);
					if (springs[i].over(c.currentPoint.x,c.currentPoint.y)){
						springs[i].pressed(c.currentPoint.x,c.currentPoint.y);
						springs[i].update(c.currentPoint.x,c.currentPoint.y); 
					}
				}
			}
		}
		
		if (top_menu.pressed){

		// gui sliders
		index.update(c.currentPoint.x,c.currentPoint.y);
		ratio.update(c.currentPoint.x,c.currentPoint.y);
		backA.update(c.currentPoint.x,c.currentPoint.y);
		damping.update(c.currentPoint.x,c.currentPoint.y);
		mass.update(c.currentPoint.x,c.currentPoint.y);
		springyness.update(c.currentPoint.x,c.currentPoint.y); 
		}
	}
	@Override
	public void touchUp(final Cursor c) {
	
		for (int i=0; i<num; i++) { 
			if (top_menu.pressed == true){
				if (c.currentPoint.y>height/2-10){
					if (springs[i].over(c.currentPoint.x,c.currentPoint.y)){
						springs[i].released();
					}
				}
			}
			else {
				if (springs[i].over(c.currentPoint.x,c.currentPoint.y)){
					springs[i].released();
				}
			}	
		}
	}

	// big function for label names according to the scale
	// over matrix of buttons
	void updateButtons() {
		if (buttons[0].pressed()) {
			scaleLabel = "Scale : major";
			PdBase.sendFloat("pdscaleid",0);
		}
		else if (buttons[1].pressed()) {
			scaleLabel = "Scale : middle-east minor";
			PdBase.sendFloat("pdscaleid",1);
		}
		else if (buttons[2].pressed()) {
			scaleLabel = "Scale : lydian dominant";
			PdBase.sendFloat("pdscaleid",2);
		}
		else if (buttons[3].pressed()) {
			scaleLabel = "Scale : harmonic minor";
			PdBase.sendFloat("pdscaleid", 3);
		} else if (buttons[4].pressed()) {
			scaleLabel = "Scale : chromatic blues";
			PdBase.sendFloat("pdscaleid", 4);
		} else if (buttons[5].pressed()) {
			scaleLabel = "Scale : whole tones";
			PdBase.sendFloat("pdscaleid", 5);
		} else if (buttons[6].pressed()) {
			scaleLabel = "Scale : diminished";
			PdBase.sendFloat("pdscaleid", 6);
		} else if (buttons[7].pressed()) {
			scaleLabel = "Scale : pentatonic";
			PdBase.sendFloat("pdscaleid", 7);
		} else if (buttons[8].pressed()) {
			scaleLabel = "Scale : pentatonic blue";
			PdBase.sendFloat("pdscaleid", 8);
		} else if (buttons[9].pressed()) {
			scaleLabel = "Scale : Gaku_ioshi";
			PdBase.sendFloat("pdscaleid", 9);
		} else if (buttons[10].pressed()) {
			scaleLabel = "Scale : in_sen";
			PdBase.sendFloat("pdscaleid", 10);
		} else if (buttons[11].pressed()) {
			scaleLabel = "Scale : hira-joshi";
			PdBase.sendFloat("pdscaleid", 11);
		} else if (buttons[12].pressed()) {
			scaleLabel = "Scale : yo";
			PdBase.sendFloat("pdscaleid", 12);
		} else if (buttons[13].pressed()) {
			scaleLabel = "Scale : ryo";
			PdBase.sendFloat("pdscaleid", 13);
		} else if (buttons[14].pressed()) {
			scaleLabel = "Scale : wato";
			PdBase.sendFloat("pdscaleid", 14);
		} else if (buttons[15].pressed()) {
			scaleLabel = "Scale : tamuke";
			PdBase.sendFloat("pdscaleid", 15);
		} else if (buttons[16].pressed()) {
			scaleLabel = "Scale : enigmatic";
			PdBase.sendFloat("pdscaleid", 16);
		} else if (buttons[17].pressed()) {
			scaleLabel = "Scale : hungarian";
			PdBase.sendFloat("pdscaleid", 17);
		} else if (buttons[18].pressed()) {
			scaleLabel = "Scale : hindu";
			PdBase.sendFloat("pdscaleid", 18);
		} else if (buttons[19].pressed()) {
			scaleLabel = "Scale : oriental 1";
			PdBase.sendFloat("pdscaleid", 19);
		} else if (buttons[20].pressed()) {
			scaleLabel = "Scale : oriental 2";
			PdBase.sendFloat("pdscaleid",20);
		} else if (buttons[21].pressed()) {
			scaleLabel = "Scale : oriental 3";
			PdBase.sendFloat("pdscaleid", 21);
		} else if (buttons[22].pressed()) {
			scaleLabel = "Scale : persan";
			PdBase.sendFloat("pdscaleid", 22);
		} else if (buttons[23].pressed()) {
			scaleLabel = "Scale : experimental";
			PdBase.sendFloat("pdscaleid", 23);
		}
	}

	void updateButtons2() {
		if (buttons2[0].pressed()) {
			scaleLabel2 = "Base Note : C";
			PdBase.sendFloat("pdbasenote",0);	
		}
		else if (buttons2[1].pressed()) {
			scaleLabel2 = "Base Note : C#";
			PdBase.sendFloat("pdbasenote",1);
		}
		else if (buttons2[2].pressed()) {
			scaleLabel2 = "Base Note : D";
			PdBase.sendFloat("pdbasenote",2);
		}
		else if (buttons2[3].pressed()) {
			scaleLabel2 = "Base Note : Eb";
			PdBase.sendFloat("pdbasenote", 3);
		} 
		else if (buttons2[4].pressed()) {
			scaleLabel2 = "Base Note : E";
			PdBase.sendFloat("pdbasenote", 4);
		}
		else if (buttons2[5].pressed()) {
			scaleLabel2 = "Base Note : F";
			PdBase.sendFloat("pdbasenote", 5);
		} 
		else if (buttons2[6].pressed()) {
			scaleLabel2 = "Base Note : F#";
			PdBase.sendFloat("pdbasenote", 6);
		} 
		else if (buttons2[7].pressed()) {
			scaleLabel2 = "Base Note : G";
			PdBase.sendFloat("pdbasenote", 7);
		} 
		else if (buttons2[8].pressed()) {
			scaleLabel2 = "Base Note : G#";
			PdBase.sendFloat("pdbasenote", 8);
		} 
		else if (buttons2[9].pressed()) {
			scaleLabel2 = "Base Note : A";
			PdBase.sendFloat("pdbasenote", 9);
		} 
		else if (buttons2[10].pressed()) {
			scaleLabel2 = "Base Note : Bb";
			PdBase.sendFloat("pdbasenote", 10);
		} 
		else if (buttons2[11].pressed()) {
			scaleLabel2 = "Base Note : B";
			PdBase.sendFloat("pdbasenote", 11);
		} 
	}
	
	void updateButtons3() {
		if (buttons3[0].pressed()) {
			scaleLabel3 = "Octave : 0";
			PdBase.sendFloat("pdt", 0);
		}
		else if (buttons3[1].pressed()) {
			scaleLabel3 = "Octave : 1";
			PdBase.sendFloat("pdt", 12);
		}
		else if (buttons3[2].pressed()) {
			scaleLabel3 = "Ocatve : 2";
			PdBase.sendFloat("pdt", 24);
		}
		else if (buttons3[3].pressed()) {
			scaleLabel3 = "Octave : 3";
			PdBase.sendFloat("pdt", 36);
		} 
		else if (buttons3[4].pressed()) {
			scaleLabel3 = "Octave : 4";
			PdBase.sendFloat("pdt", 48);
		}
		else if (buttons3[5].pressed()) {
			scaleLabel3 = "Octave : 5";
			PdBase.sendFloat("pdt", 60);
		} 
		else if (buttons3[6].pressed()) {
			scaleLabel3 = "Octave : 6";
			PdBase.sendFloat("pdt", 72);
		} 
		else if (buttons3[7].pressed()) {
			scaleLabel3 = "Octave : 7";
			PdBase.sendFloat("pdt", 84);
		} 
		else if (buttons3[8].pressed()) {
			scaleLabel3 = "Octave : 8";
			PdBase.sendFloat("pdt", 96);
		} 
		else if (buttons3[9].pressed()) {
			scaleLabel3 = "Octave : 9";
			PdBase.sendFloat("pdt", 108);
		} 
		else if (buttons3[10].pressed()) {
			scaleLabel3 = "Octave : 10";
			PdBase.sendFloat("pdt", 120);
		} 
	}

	public void accelerationEvent(float x, float y, float z) {
		// println("acceleration: " + x + ", " + y + ", " + z);
		ax = x;
		ay = y;
		az = z;
		redraw();
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// PURE DATA : JAVA STUFF
	// you can copy paste this in your project and just use the openPatch helper function
	private static final int SAMPLE_RATE = 44100;
	private Toast toast = null;
 
	private void toast(final String msg) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (toast == null) {
					toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
				}
				//toast.setText(TAG + ": " + msg);
				toast.show();
			}
		});
	}
 
	public void onResume() {
		super.onResume();
		//Log.d(TAG, "Starting LibPD");
		if (AudioParameters.suggestSampleRate() < SAMPLE_RATE) {
			toast("required sample rate not available; exiting");
			finish();
			return;
		}
		final int nOut = Math.min(AudioParameters.suggestOutputChannels(), 2);
		if (nOut == 0) {
			toast("audio output not available; exiting");
			finish();
			return;
		}
		try {
			PdAudio.initAudio(SAMPLE_RATE, 0, nOut, 1, true);
			PdAudio.startAudio(this);
			} catch (final IOException e) {
			//Log.e(TAG, e.toString());
		}
	}
 
	public void onPause() {
		super.onPause();
		PdAudio.stopAudio();
	}
 
	public void onDestroy() {
		cleanup();
		super.onDestroy();
	}
 
	public void finish() {
		//Log.d(TAG, "Finishing for some reason");
		cleanup();
		super.finish();
	}
 
	// this one will help you open a pd patch !
	public int openPatch(final String patch) {
		final File dir = this.getFilesDir();
		final File patchFile = new File(dir, patch);
		int out=-1;
		try {
			IoUtils.extractZipResource(this.getResources().openRawResource(processing.test.springs.R.raw.patch), dir, true);
			out = PdBase.openPatch(patchFile.getAbsolutePath());
		} catch (final IOException e) {
			e.printStackTrace();
			//Log.e(TAG, e.toString() + "; exiting now");
			finish();
		}
		return out;
	}		
 
	public void cleanup() {
		// make sure to release all resources
		PdAudio.stopAudio();
		PdBase.release();
	}
	
}