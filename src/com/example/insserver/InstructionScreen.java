package com.example.insserver;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class InstructionScreen extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState){
	super.onCreate(savedInstanceState);// Add THIS LINE

	    setContentView(R.layout.instructions);
	}
	
	public void done(View view) {
		finish();
	}
}
