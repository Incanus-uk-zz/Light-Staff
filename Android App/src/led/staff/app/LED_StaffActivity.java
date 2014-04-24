package led.staff.app;

import android.app.Activity;
import android.os.Bundle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID; 
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.SeekBar;
import android.graphics.Color;



/* Primitives
 * 0 : Staff all one Colour
 * 1 : Smooth Change
 * 2 : Swipe
 * 3 : Chase
 * 4 : Data
 * 5 : Wait
*/
public class LED_StaffActivity extends Activity{
	
		// Intent request codes
    	private static final int REQUEST_ENABLE_BT = 3;

       
        private static final String TAG = "LED Staff";

        private BluetoothAdapter mBluetoothAdapter = null;
        private BluetoothSocket btSocket = null;
        private OutputStream outStream = null;
        private InputStream inStream = null;
        
        private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
 
        private static String address = "00:06:66:45:B6:D4";
        	
        private SeekBar redSeek;
        private SeekBar blueSeek;
        private SeekBar greenSeek;
        
        private Button smoothButton;
        private Button chaseButton;
        private Button swipeButton;
        private Button setColourButton;
        private Button dataButton;
        
        private Spinner smoothPeriod;
        private Spinner smoothRes;
        private Spinner swipeSpeed;
        
        private CheckBox liveCheckBox;
        private CheckBox swipeInCheckBox;
        
        private View mainLayout;
        
        int redValue=0;
        int greenValue=0;
        int blueValue=0;
        
        int max = 127;
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.main);
  
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter == null) {
                        Toast.makeText(this,
                                "Bluetooth is not available.",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                }
 
                if (!mBluetoothAdapter.isEnabled()) {
                	Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);                }
                
                mainLayout = (View) findViewById(R.id.mainLayout);
                
                redSeek=(SeekBar) findViewById(R.id.seekBarRed);
                greenSeek=(SeekBar) findViewById(R.id.seekBarGreen);
                blueSeek=(SeekBar) findViewById(R.id.seekBarBlue);
                
                smoothButton = (Button) findViewById(R.id.smoothButton);
                chaseButton = (Button) findViewById(R.id.chaseButton);
                swipeButton = (Button) findViewById(R.id.swipeButton);
                setColourButton = (Button) findViewById(R.id.setColourButton);
                dataButton = (Button) findViewById(R.id.dataButton);
                
                liveCheckBox = (CheckBox) findViewById(R.id.liveUpdateOnOff);
                swipeInCheckBox = (CheckBox) findViewById(R.id.swipeCheck);
                
                smoothPeriod = (Spinner) findViewById(R.id.smoothPeriod);
                smoothRes = (Spinner) findViewById(R.id.smoothRes);
                swipeSpeed = (Spinner) findViewById(R.id.swipeSpeed);
                
                ArrayAdapter<CharSequence> adapterPeriod = ArrayAdapter.createFromResource(this, R.array.period, android.R.layout.simple_spinner_item);
                ArrayAdapter<CharSequence> adapterRes = ArrayAdapter.createFromResource(this, R.array.res, android.R.layout.simple_spinner_item);
                adapterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                adapterRes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                
                smoothPeriod.setAdapter(adapterPeriod);
                smoothRes.setAdapter(adapterRes);
                swipeSpeed.setAdapter(adapterPeriod);
                
                
                redSeek.setMax(max);
                greenSeek.setMax(max);
                blueSeek.setMax(max);
                
                redSeek.setOnSeekBarChangeListener(mSeekBarListener);
                greenSeek.setOnSeekBarChangeListener(mSeekBarListener);
                blueSeek.setOnSeekBarChangeListener(mSeekBarListener);

                smoothButton.setOnClickListener(smoothButtonListener);
                chaseButton.setOnClickListener(chaseButtonListener);
                swipeButton.setOnClickListener(swipeButtonListener);
                setColourButton.setOnClickListener(setColourButtonListener);
                dataButton.setOnClickListener(dataButtonListener);
        }
        
        private View.OnClickListener smoothButtonListener = new View.OnClickListener() {
			// Smooth Transition, 1, period, res, r, g, b
			@Override
			public void onClick(View v) {
				String periodString = smoothPeriod.getSelectedItem().toString();
				String resString = smoothRes.getSelectedItem().toString();
				
				int periodInt = (int)(Double.parseDouble(periodString) * 10);
				int resInt = Integer.parseInt(resString);
				
        		redValue = redSeek.getProgress();
        		greenValue = greenSeek.getProgress();
        		blueValue = blueSeek.getProgress();

        		ByteArrayOutputStream byteArray= new ByteArrayOutputStream();
	        	
        		byteArray.write(1);
        		byteArray.write(resInt);
        		byte[] byteBuffer = byteArray.toByteArray();
        		sendOverBT(byteBuffer);		
        		
        		int[] staffState = null;
        		
        		try {
					staffState = getStaffState();
				} catch (IOException e) {
					Log.e(TAG, "Smooth Transition: failed to get Staff State.", e);
				}
        		
        		int j, r, g, b;

        		float[] hsv = new float[3]; //led values
    			float[][] hsvp = new float[staffState.length][]; //HSV staffArray
    			
    			float[] hsv2 = new float[3]; // goal Colour in HSV
    			Color.RGBToHSV(redValue, greenValue, blueValue, hsv2);
    			
    			float pcent = (float) (1.0/resInt);
    			
        		for (j = 0; j < staffState.length; j+=3){
        			r = staffState[j];
        			g = staffState[j+1];
        			b = staffState[j+2];
        			Color.RGBToHSV(r, g, b, hsv);
        			hsvp[j] = hsv;
        		}
        		int counter = 0;
        		float[] hsvNew = new float[3];
        		
        		while (counter <= resInt){
//        			try {
//						readOverBT();
//					} catch (IOException e) {
//						Log.e(TAG, "Smooth Transition: Failed to read BT for check byte", e);
//					}
            		ByteArrayOutputStream byteArray2 = new ByteArrayOutputStream();
        			float p = pcent * counter;
        			for (j = 0; j < staffState.length; j+=3){
        				if (Math.abs(hsvp[j][0] - hsv2[0]) > 180){
        					if (hsv2[0] > hsvp[j][0]){  
        						hsv2[0]-= 360;
        					} else {
        						hsvp[j][0] -= 360;
        					}
        				}
        				float offset = hsv2[0]; 
        				
        				hsvNew[0] = ((1 - p) * (hsvp[j][0]-offset));
        				hsvNew[0] += offset + 360;
        				hsvNew[0] = hsvNew[0] % 360;
        				hsvNew[1] = (1 - p) * hsvp[j][1] + p * hsv2[1];
        				hsvNew[2] = (1 - p) * hsvp[j][2] + p * hsv2[2];
        				
        				
        				int rgb = Color.HSVToColor(hsvNew);
            			r = Color.red(rgb);
            			g = Color.green(rgb);
            			b = Color.blue(rgb);
            			byteArray2.write(r);
            			byteArray2.write(g);
            			byteArray2.write(b);
        			}
            		byte[] byteBuffer2 = byteArray2.toByteArray();
            		sendOverBT(byteBuffer2);
        			counter++;

        		}        		
			}
		};

        private View.OnClickListener chaseButtonListener = new View.OnClickListener() {
			// Not Done
			@Override
			public void onClick(View v) {
	        	ByteArrayOutputStream byteArray= new ByteArrayOutputStream();
	        	byteArray.write(3);
        		byte[] byteBuffer = byteArray.toByteArray();
        		sendOverBT(byteBuffer);	

			}
		};

        private View.OnClickListener swipeButtonListener = new View.OnClickListener() {
			// 2, speed, out or in, r, g, b
			@Override
			public void onClick(View v) {
				
				String speedString = swipeSpeed.getSelectedItem().toString();
				int speedInt = (int)(Double.parseDouble(speedString) * 10);
				
				boolean in = swipeInCheckBox.isChecked(); 
				
        		redValue = redSeek.getProgress();
        		greenValue = greenSeek.getProgress();
        		blueValue = blueSeek.getProgress();
        		
	        	ByteArrayOutputStream byteArray= new ByteArrayOutputStream();
	        	byteArray.write(2);
        		byteArray.write(speedInt);
        		if (in){
            		byteArray.write(0);        			
        		}
        		else {
        			byteArray.write(1);
        		}
        		byteArray.write(redValue);
        		byteArray.write(greenValue);
        		byteArray.write(blueValue);
        		
        		byte[] byteBuffer = byteArray.toByteArray();
        		sendOverBT(byteBuffer);	
			}
		};

        private View.OnClickListener setColourButtonListener = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
	        	ByteArrayOutputStream byteArray= new ByteArrayOutputStream();
	        	byteArray.write(0);
        		redValue = redSeek.getProgress();
        		greenValue = greenSeek.getProgress();
        		blueValue = blueSeek.getProgress();
        	
        		byteArray.write(redValue);
        		byteArray.write(greenValue);
        		byteArray.write(blueValue);
        	
        		byte[] byteBuffer = byteArray.toByteArray();
        		sendOverBT(byteBuffer);			
			}
		};

        private View.OnClickListener dataButtonListener = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
			}
		};

		private SeekBar.OnSeekBarChangeListener mSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {

				redValue = redSeek.getProgress();
        		greenValue = greenSeek.getProgress();
        		blueValue = blueSeek.getProgress();
				
        		
				mainLayout.setBackgroundColor(Color.rgb(redValue, greenValue, blueValue));
	        	if (liveCheckBox.isChecked()){
		        	ByteArrayOutputStream byteArray= new ByteArrayOutputStream();
		        	byteArray.write(0);
	        	
	        		byteArray.write(redValue);
	        		byteArray.write(greenValue);
	        		byteArray.write(blueValue);
	        	
	        		byte[] byteBuffer = byteArray.toByteArray();
	        		sendOverBT(byteBuffer);			
	        	}
			}
		};
 
        @Override
        public void onStart() {
                super.onStart();
        }
 
        @Override
        public void onResume() {
                super.onResume();
  
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
               
                try {
                        btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                } catch (IOException e) {
                        Log.e(TAG, "ON RESUME: Socket creation failed.", e);
                }
 
                mBluetoothAdapter.cancelDiscovery();
 
                try {
                        btSocket.connect();
                        Log.e(TAG, "ON RESUME: BT connection established, data transfer link open.");
                } catch (IOException e) {
                        try {
                                btSocket.close();
                        } catch (IOException e2) {
                                Log.e(TAG,
                                        "ON RESUME: Unable to close socket during connection failure", e2);
                        }
                }
 
                try {
                        outStream = btSocket.getOutputStream();
                        
                } catch (IOException e) {
                        Log.e(TAG, "ON RESUME: Output stream creation failed.", e);
                }
                
                try {
                	inStream = btSocket.getInputStream();
                } catch (IOException e){
                	Log.e(TAG, "ON RESUME: Input stream creation failed.", e);
                }
        }
 
        @Override
        public void onPause() {
                super.onPause();
  
                if (outStream != null) {
                        try {
                                outStream.flush();
                        } catch (IOException e) {
                                Log.e(TAG, "ON PAUSE: Couldn't flush output stream.", e);
                        }
                }
 
                try     {
                        btSocket.close();
                } catch (IOException e2) {
                        Log.e(TAG, "ON PAUSE: Unable to close socket.", e2);
                }
        }
                
        public void sendOverBT(byte[] byteBuffer){
        	try {
        		outStream.write(byteBuffer);
        	} catch (IOException e) {
                Log.e(TAG, "sendOverBT: Exception during write.", e);
        	}
        }
        
        public int readOverBT() throws IOException{  
        	int intByte = -1;
        	while (intByte == -1){
        		intByte = inStream.read();
        	}
        	return intByte;
        }
        
        public int[] getStaffState() throws IOException{
        	int[] staffArray;
        	int leds = readOverBT();
        	staffArray = new int[leds*3];
        	int j = 0;
        	for (j=0; j<leds*3; j++){
        		staffArray[j] = readOverBT();
        	}
        	return staffArray;
        }
 }
