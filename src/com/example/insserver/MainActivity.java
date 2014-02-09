package com.example.insserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * Main Activity for the Server side application. Starts into the main page defined by activity_main.xml
 */

public class MainActivity extends Activity {

	static BluetoothAdapter mBluetoothAdapter;
	private int REQUEST_ENABLE_BT;
	private Set<BluetoothDevice> pairedDevices;
	private TextView cs, sent, ts, textSteps, textDegrees;
	private Button send, actT, actW, actS;
	private Button s1,s2,s3,s4,s5,s6,s7,s8,s9,s10, t1,t2,t3,t4,t5, dF,dB,dR,dL, dSR, dSL;
	private EditText editdeg, customTxt;
	private String msgSteps = "steps";
	private String msgDegs = "degrees to the";
	private String newW;
	private ToggleButton tButton;
	private BluetoothDevice btDevice;
	private AcceptThread at;
	private boolean connect;
	private boolean cReady = false;
	private boolean edit = false;
	private boolean customDeg = false;
	private boolean customCmd = false;
	private ArrayList<String> message;
	public Handler handler;
	
	
	/**
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 * 
	 * Initialises IDs, texts, bluetooth and handlers.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		getIDs(); 
		setBase();
		setPrevious();
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mBluetoothAdapter == null) {
			// Device does not support Bluetooth
			CharSequence text = "Device does not support Bluetooth!";
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(getApplicationContext(), text, duration);
			toast.show();
		}
		if(!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		handler = new Handler() {
			/**
			 * @see android.os.Handler#handleMessage(android.os.Message)
			 * 
			 * Handles incoming messages. (Ideally should use another thread instead of UI thread)
			 */
			@Override
			public void handleMessage(Message msg) {
				switch(msg.what) { // what: 0 - misc, 1 - client status, 2 - handshake lost
				case 0: //Connection message
					CharSequence text = (String) msg.obj;
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(getApplicationContext(), text, duration);
					toast.show();
					if(((String) msg.obj).contentEquals("Connected")) {
						connect = true;
						if(!edit)
							send.setEnabled(true);
					}
					break;
				case 1: //Client status: 0->not ready, 1->ready, 2->Tracker live, 3->Tracker accuracy low, 4->Tracker stopped 
					if((Integer)msg.obj==1) {
						cs.setText(getResources().getString(R.string.ready));
						cReady = true;
					}
					else if((Integer)msg.obj==0){
						cs.setText(getResources().getString(R.string.notready));
						cReady = false;
					}
					else if((Integer)msg.obj==2){
						ts.setText(getResources().getString(R.string.live));
					}
					else if((Integer)msg.obj==3){
						ts.setText(getResources().getString(R.string.lowacc));
					}
					else if((Integer)msg.obj==4){
						ts.setText(getResources().getString(R.string.off));
					}
					break;
				case 2: //
					if(connect) {
						CharSequence text2 = (String) msg.obj;
						int duration2 = Toast.LENGTH_SHORT;
						Toast toast2 = Toast.makeText(getApplicationContext(), text2, duration2);
						toast2.show();
						disconnectDevice();
					}
					break;
				default: super.handleMessage(msg);
				}
			}

		};
	}
	
	/**
	 * Retrieve the IDs of elements on the main page.
	 */
	protected void getIDs() {		
		actW = (Button) findViewById(R.id.walkButton); 
		actT = (Button) findViewById(R.id.turnButton); 
		actS = (Button) findViewById(R.id.shorelineButton);
		s1 = (Button) findViewById(R.id.button1); s2 = (Button) findViewById(R.id.button2); s3 = (Button) findViewById(R.id.button3); s4 = (Button) findViewById(R.id.button4); s5 = (Button) findViewById(R.id.button5);
		s6 = (Button) findViewById(R.id.button6); s7 = (Button) findViewById(R.id.button7); s8 = (Button) findViewById(R.id.button8); s9 = (Button) findViewById(R.id.button9); s10 = (Button) findViewById(R.id.button10);
		t1 = (Button) findViewById(R.id.deg45); t2 = (Button) findViewById(R.id.deg90); t3 = (Button) findViewById(R.id.deg135); t4 = (Button) findViewById(R.id.deg180); t5 = (Button) findViewById(R.id.degcustom);  editdeg = (EditText) findViewById(R.id.editdeg);
		dF = (Button) findViewById(R.id.forwardButton); dB = (Button) findViewById(R.id.backButton); dR = (Button) findViewById(R.id.rightButton); dL = (Button) findViewById(R.id.leftButton); dSL = (Button) findViewById(R.id.shoreLeftButton); dSR = (Button) findViewById(R.id.shoreRightButton);
		customTxt = (EditText) findViewById(R.id.customText);
		textSteps = (TextView) findViewById(R.id.textSteps);
		textDegrees = (TextView) findViewById(R.id.textDegs);
		send = (Button) findViewById(R.id.send);
		cs = (TextView) findViewById(R.id.textView1);
		ts = (TextView) findViewById(R.id.tstatus);
		sent = (TextView) findViewById(R.id.sent);
		tButton = (ToggleButton) findViewById(R.id.startT);
	}
	
	/**
	 * Set the default buttons to be enabled and create a default message. Message should not be able to be sent.
	 */
	private void setBase() {
		message = new ArrayList<String>(5);
		message.add((String)actW.getText()); message.add((String)s1.getText()); message.add(msgDegs); message.add((String)dF.getText());
		message.add("Walk");message.add("");
		t1.setEnabled(false); t2.setEnabled(false); t3.setEnabled(false); t4.setEnabled(false); t5.setEnabled(false); editdeg.setEnabled(false);
		dF.setEnabled(true); dB.setEnabled(true); dR.setEnabled(false); dL.setEnabled(false); dSR.setEnabled(false); dSL.setEnabled(false);
		send.setEnabled(false); textDegrees.setEnabled(false);
	}
	
	/**
	 * Sets the texts of the buttons to a previously saved state or to the default of none is saved.
	 */
	private void setPrevious() {
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		if(!settings.getBoolean("saved", false)) {
			actW.setText("Walk"); actT.setText("Turn"); actS.setText(R.string.shoreline);
			s1.setText("One");
			s2.setText("Two");
			s3.setText("Three");
			s4.setText("Four");
			s5.setText("Five");
			s6.setText("Six");
			s7.setText("Seven");
			s8.setText("Eight");
			s9.setText("Nine");
			s10.setText("Ten");
			t1.setText("45");
			t2.setText("90");
			t3.setText("135");
			t4.setText("180");
			t5.setText("Custom");
			dF.setText("Forward"); dB.setText("Back"); dR.setText("Right"); dL.setText("Left"); dSR.setText("Right"); dSL.setText("Left");
			textSteps.setText("steps");
			textDegrees.setText("degrees to the");
			return;
		}
		actW.setText(settings.getString("actW", "Walk"));
		actT.setText(settings.getString("actT", "Turn"));
		actS.setText(settings.getString("actS", "Follow Shore line"));
		s1.setText(settings.getString("s1", "One"));
		s2.setText(settings.getString("s2", "Two"));
		s3.setText(settings.getString("s3", "Three"));
		s4.setText(settings.getString("s4", "Four"));
		s5.setText(settings.getString("s5", "Five"));
		s6.setText(settings.getString("s6", "Six"));
		s7.setText(settings.getString("s7", "Seven"));
		s8.setText(settings.getString("s8", "Eight"));
		s9.setText(settings.getString("s9", "Nine"));
		s10.setText(settings.getString("s10", "Ten"));
		t1.setText(settings.getString("t1", "45"));
		t2.setText(settings.getString("t2", "90"));
		t3.setText(settings.getString("t3", "135"));
		t4.setText(settings.getString("t4", "180"));
		t5.setText(settings.getString("t5", "Custom"));
		dF.setText(settings.getString("dF", "Forward"));
		dB.setText(settings.getString("dB", "Back"));
		dR.setText(settings.getString("dR", "Right"));
		dL.setText(settings.getString("dL", "Left"));
		dSR.setText(settings.getString("dSR", "Right"));
		dSL.setText(settings.getString("dSL", "Left"));
		textSteps.setText(settings.getString("textStep", "steps"));
		textDegrees.setText(settings.getString("textDeg", "degrees to the"));
	}
	
	/** 
	 * @see android.app.Activity#onStop()
	 * 
	 * If connected to bluetooth kill Acceptthread, uncheck tracker button.
	 * Show status text as not ready, disable send button.
	 */
	@Override
	protected void onStop() {
		super.onStop();
		if(connect) {
			at.cancel();
			connect = false;
			tButton.setChecked(false);
		}
		cs.setText(getResources().getString(R.string.notready));
		send.setEnabled(false);
		sent.setText("Message sent:");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	/** 
	 * On menu connect selection, bluetooth connection is disconnected.
	 * On menu disconnect selection, bluetooth connection is disconnected.
	 * On menu edit selection, user is able to click buttons/text to edit their text.
	 * On menu defaults selection, return buttons/texts to default.
	 * 
	 */
	public void onOptionsSelected (MenuItem item) {
		switch(item.getItemId()) {
		case R.id.connect:
			if(!connect) {
				at = new AcceptThread();
				at.start();
			}
			break;
		case R.id.disconnect:
			if(connect) {
				at.wt.handShake.cancel();
				at.wt.handShake.purge();
				at.cancel();
				cs.setText(getResources().getString(R.string.notready));
				sent.setText("Message sent:");
				cReady = false;
				connect = false;
			}
			tButton.setChecked(false);
			send.setEnabled(false);
			CharSequence text = "Disconnected!";
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(getApplicationContext(), text, duration);
			toast.show();
			break;
		case R.id.edit:
			if (!edit) {
				edit = true;
				item.setTitle("Set Words");
				send.setEnabled(false);
			}else {
				edit = false;
				item.setTitle("Edit Words");
				setBase();
				send.setEnabled(true);
			}
			break;
		case R.id.retdefault:
			SharedPreferences settings = getPreferences(MODE_PRIVATE);
			SharedPreferences.Editor edit = settings.edit();
			edit.putBoolean("saved", false);
			edit.commit();
			setPrevious();
			setBase();
			break;
		case R.id.instructions:
			Intent i = new Intent(MainActivity.this, InstructionScreen.class);
			startActivity(i);
			break;
		default:
			break;
		}
	}
	
	/**
	 * Disconnect bluetooth device.
	 */
	private void disconnectDevice() {
		if(connect) {
			at.wt.stopHandshake();
			at.cancel();
			cs.setText(getResources().getString(R.string.notready));
			sent.setText("Message sent:");
			cReady = false;
			connect = false;
		}
		tButton.setChecked(false);
		send.setEnabled(false);
		CharSequence text = "Disconnected!";
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(getApplicationContext(), text, duration);
		toast.show();
	}
	
	/**
	 * @param name
	 * @param rb
	 * 
	 * Produces dialog box for the user to enter the new text for the button/text.
	 */
	private void setNew(final String name, final TextView rb) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Change word");  
		alert.setMessage("Enter new word:");                

		// Set an EditText view to get user input   
		final EditText input = new EditText(this); 
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {  
			public void onClick(DialogInterface dialog, int whichButton) {  
				SharedPreferences settings = getPreferences(MODE_PRIVATE);
				SharedPreferences.Editor edit = settings.edit();
				newW = input.getText().toString();
				edit.putBoolean("saved", true);
				edit.putString(name, newW);
				edit.commit();
				rb.setText(newW);
				return;                  
			}  
		});  

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				return;   
			}
		});
		alert.show();
	}
	
	/**
	 * @param view
	 * 
	 * Sends an emergency stop signal to client. Can be sent even if client is not ready.
	 */
	public void eStop(View view) {
		if(connect) {
			String s= "Sound:Stop";
			sent.setText("Message sent: Emergency Stop!");
			at.wt.write(s.getBytes());
		}/*else if(connect && !cReady){
			CharSequence text = "Not ready";
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(getApplicationContext(), text, duration);
			toast.show();
		}*/else if(!connect){
			CharSequence text = "Not connected";
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(getApplicationContext(), text, duration);
			toast.show();
		}
	}
	
	/**
	 * @param view
	 * 
	 * When one of the standard command button is clicked 
	 */
	public void onStandardClicked(View view) {
		if(!connect){
			CharSequence text = "Not connected";
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(getApplicationContext(), text, duration);
			toast.show();
			return;
		}
		switch(view.getId()) {
		case R.id.estop:
			String s= "Sound:Stop";
			sent.setText("Message sent: Emergency Stop!");
			at.wt.write(s.getBytes());
			break;
		case R.id.feelButton:
			String f= "Sound:Feel";
			sent.setText("Message sent: Feel for");
			at.wt.write(f.getBytes());
			break;
		case R.id.scanButton:
			String sc= "Sound:Scan";
			sent.setText("Message sent: Scan for");
			at.wt.write(sc.getBytes());
			break;
		case R.id.alignButton:
			String a= "Sound:Align";
			sent.setText("Message sent: Align with");
			at.wt.write(a.getBytes());
			break;
		case R.id.popButton:
			String p= "Sound:Pop";
			sent.setText("Message sent: Pop up");
			at.wt.write(p.getBytes());
			break;
		case R.id.squareButton:
			String sq= "Sound:Square";
			sent.setText("Message sent: Square off");
			at.wt.write(sq.getBytes());
			break;
		default:
			break;
		}
	}
	
	/**
	 * @param view
	 * 
	 * When startT button is pressed send a signal to client to begin logging.
	 */
	public void startLog(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		if(!connect){
			((ToggleButton) view).setChecked(!on);
			return;
		}
		if(on) {
			String s = "Log:Start";
			at.wt.write(s.getBytes());
		}else {
			String s = "Log:Stop";
			at.wt.write(s.getBytes());
		}
	}
	
	/**
	 * Update message to be sent on server screen
	 */
	public void updateMessage(int pos, String string) {
		if(pos == -1) {
			sent.setText("Message sending: " + string);
			return;
		}
		String dispMessage = new String();
		dispMessage = dispMessage.concat("Message sending: "+ message.get(0) + " " + message.get(1) + " "+ message.get(2) + " " + message.get(3));
		sent.setText(dispMessage);
	}
	
	/**
	 * @param view
	 * 
	 * Called when an action button is clicked. ie. Walk or Turn.
	 * Enables or disables other buttons based on which is selected.
	 * Also sets message based on selected button.
	 */
	public void onActionClicked(View view) {
	    
	    switch(view.getId()) {
	        case R.id.walkButton:
	            if (!edit) {
					customDeg = false;
					customCmd = false;
	                message.set(0,(String) actW.getText());
	                message.set(2, msgSteps);
	                message.set(4, "Walk");
	                s1.setEnabled(true);s2.setEnabled(true);s3.setEnabled(true);s4.setEnabled(true);s5.setEnabled(true);
	                s6.setEnabled(true);s7.setEnabled(true);s8.setEnabled(true);s9.setEnabled(true);s10.setEnabled(true);
	                t1.setEnabled(false);t2.setEnabled(false);t3.setEnabled(false);t4.setEnabled(false);t5.setEnabled(false);editdeg.setEnabled(false);
	                dF.setEnabled(true); dB.setEnabled(true); dR.setEnabled(false); dL.setEnabled(false); dSR.setEnabled(false); dSL.setEnabled(false);
	                textSteps.setEnabled(true); textDegrees.setEnabled(false);
	                
	                updateMessage(0,(String) actW.getText());
	                
	            } else if(edit) {
	            	setNew("actW", actW);
	            }
	            break;
	        case R.id.turnButton:
	            if (!edit) {
					customDeg = false;
					customCmd = false;
	                message.set(0,(String) actT.getText()); 
	                message.set(2, msgDegs);
	                message.set(4, "Turn");
	            	s1.setEnabled(false);s2.setEnabled(false);s3.setEnabled(false);s4.setEnabled(false);s5.setEnabled(false);
	                s6.setEnabled(false);s7.setEnabled(false);s8.setEnabled(false);s9.setEnabled(false);s10.setEnabled(false);
	                t1.setEnabled(true);t2.setEnabled(true);t3.setEnabled(true);t4.setEnabled(true);t5.setEnabled(true);editdeg.setEnabled(true);
	                dR.setEnabled(true); dL.setEnabled(true); dF.setEnabled(false); dB.setEnabled(false); dSR.setEnabled(false); dSL.setEnabled(false);
	                textSteps.setEnabled(false); textDegrees.setEnabled(true);

	                updateMessage(0, (String) actT.getText());
	            }else if(edit) {
	            	setNew("actT", actT);
	            }
	            break;
	        case R.id.shorelineButton:
	        	if (!edit) {
					customDeg = false;
					customCmd = false;
	                message.set(0,(String) actS.getText()); 
	                message.set(1, "");
	                message.set(2, "");
	                message.set(4, "Follow Shore line");
	            	s1.setEnabled(false);s2.setEnabled(false);s3.setEnabled(false);s4.setEnabled(false);s5.setEnabled(false);
	                s6.setEnabled(false);s7.setEnabled(false);s8.setEnabled(false);s9.setEnabled(false);s10.setEnabled(false);
	                t1.setEnabled(false);t2.setEnabled(false);t3.setEnabled(false);t4.setEnabled(false);t5.setEnabled(false);editdeg.setEnabled(false);
	                dR.setEnabled(false); dL.setEnabled(false); dF.setEnabled(false); dB.setEnabled(false); dSR.setEnabled(true); dSL.setEnabled(true);
	                textSteps.setEnabled(false); textDegrees.setEnabled(true);

	                updateMessage(0, (String) actS.getText());
	            }else if(edit) {
	            	setNew("actS", actS);
	            }
	            break;
	        case R.id.useButton:
	        	customDeg = false;
	        	customCmd = true;
	        	message.set(0, "Custom");
	        	message.set(1, "Custom");
	        	message.set(2, "Custom");
	        	message.set(3, "Custom");
	        	message.set(4, "Custom");
	        	message.set(5, "Custom");
	        	updateMessage(-1, customTxt.getText().toString());
	        default:
	        	break;
	    }
	}	
	
	/**
	 * @param view
	 * 
	 * Called when a step value button is pressed. Also sets the message accordingly.
	 */
	public void onStepClicked(View view) {

		// Check which radio button was clicked
		switch(view.getId()) {
		case R.id.button1:
			if (!edit) {
				message.set(1,(String) s1.getText());
				updateMessage(1,(String) s1.getText());
			}else if(edit) {
				setNew("s1", s1);
			}
			break;
		case R.id.button2:
			if (!edit) {
				message.set(1,(String) s2.getText());
				updateMessage(1,(String) s2.getText());
			}else if(edit) {
				setNew("s2", s2);
			}
			break;
		case R.id.button3:
			if (!edit) {
				message.set(1,(String) s3.getText());
				updateMessage(1,(String) s3.getText());
			}else if(edit) {
				setNew("s3", s3);
			}
			break;
		case R.id.button4:
			if (!edit) {
				message.set(1,(String) s4.getText());
				updateMessage(1,(String) s4.getText());
			}else if(edit) {
				setNew("s4", s4);
			}
			break;
		case R.id.button5:
			if (!edit) {
				message.set(1,(String) s5.getText());
				updateMessage(1,(String) s5.getText());
			}else if(edit) {
				setNew("s5",s5);
			}
			break;
		case R.id.button6:
			if (!edit) {
				message.set(1,(String) s6.getText());
				updateMessage(1,(String) s6.getText());
			}else if(edit) {
				setNew("s6", s6);
			}
			break;
		case R.id.button7:
			if (!edit) {
				message.set(1,(String) s7.getText());
				updateMessage(1,(String) s7.getText());
			}else if(edit) {
				setNew("s7", s7);
			}
			break;
		case R.id.button8:
			if (!edit) {
				message.set(1,(String) s8.getText());
				updateMessage(1,(String) s8.getText());
			}else if(edit) {
				setNew("s8", s8);
			}
			break;
		case R.id.button9:
			if (!edit) {
				message.set(1,(String) s9.getText());
				updateMessage(1,(String) s9.getText());
			}else if(edit) {
				setNew("s9", s9);
			}
			break;
		case R.id.button10:
			if (!edit) {
				message.set(1,(String) s10.getText());
				updateMessage(1,(String) s10.getText());
			}else if(edit) {
				setNew("s10", s10);
			}
			break;
		default:
			break;
		}
	}
	
	/**
	 * @param view
	 * 
	 * Called when a turn value button is pressed. Also sets the message accordingly
	 */
	public void onTurnClicked(View view) {

		// Check which radio button was clicked
		switch(view.getId()) {
		case R.id.deg45:
			if (!edit) {
				customDeg = false;
				message.set(1,(String) t1.getText());
				updateMessage(1,(String) t1.getText());
			}else if(edit) {
				setNew("t1", t1);
			}
			break;
		case R.id.deg90:
			if (!edit) {
				customDeg = false;
				message.set(1,(String) t2.getText());
				updateMessage(1,(String) t2.getText());
			}else if(edit) {
				setNew("t2", t2);
			}
			break;
		case R.id.deg135:
			if (!edit) {
				customDeg = false;
				message.set(1,(String) t3.getText());
				updateMessage(1,(String) t3.getText());
			}else if(edit) {
				setNew("t3", t3);
			}
			break;
		case R.id.deg180:
			if (!edit) {
				customDeg = false;
				message.set(1,(String) t4.getText());
				updateMessage(1,(String) t4.getText());
			}else if(edit) {
				setNew("t4", t4);
			}
			break;
		case R.id.degcustom:
			if (!edit) {
				customDeg = true;
				message.set(1, editdeg.getText().toString());
				updateMessage(1, editdeg.getText().toString());
			}else if(edit) {
				setNew("t5", t5);
			}
			break;
		default:
			break;
		}
		
	}
	
	/**
	 * @param view
	 * 
	 * Called when a direction radio button is pressed. Also sets the message accordingly
	 */
	public void onDirClicked(View view) {

		// Check which radio button was clicked
		switch(view.getId()) {
		case R.id.forwardButton:
			if (!edit) {
				message.set(3,(String) dF.getText());
				updateMessage(3,(String) dF.getText());
			}else if(edit) {
				setNew("dF", dF);
			}
			break;
		case R.id.backButton:
			if (!edit) {
				message.set(3,(String) dB.getText());
				updateMessage(3,(String) dB.getText());
			}else if(edit) {
				setNew("dB", dB);
			}
			break;
		case R.id.rightButton:
			if (!edit) {
				message.set(3,(String) dR.getText());
				message.set(5,"right");
				updateMessage(3,(String) dR.getText());
			}else if(edit) {
				setNew("dR", dR);
			}
			break;
		case R.id.leftButton:
			if (!edit) {
				message.set(3,(String) dL.getText());
				message.set(5,"left");
				updateMessage(3,(String) dL.getText());
			}else if(edit) {
				setNew("dL", dL);
			}
			break;
		case R.id.shoreLeftButton:
			if (!edit) {
				message.set(3,(String) dSL.getText());
				updateMessage(3,(String) dSL.getText());
			}else if(edit) {
				setNew("dSL", dSL);
			}
			break;
		case R.id.shoreRightButton:
			if (!edit) {
				message.set(3,(String) dSR.getText());
				updateMessage(3,(String) dSR.getText());
			}else if(edit) {
				setNew("dSR", dSR);
			}
			break;
		default:
			break;
		}		
	}
	
	/**
	 * @param view
	 * 
	 * Called when the send button is pressed. Send button is only enabled when connected.
	 * The message is sent to the client application.
	 */
	public void onSendClicked(View view) {
		if(message.size() < 6) {
			sent.setText("Message size error, not sent.");
			return;
		}
		String dispMessage = new String();
		String sendMessage = new String();
		byte[] b = new byte[1024];
		if(customDeg) {
			String customTxt = editdeg.getText().toString();
			message.set(1, editdeg.getText().toString());
			if(customTxt.length() < 1) {
				sent.setText("Custom degrees error, not sent.");
				return;
			}
		} else if (customCmd) {
			String customTxt_ = customTxt.getText().toString();
			sent.setText("Message sent: " + customTxt_);
			b = msgToByteArray(customTxt_);
			if(b != null)
				at.wt.write(b);
			return;
		}
		dispMessage = dispMessage.concat("Message sent: "+ message.get(0) + " " + message.get(1) + " "+ message.get(2) + " " + message.get(3));
		sent.setText(dispMessage);
		sendMessage = sendMessage.concat(message.get(0) + " " + message.get(1) + " "+ message.get(2) + " " + message.get(3));
		b = msgToByteArray(sendMessage);
		if(b != null)
			at.wt.write(b);
	}
	
	/**
	 * @param view
	 * 
	 * Called when a text is pressed. Allows text edits.
	 */
	public void textEdit(View view) {
		if(edit) {
			switch(view.getId()) {
			case R.id.textSteps:
				setNew("textStep",textSteps);
				break;
			case R.id.textDegs:
				setNew("textDeg",textDegrees);
				break;
			}
		}
	}
	
	/**
	 * @param mid
	 * @param whole
	 * @return a byte array consisting of the message to be sent
	 * 
	 * Converts the message to be sent from a string array to a byte array.
	 */
	private byte[] msgToByteArray(String whole) {
		ArrayList<String> bA = new ArrayList<String>();
		bA.add(message.get(0));//Action
		bA.add(message.get(1));//Steps or degrees
		bA.add(message.get(2));
		bA.add(message.get(3));//Dir
		bA.add(message.get(4));//walk or turn or shoreline or custom
		bA.add(message.get(5));//left or right
		bA.add(whole);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(baos);
			for (String element : bA) {
				oos.writeObject(element);
			}
		} catch (IOException e) {
			sent.setText("Message failed to be converted and sent.");
			e.printStackTrace();
			return null;
		}
		byte[] b = baos.toByteArray();
		return b;		
	}
	
	/**
	 * @author Gabriel
	 *
	 * Thread for accepting bluetooth connections.
	 */
	private class AcceptThread extends Thread {
		private final BluetoothServerSocket mmServerSocket;
		private final String MY_UUID = "e4e7dcc0-0d67-11e3-8ffd-0800200c9a66";
		private final String NAME = "Instructions";
		public Handler aHandler;
		private WorkerThread wt;
		 
	    /**
	     * Constructor, initialises Bluetooth
	     */
	    public AcceptThread() {
	        // Use a temporary object that is later assigned to mmServerSocket,
	        // because mmServerSocket is final
	        BluetoothServerSocket tmp = null;
	        try {
	            // MY_UUID is the app's UUID string, also used by the client code
	            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, UUID.fromString(MY_UUID));
	        } catch (IOException e) { }
	        mmServerSocket = tmp;
	    }
	 
	    /** 
	     * @see java.lang.Thread#run()
	     * 
	     * Main method in thread. Creates handler thread to send message to worker thread.
	     */
	    public void run() {
	        BluetoothSocket socket = null;
	        // Create and start the HandlerThread - it requires a custom name
	        HandlerThread handlerThread = new HandlerThread("MyHandlerThread");
	        handlerThread.start();
	        // Get the looper from the handlerThread
	        // Note: this may return null
	        Looper looper = handlerThread.getLooper();
	        // Create a new handler - passing in the looper to use and this class as
	        // the message handler
	        aHandler = new Handler(looper) { //what; 0 - misc
	        	public void handleMessage(Message msg) {
	        		Message msgS = Message.obtain(msg);
	        		if(!wt.wHandler.sendMessage(msgS))
	        			super.handleMessage(msg);
	        	}
	        };
	        
	        // Keep listening until exception occurs or a socket is returned
	        while (true) {
	            try {
	                handler.obtainMessage(0,"Waiting for connection...").sendToTarget();
	                socket = mmServerSocket.accept();
	            } catch (IOException e) {
	                break;
	            }
	            // If a connection was accepted
	            if (socket != null) {
	                // Do work to manage the connection (in a separate thread)
	            	wt = new WorkerThread(socket);
	        		wt.start();
	            	handler.obtainMessage(0, "Connected").sendToTarget();
	                try {
						mmServerSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
	                break;
	            }
	        }
	    }
	 
	    /** Will cancel the listening socket, and cause the thread to finish */
	    public void cancel() {
	        try {
	            mmServerSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
	/**
	 * @author Gabriel
	 *
	 * WorkerThread Class, mainly deals with writing messages to the Bluetooth stream, reading client's status
	 * and running a handshake schedule.
	 */
	private class WorkerThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	    private Timer handShake = new Timer();
	    private boolean hs = true;
	    public Handler wHandler;
	 
	    /**
	     * @param socket
	     * 
	     * Constructor to initialise streams.
	     */
	    public WorkerThread(BluetoothSocket socket) {
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	    
	    /**
	     * Stop handshake timer
	     */
	    private void stopHandshake() {
	    	handShake.purge();
	    	handShake.cancel();
	    }
	 
	    /**
	     * @see java.lang.Thread#run()
	     * 
	     * Main method for thread, schedules handshake. Sends client status to main thread to handle message.
	     */
	    public void run() {
	        byte[] buffer = new byte[1024];  // buffer store for the stream
	        int bytes; // bytes returned from read()
	    	int clientStatus = 0; //0-not ready, 1-ready

		    handShake.scheduleAtFixedRate(new TimerTask() {
		    	public void run() {
		    		if(hs == false)
		    			handler.obtainMessage(2, "Connection lost").sendToTarget();
		    		hs = false;
		    		String hs = "Handshake";
		    		write(hs.getBytes());
		    	}
		    },0,3000);
	        
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	                //bytes = mmInStream.read(buffer);
	            	clientStatus = mmInStream.read();
	            	if(clientStatus == 5) hs = true;
	            	// Send the obtained bytes to the UI activity
	            	else
	            		handler.obtainMessage(1, clientStatus).sendToTarget();
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) {
	        try {
	            mmOutStream.write(bytes);
	        } catch (IOException e) { }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    @SuppressWarnings("unused")
		public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
	
}

