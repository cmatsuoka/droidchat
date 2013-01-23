package org.garrett.droidchat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class ChatClientService extends Service {
	private final int SERVER_PORT = 8725; // server port
	private final int TIMEOUT = 4000; // default timeout
	
	private final IBinder mBinder = new ChatClientBinder(); // service binder	
	private String name; // client name
	private String serverHost; // server address
	private Socket socket; // socket for the connection
	private DataOutputStream sockOut; // stream for sending data
	private DataInputStream sockIn; // stream for receiving data
	private serverListenerThread listenServerThread; // thread for listening to the server 
	private ChatActivity chat; // chat activity instance
	private String[] clients; // list of connected clients
	private List<String[]> messages; // list of messages (ID, client, text)
	private boolean connected = false;
	
	public class ChatClientBinder extends Binder {
		public ChatClientService getService() {
			return ChatClientService.this;
		}
	}
	
	// this thread listen to the server messages in order to get 
	// messages and connected clients. If new messages were received
	// and the ChatActivity is not focused it raises a notification.
	// if connection is lost the service stops and the activity informed
	// through a callback
	private class serverListenerThread extends Thread {
		private boolean end = false; // used for finishing the thread
		private int lastID; // needed for checking if there are new messages
				
		public void run(){
			lastID = -1;
			while(! end){
				List<String[]> msgList = new ArrayList<String[]>();
				byte[] buf = new byte[1000];
				int sz;
				String pkg;

				// receiving message
				try{
					sz = sockIn.read(buf);
					pkg = (new String(buf)).substring(0, sz).trim();
				}
				catch (Exception e) {
					// connection lost
					if(connected){
						messages.clear();
						chat.connectionLost();
					}
					break;
				}
				
				// parsing message
				// Message format is: clients#messages
				// clients format is: client1;client2;client3;...
				// messages format is: message1;message2;message3;...
				// each message format is: ID@client@text 
				try {
					String[] data = pkg.split("#");
										
					clients = data[0].split(";");
					
					String[] msgData = data[1].split(";");
					for(int i=0; i<msgData.length; i++){
						try {
							if(msgData[i].indexOf((int)'@') != -1){
								msgList.add(msgData[i].split("@"));
							}
						}
						catch (Exception e) {
						}
					}		
					
					messages = msgList;
								
					// check if there are new messages
					if(msgList.size() > 0){
						int newLastID = Integer.parseInt(msgList.get(msgList.size()-1)[0]);
						if(newLastID != lastID){
							lastID = newLastID;
							String[] msg = msgList.get(msgList.size()-1);
							// notify the new message
							if((chat != null) && (! chat.hasWindowFocus())){
								notifyNewMessage(msg[2]);
							}
						}
					}
				}
				catch (Exception e) {
				}
			}
		}
		
		public void end(){
			end = true;
		}
	}
	
	public void setChatActivity(ChatActivity chat){
		this.chat = chat;
	}
	
	public String[] getClients(){
		return clients;
	}

	public List<String[]> getMessages(){
		return messages;
	}
	
	// stop listening and disconnect
	public void end(){
		listenServerThread.end();
		connected = false;
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// starts the service by connecting to the server and starting the listener thread
	// also avoid reconnecting if a connection already exists
	public void start(String host, String cName, final LoginActivity login){
		serverHost = host;
		name = cName;
		
		// the connection must run on another thread since 
		// this method is called by the UI thread (LoginActivity).
		// A callback is used to inform the activity whether the 
		// connection was successful or not
		new Thread(new Runnable(){
	        public void run() {
	        	if(! connected){
	        		if(connectServer()){
	        			messages = new ArrayList<String[]>();
		        		login.connectionStarted();
		        		listenServerThread = new serverListenerThread();
		        		listenServerThread.start();
		        		connected = true;
		        	}
		        	else {
		        		connected = false;
		        		login.connectionFailed();
		        	}	        		
	        	}
	        	else {
	        		login.connectionStarted();
	        	}
	        }
	    }).start();
	}
	
	// connect to the server
	private boolean connectServer(){
		socket = new Socket();
		SocketAddress addr = new InetSocketAddress(serverHost, SERVER_PORT);

		try {
			socket.setSoTimeout(TIMEOUT);
			socket.connect(addr, TIMEOUT);
			sockOut = new DataOutputStream(socket.getOutputStream());
			sockIn = new DataInputStream(socket.getInputStream());
		
			// send name
			sockOut.writeBytes(name);
		}
		catch (Exception e) {
			return false;
		}
		
		return true;
	}
	
	// raise a notification informing that new messages were received
	// clicking on it will open the chat
	private void notifyNewMessage(String msg){
	    	// just raise a notification for now
	    	NotificationCompat.Builder mBuilder =
	    	        new NotificationCompat.Builder(this)
	    			.setSmallIcon(R.drawable.ic_launcher)
	    	        .setContentTitle("New messages received !")
	    	        .setContentText(msg);
	    	
	    	NotificationManager mNotificationManager =
	    		    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	    	
	    	Intent intent = new Intent(this, ChatActivity.class);
	    	PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	    
	    	mBuilder.setContentIntent(pIntent);
	    	mNotificationManager.notify(0, mBuilder.getNotification());
	    }

	@Override
	public IBinder onBind(Intent intent) {
		return this.mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return true;
	}
	
	public boolean isConnected(){
		return connected;
	}

	// send a message to the server
	// sending a message through the network must be done on a thread
	// since this method will be called by the UI thread (ChatActivity)
	public void sendMsg(final String msg){
		new Thread(new Runnable() {
	        public void run() {
	        	try {
					sockOut.writeBytes(msg);
				} catch (IOException e) {
					e.printStackTrace();
				}        	
	        }
	    }).start();
	}
}
