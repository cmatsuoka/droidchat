package org.garrett.droidchat;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockFragment;

// messages list fragment
public class MessagesFragment extends SherlockFragment {
	private ChatActivity chat;
	private EditText inputMsg;
	private ListView msgList;

    // send button clicked
    public void sendClicked(){
    	String msg = inputMsg.getText().toString();
    	if(msg.length() > 0){
    		chat.sendMsg(msg);
    		inputMsg.setText("");
    	}
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.message_list, container, false);
		
		chat = (ChatActivity)getActivity();
		
		// get the necessary views
		msgList = (ListView) view.findViewById(R.id.chatMsg);
		inputMsg = (EditText) view.findViewById(R.id.chatInput);
		
	    // create adapters for the lists
	    msgList.setAdapter(chat.getMessages());
	    
	    // set a key listener in order to catch the "enter" key
	    inputMsg.setOnKeyListener(new OnKeyListener(){
	        public boolean onKey(View v, int keyCode, KeyEvent event){
	            if (event.getAction() == KeyEvent.ACTION_DOWN){
	                switch (keyCode){
	                    case KeyEvent.KEYCODE_DPAD_CENTER:
	                    case KeyEvent.KEYCODE_ENTER:
	                    	sendClicked();
	                        return true;
	                    default:
	                        break;
	                }
	            }
	            return false;
	        }
	    });
	    
	    // send msg when button is pressed
	    ((Button)view.findViewById(R.id.sendMsgButton)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendClicked();				
			}
		});
	    
		return view;
	}
	
}
