package org.garrett.droidchat;

import org.garrett.droidchat.ChatClientService.ChatClientBinder;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {
	private ChatClientService mService; // service instance
	private boolean mBound = false; // whether the service in bound or not
	
	// service connection
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service){
			// get the service instance
			ChatClientBinder binder = (ChatClientBinder) service;
			mService = binder.getService();
			mBound = true;
			
			if(mService.isConnected()){
				connectionStarted();
			}
		}

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mBound = false;
        }
    };
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, ChatClientService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        
        setContentView(R.layout.activity_login);
        
        // set a key listener in order to catch the "enter" key
        OnKeyListener keyListener = new OnKeyListener(){
            public boolean onKey(View v, int keyCode, KeyEvent event){
                if (event.getAction() == KeyEvent.ACTION_DOWN){
                    switch (keyCode){
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            connectButtonClicked(null);
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        };
        
        findViewById(R.id.loginServerText).setOnKeyListener(keyListener);
        findViewById(R.id.loginNameText).setOnKeyListener(keyListener);
        
    }
    
    @Override
    protected void onDestroy(){
    	if(mBound)
    		unbindService(mConnection);
    	super.onDestroy();
    }

    // callback used by the service when the connection to the server is ready
    public void connectionStarted(){
    	Intent intent = new Intent(this, ChatActivity.class);
    	startActivity(intent);
    }
    
    // callback used by the service when the connection to the server failed
    public void connectionFailed(){
    	notify(getString(R.string.connection_failed));
    }
    
    // connect to the server using a service
    // the service will call a callback method when the connection is ready
    public void connectButtonClicked(View button){
    	if(!mBound)
    		return;
    	
    	String server = ((TextView) findViewById(R.id.loginServerText)).getText().toString();
    	String name = ((TextView) findViewById(R.id.loginNameText)).getText().toString();
    	
    	mService.start(server, name, this);
    }
    
    // show a notification using Toast
    // this must run on the UI thread
    private void notify(final String msg){
    	LoginActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
			}
		});
    }
}
