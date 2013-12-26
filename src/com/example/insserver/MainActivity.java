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
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
	private RadioGroup act, step, turn, dir;
	private Button send;
	private RadioButton ra1, ra2, rs1,rs2,rs3,rs4,rs5,rs6,rs7,rs8,rs9,rs10, rt1,rt2,rt3,rt4,rt5, rd1,rd2,rd3,rd4;
	private EditText editdeg;
	private String msgSteps = "steps";
	private String msgDegs = "degrees to the";
	private String newW;
	private ToggleButton tButton;
	private BluetoothDevice btDevice;
	private AcceptThread at;
	private boolean connect;
	private boolean cReady = false;
	private boolean edit = false;
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
		setDefaults();
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
					CharSequence text2 = (String) msg.obj;
					int duration2 = Toast.LENGTH_SHORT;
					Toast toast2 = Toast.makeText(getApplicationContext(), text2, duration2);
					toast2.show();
					disconnectDevice(null);
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
		act = (RadioGroup) findViewById(R.id.actionradio);
		step = (RadioGroup) findViewById(R.id.stepradio);
		turn = (RadioGroup) findViewById(R.id.turnradio);
		dir = (RadioGroup) findViewById(R.id.dirradio);
		ra1 = (RadioButton) findViewById(R.id.walkbutton); ra2 = (RadioButton) findViewById(R.id.turnbutton);
		rs1 = (RadioButton) findViewById(R.id.onestep); rs2 = (RadioButton) findViewById(R.id.twostep); rs3 = (RadioButton) findViewById(R.id.threestep); rs4 = (RadioButton) findViewById(R.id.fourstep); rs5 = (RadioButton) findViewById(R.id.fivestep);
		rs6 = (RadioButton) findViewById(R.id.sixstep); rs7 = (RadioButton) findViewById(R.id.sevenstep); rs8 = (RadioButton) findViewById(R.id.eightstep); rs9 = (RadioButton) findViewById(R.id.ninestep); rs10 = (RadioButton) findViewById(R.id.tenstep);
		rt1 = (RadioButton) findViewById(R.id.deg45); rt2 = (RadioButton) findViewById(R.id.deg90); rt3 = (RadioButton) findViewById(R.id.deg135); rt4 = (RadioButton) findViewById(R.id.deg180); rt5 = (RadioButton) findViewById(R.id.degcustom);  editdeg = (EditText) findViewById(R.id.editdeg);
		rd1 = (RadioButton) findViewById(R.id.forward); rd2 = (RadioButton) findViewById(R.id.back); rd3 = (RadioButton) findViewById(R.id.right); rd4 = (RadioButton) findViewById(R.id.left);
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
	private void setDefaults() {
		message = new ArrayList<String>(5);
		message.add((String)ra1.getText()); message.add((String)rs1.getText()); message.add((String)rd1.getText());
		message.add("");message.add("");
		turn.setEnabled(false); send.setEnabled(false); rd1.setEnabled(true); rd2.setEnabled(true); rd3.setEnabled(false); rd4.setEnabled(false);
		rt1.setEnabled(false);rt2.setEnabled(false);rt3.setEnabled(false);rt4.setEnabled(false);rt5.setEnabled(false);editdeg.setEnabled(false);
		textDegrees.setEnabled(false);
	}
	
	/**
	 * Sets the texts of the buttons to a previously saved state or to the default of none is saved.
	 */
	private void setPrevious() {
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		if(!settings.getBoolean("saved", false)) {
			ra1.setText("Walk");
			ra2.setText("Turn");
			rs1.setText("One");
			rs2.setText("Two");
			rs3.setText("Three");
			rs4.setText("Four");
			rs5.setText("Five");
			rs6.setText("Six");
			rs7.setText("Seven");
			rs8.setText("Eight");
			rs9.setText("Nine");
			rs10.setText("Ten");
			rt1.setText("45");
			rt2.setText("90");
			rt3.setText("135");
			rt4.setText("180");
			rt5.setText("Custom");
			rd1.setText("Forward");
			rd2.setText("Back");
			rd3.setText("Right");
			rd4.setText("Left");
			textSteps.setText("steps");
			textDegrees.setText("degrees to the");
			return;
		}
		ra1.setText(settings.getString("ra1", "Walk"));
		ra2.setText(settings.getString("ra2", "Turn"));
		rs1.setText(settings.getString("rs1", "One"));
		rs2.setText(settings.getString("rs2", "Two"));
		rs3.setText(settings.getString("rs3", "Three"));
		rs4.setText(settings.getString("rs4", "Four"));
		rs5.setText(settings.getString("rs5", "Five"));
		rs6.setText(settings.getString("rs6", "Six"));
		rs7.setText(settings.getString("rs7", "Seven"));
		rs8.setText(settings.getString("rs8", "Eight"));
		rs9.setText(settings.getString("rs9", "Nine"));
		rs10.setText(settings.getString("rs10", "Ten"));
		rt1.setText(settings.getString("rt1", "45"));
		rt2.setText(settings.getString("rt2", "90"));
		rt3.setText(settings.getString("rt3", "135"));
		rt4.setText(settings.getString("rt4", "180"));
		rt5.setText(settings.getString("rt5", "Custom"));
		rd1.setText(settings.getString("rd1", "Forward"));
		rd2.setText(settings.getString("rd2", "Back"));
		rd3.setText(settings.getString("rd3", "Right"));
		rd4.setText(settings.getString("rd4", "Left"));
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
	 * @param item
	 * 
	 * On menu item selection, new thread is created to accept incoming bluetooth connections.
	 */
	public void connectDevice(MenuItem item) {
		if(!connect) {
			at = new AcceptThread();
			at.start();
		}
	}
	
	/**
	 * @param item
	 * 
	 * On menu item selection, bluetooth connection is disconnected.
	 */
	public void disconnectDevice(MenuItem item) {
		if(connect) {
			at.cancel();
			cs.setText(getResources().getString(R.string.notready));
			sent.setText("Message sent:");
			cReady = false;
			connect = false;
			at.wt.handShake.cancel();
		}
		tButton.setChecked(false);
		send.setEnabled(false);
		CharSequence text = "Disconnected!";
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(getApplicationContext(), text, duration);
		toast.show();
	}
	
	/**
	 * @param item
	 * 
	 * On menu item selection, user is able to click buttons/text to edit their text.
	 */
	public void editWords(MenuItem item) {
		if (!edit) {
			edit = true;
			item.setTitle("Set Words");
			send.setEnabled(false);
		}else {
			edit = false;
			item.setTitle("Edit Words");
			setMessage();
			send.setEnabled(true);
		}
	}

	/**
	 * Set message to be sent based on what radio buttons are selected.
	 */
	private void setMessage() {
		switch(act.getCheckedRadioButtonId()) {
		case R.id.walkbutton:
			message.set(0,(String) ra1.getText());
			switch(step.getCheckedRadioButtonId()) {
			case R.id.onestep:
				message.set(1,(String) rs1.getText());
				break;
			case R.id.twostep:
				message.set(1,(String) rs2.getText());
				break;
			case R.id.threestep:
				message.set(1,(String) rs3.getText());
				break;
			case R.id.fourstep:
				message.set(1,(String) rs4.getText());
				break;
			case R.id.fivestep:
				message.set(1,(String) rs5.getText());
				break;
			case R.id.sixstep:
				message.set(1,(String) rs6.getText());
				break;
			case R.id.sevenstep:
				message.set(1,(String) rs7.getText());
				break;
			case R.id.eightstep:
				message.set(1,(String) rs8.getText());
				break;
			case R.id.ninestep:
				message.set(1,(String) rs9.getText());
				break;
			case R.id.tenstep:
				message.set(1,(String) rs10.getText());
				break;
			} 
			break;
		case R.id.turnbutton:
			message.set(0,(String) ra2.getText()); 
			switch (turn.getCheckedRadioButtonId()) {
			case R.id.deg45:
					message.set(1,(String) rt1.getText());
				break;
			case R.id.deg90:
					message.set(1,(String) rt2.getText());
				break;
			case R.id.deg135:
					message.set(1,(String) rt3.getText());
				break;
			case R.id.deg180:
					message.set(1,(String) rt4.getText());
				break;
			}
			break;
		}  
		switch(dir.getCheckedRadioButtonId()) {
		case R.id.forward:
				message.set(2,(String) rd1.getText());
			break;
		case R.id.back:
				message.set(2,(String) rd2.getText());
			break;
		case R.id.right:
				message.set(2,(String) rd3.getText());
            	message.set(4, "right");
			break;
		case R.id.left:
				message.set(2,(String) rd4.getText());
            	message.set(4, "left");
			break;
		}
		msgSteps = textSteps.getText().toString();
		msgDegs = textDegrees.getText().toString();
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
	 * @param item
	 * 
	 * Return buttons/texts to default.
	 */
	public void retDefault(MenuItem item) {
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor edit = settings.edit();
		edit.putBoolean("saved", false);
		edit.commit();
		setPrevious();
		setMessage();
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
	 * @param view
	 * 
	 * Called when an action radiobutton is clicked. ie. Walk or Turn.
	 * Enables or disables other buttons based on which is selected.
	 * Also sets message based on selected radio buttons.
	 */
	public void onActionClicked(View view) {
		boolean checked = ((RadioButton) view).isChecked();
	    
	    // Check which radio button was clicked
	    switch(view.getId()) {
	        case R.id.walkbutton:
	            if (checked && !edit) {
	                message.set(0,(String) ra1.getText());
	                message.set(3, "Walk");
	                rs1.setEnabled(true);rs2.setEnabled(true);rs3.setEnabled(true);rs4.setEnabled(true);rs5.setEnabled(true);
	                rs6.setEnabled(true);rs7.setEnabled(true);rs8.setEnabled(true);rs9.setEnabled(true);rs10.setEnabled(true);
	                rt1.setEnabled(false);rt2.setEnabled(false);rt3.setEnabled(false);rt4.setEnabled(false);rt5.setEnabled(false);editdeg.setEnabled(false);
	                rd1.setEnabled(true); rd2.setEnabled(true); rd3.setEnabled(false); rd4.setEnabled(false);
	                textSteps.setEnabled(true); textDegrees.setEnabled(false);
	                if(rd3.isChecked() || rd4.isChecked()) rd1.setChecked(true);
	                
	                message.set(1,(String) ((RadioButton)findViewById(step.getCheckedRadioButtonId())).getText());
	                message.set(2,(String) ((RadioButton)findViewById(dir.getCheckedRadioButtonId())).getText());
	            } else if(edit) {
	            	setNew("ra1", ra1);
	            }
	            break;
	        case R.id.turnbutton:
	            if (checked && !edit) {
	                message.set(0,(String) ra2.getText()); 
	                message.set(3, "Turn");
	            	rs1.setEnabled(false);rs2.setEnabled(false);rs3.setEnabled(false);rs4.setEnabled(false);rs5.setEnabled(false);
	                rs6.setEnabled(false);rs7.setEnabled(false);rs8.setEnabled(false);rs9.setEnabled(false);rs10.setEnabled(false);
	                rt1.setEnabled(true);rt2.setEnabled(true);rt3.setEnabled(true);rt4.setEnabled(true);rt5.setEnabled(true);editdeg.setEnabled(true);
	                rd3.setEnabled(true); rd4.setEnabled(true); rd1.setEnabled(false); rd2.setEnabled(false);
	                textSteps.setEnabled(false); textDegrees.setEnabled(true);
	                if(rd1.isChecked() || rd2.isChecked()) rd3.setChecked(true);

	                if(((String) ((RadioButton)findViewById(turn.getCheckedRadioButtonId())).getText()).compareTo("Custom") == 0)
	                	message.set(1,editdeg.getText().toString());
	                else
	                	message.set(1,(String) ((RadioButton)findViewById(turn.getCheckedRadioButtonId())).getText());
	                message.set(2,(String) ((RadioButton)findViewById(dir.getCheckedRadioButtonId())).getText());
	                if(rd3.isChecked())
	                	message.set(4, "right");
	                else if(rd4.isChecked())
	                	message.set(4, "left");
	            }else if(edit) {
	            	setNew("ra2", ra2);
	            }
	            break;
	        default:
	        	break;
	    }
	}
	
	/**
	 * @param view
	 * 
	 * Called when a step value radio button is pressed. Also sets the message accordingly.
	 */
	public void onStepClicked(View view) {
		boolean checked = ((RadioButton) view).isChecked();

		// Check which radio button was clicked
		switch(view.getId()) {
		case R.id.onestep:
			if (checked && !edit) {
				message.set(1,(String) rs1.getText());
			}else if(edit) {
				setNew("rs1", rs1);
			}
			break;
		case R.id.twostep:
			if (checked && !edit) {
				message.set(1,(String) rs2.getText());
			}else if(edit) {
				setNew("rs2", rs2);
			}
			break;
		case R.id.threestep:
			if (checked && !edit) {
				message.set(1,(String) rs3.getText());
			}else if(edit) {
				setNew("rs3", rs3);
			}
			break;
		case R.id.fourstep:
			if (checked && !edit) {
				message.set(1,(String) rs4.getText());
			}else if(edit) {
				setNew("rs4", rs4);
			}
			break;
		case R.id.fivestep:
			if (checked && !edit) {
				message.set(1,(String) rs5.getText());
			}else if(edit) {
				setNew("rs5", rs5);
			}
			break;
		case R.id.sixstep:
			if (checked && !edit) {
				message.set(1,(String) rs6.getText());
			}else if(edit) {
				setNew("rs6", rs6);
			}
			break;
		case R.id.sevenstep:
			if (checked && !edit) {
				message.set(1,(String) rs7.getText());
			}else if(edit) {
				setNew("rs7", rs7);
			}
			break;
		case R.id.eightstep:
			if (checked && !edit) {
				message.set(1,(String) rs8.getText());
			}else if(edit) {
				setNew("rs8", rs8);
			}
			break;
		case R.id.ninestep:
			if (checked && !edit) {
				message.set(1,(String) rs9.getText());
			}else if(edit) {
				setNew("rs9", rs9);
			}
			break;
		case R.id.tenstep:
			if (checked && !edit) {
				message.set(1,(String) rs10.getText());
			}else if(edit) {
				setNew("rs10", rs10);
			}
			break;
		default:
			break;
		}
	}
	
	/**
	 * @param view
	 * 
	 * Called when a turn value radio button is pressed. Also sets the message accordingly
	 */
	public void onTurnClicked(View view) {
		boolean checked = ((RadioButton) view).isChecked();

		// Check which radio button was clicked
		switch(view.getId()) {
		case R.id.deg45:
			if (checked && !edit) {
				message.set(1,(String) rt1.getText());
			}else if(edit) {
				setNew("rt1", rt1);
			}
			break;
		case R.id.deg90:
			if (checked && !edit) {
				message.set(1,(String) rt2.getText());
			}else if(edit) {
				setNew("rt2", rt2);
			}
			break;
		case R.id.deg135:
			if (checked && !edit) {
				message.set(1,(String) rt3.getText());
			}else if(edit) {
				setNew("rt3", rt3);
			}
			break;
		case R.id.deg180:
			if (checked && !edit) {
				message.set(1,(String) rt4.getText());
			}else if(edit) {
				setNew("rt4", rt4);
			}
			break;
		case R.id.degcustom:
			if (checked && !edit) {
				editdeg.requestFocus();
			}else if(edit) {
				setNew("rt5", rt5);
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
		boolean checked = ((RadioButton) view).isChecked();

		// Check which radio button was clicked
		switch(view.getId()) {
		case R.id.forward:
			if (checked && !edit) {
				message.set(2,(String) rd1.getText());
			}else if(edit) {
				setNew("rd1", rd1);
			}
			break;
		case R.id.back:
			if (checked && !edit) {
				message.set(2,(String) rd2.getText());
			}else if(edit) {
				setNew("rd2", rd2);
			}
			break;
		case R.id.right:
			if (checked && !edit) {
				message.set(2,(String) rd3.getText());
            	message.set(4, "right");
			}else if(edit) {
				setNew("rd3", rd3);
			}
			break;
		case R.id.left:
			if (checked && !edit) {
				message.set(2,(String) rd4.getText());
            	message.set(4, "left");
			}else if(edit) {
				setNew("rd4", rd4);
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
		if(message.size() < 5) {
			sent.setText("Message size error, not sent.");
			return;
		}
		String dispMessage = new String(); 
		byte[] b = new byte[1024];
		if(ra1.isChecked()) {
			dispMessage = dispMessage.concat(message.get(0) + " " + message.get(1) + " "+ msgSteps + " " + message.get(2));
			sent.setText(dispMessage);
			b = msgToByteArray(msgSteps,dispMessage);
		} else if(ra2.isChecked()) {
			if(rt5.isChecked()){
				String custom = editdeg.getText().toString();
				if(custom.length() < 1) {
					sent.setText("Custom degrees error, not sent.");
					return;
				}
				message.set(1, custom);
			}
			dispMessage = dispMessage.concat(message.get(0) + " " + message.get(1) + " "+msgDegs+" " + message.get(2));
			sent.setText(dispMessage);
			b = msgToByteArray(msgDegs,dispMessage);
		}
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
	private byte[] msgToByteArray(String mid, String whole) {
		ArrayList<String> bA = new ArrayList<String>();
		bA.add(message.get(0));//Action
		bA.add(message.get(1));//Steps or degrees
		bA.add(mid);
		bA.add(message.get(2));//Dir
		bA.add(message.get(3));//walk or turn
		bA.add(message.get(4));//left or right
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

