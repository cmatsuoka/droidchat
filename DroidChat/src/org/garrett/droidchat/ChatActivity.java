package org.garrett.droidchat;

import java.util.List;

import org.garrett.droidchat.ChatClientService.ChatClientBinder;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.viewpagerindicator.PageIndicator;
import com.viewpagerindicator.TitlePageIndicator;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class ChatActivity extends SherlockFragmentActivity {
	private final int CHAT_UPDATE_RATE = 1000; // interval by which the chat must be updated
	private ChatClientService mService; // service instance
	private boolean mBound = false; // whether the service in bound or not
	private ArrayAdapter<String> messages; // messages list adapter
	private ArrayAdapter<String> clients; // online clients list adapter
	private chatFillerThread fillChatThread; // thread for updating the chat UI
	
	// adapter for creating the fragments
	class FragmentsAdapter extends FragmentStatePagerAdapter {
		private final int count = 2;

		public FragmentsAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			try {
				switch(position){
					case 0:
						MessagesFragment msgFragment = MessagesFragment.class.newInstance();
						return msgFragment;
					case 1:
						ClientsFragment cliFragment = ClientsFragment.class.newInstance();
						return cliFragment;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public int getCount() {
			return count;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch(position){
				case 0:
					return "Chat";
				case 1:
					return "Online";
			}
			return "";
		}
	}
	
	// thread for updating the chat UI
	// every CHAT_UPDATE_RATE milliseconds it calls 
	// the fillChat method
	private class chatFillerThread extends Thread {
		private boolean end = false;
		public void run(){
			while(! end){
				try {
					// this must be run on the UI thread since it manipulates
					// the interface
					ChatActivity.this.runOnUiThread(new Runnable(){
						public void run() {
							fillChat();						
						}
					});
					sleep(CHAT_UPDATE_RATE);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		public void end(){
			end = true;
		}
	}
	
	// service connection
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service){
			// get the service instance
			ChatClientBinder binder = (ChatClientBinder) service;
			mService = binder.getService();
			mService.setChatActivity(ChatActivity.this); // for future callback
			mBound = true;
		}

        @Override
        public void onServiceDisconnected(ComponentName comp) {
            mBound = false;
        }
    };
		
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        clients = new ArrayAdapter<String>(ChatActivity.this, R.layout.client);
        messages = new ArrayAdapter<String>(ChatActivity.this, R.layout.message);
        
        // Set the pager with an adapter
 		ViewPager pager = (ViewPager)findViewById(R.id.pager);
 		pager.setAdapter(new FragmentsAdapter(getSupportFragmentManager()));

 		// Bind the title indicator to the pager
 		PageIndicator indicator = (TitlePageIndicator)findViewById(R.id.title);
 		indicator.setViewPager(pager);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.activity_chat, menu);
        return true;
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        // bind to the service 
        Intent intent = new Intent(this, ChatClientService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        
        // starts the thread for updating the chat UI
        fillChatThread = new chatFillerThread();
        fillChatThread.start();
    }
    
    @Override
    protected void onStop(){
    	super.onStop();
    	
    	// this thread must run only when the chat activity is in foreground
    	fillChatThread.end();
    	
    	if(mBound)
    		unbindService(mConnection);
    }
    
    @Override
    protected void onResume(){
    	super.onResume();
    	
    	// start thread if not running
    	if(! fillChatThread.isAlive()){
    		fillChatThread = new chatFillerThread();
            fillChatThread.start();
    	}
    }
    
    // update the UI with the messages and online clients
    // messages and clients lists are provided by the service
    public void fillChat(){
        if(mBound){
        	// get data from service
        	List<String[]> mList = mService.getMessages();
        	String[] cList = mService.getClients();
        	
        	// update online clients view
        	if(cList == null)
        		return;
        	clients.clear();
        	for(int i=0; i<cList.length; i++)
        		clients.add(cList[i]);
        	clients.notifyDataSetChanged();

        	// update messages view
        	if(mList == null)
        		return;
        	messages.clear();
        	for(int i=0; i<mList.size(); i++){
        		String[] msg = mList.get(i);
        		messages.add(msg[1] + ": " + msg[2]);
        	}
        	messages.notifyDataSetChanged();        
        }
    }
    
    // exit button handler
    // disconnects and closes the chat activity
    public boolean exit(MenuItem item){
    	if(mBound){
	    	mService.end();
	    	stopService(new Intent(this, ChatClientService.class));
	    	finish();
    	}
    	return true;
    }
    
    // handler for the exit button, same as the 'exit' method
    // this is also needed for compatibility reasons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.exit:
                return exit(item);
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    // send message
    public void sendMsg(String msg){
    	mService.sendMsg(msg);
    }
    
    // connection was lost, notify and finish the activity
    public void connectionLost(){
    	notify(getString(R.string.connection_lost));
    	exit(null);
    }
    
    public ArrayAdapter<String> getClients(){
    	return clients;    	
    }
    
    public ArrayAdapter<String> getMessages(){
    	return messages;    	
    }
    
    // show a notification using Toast
    // this must run on the UI thread
    private void notify(final String msg){
    	ChatActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(ChatActivity.this, msg, Toast.LENGTH_LONG).show();
			}
		});
    }
    
    // back button should not go to the login screen
//    @Override
//    public void onBackPressed() {
//    	Intent intent = new Intent();
//    	intent.setAction(Intent.ACTION_MAIN);
//    	intent.addCategory(Intent.CATEGORY_HOME);
//
//    	startActivity(intent);
//
//    	return;
//    }
    
}

