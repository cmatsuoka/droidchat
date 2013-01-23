package org.garrett.droidchat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;

// online clients fragment
public class ClientsFragment extends SherlockFragment {
	private ListView onlineList;
	ChatActivity chat;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.online_clients, container, false);
		
		chat = (ChatActivity)getActivity();
		
		// get the necessary views
	    onlineList = (ListView) view.findViewById(R.id.onlineClients);
	    
	    // create adapters for the lists
	    onlineList.setAdapter(chat.getClients());
	    
		return view;
	}
}
