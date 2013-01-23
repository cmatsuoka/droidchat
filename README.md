droidchat
=========

Very simple Android chat application only meant for learning Android development. 
Many components are present, such as services, view pager, fragments and sockets.
It is compatible with Android 2.2 and later.

The Android application is very simple, it was a learning exercise. So don't expect
fancy technologies and perfect coding.
There is also a server for the chat written in python. 
This is even more simplistic than the Android client, 
so please don't start yelling about it. 

The network protocol is also very simple.
It is based just on strings separated by special 
characters, such as '@', '#' and ';'.
So if you input some of these in your messages while using the 
chat there is a good chance something is going to break.
**AGAIN**: this whole thing is intended for learning Android development, 
so there is no point on spending time on a super fancy and secure protocol.

That being said, have fun :D

Required libraries
------------------

The following libraries are used in this application:

- **[ActionBarSherlock](http://actionbarsherlock.com/):** an extension of the support library designed to
  facilitate the use of the action bar design pattern across all versions
  of Android with a single API.

- **[ViewPagerIndicator](http://viewpagerindicator.com/):** paging indicator widgets compatible with the ViewPager
  from the Android Support Library and ActionBarSherlock.


Instructions
------------

First you need to run the chat server. 
This server will listen on port 8725:

    $ cd pychat
    $ ./pychatd
    
With the server running you can connect to it in the Android application. In the first screen of the app you should
inform the address of the server and the name you wish to have in the chat. If you are running the app on an emulator 
and the server is running on your machine, the IP address is 10.0.2.2. After connecting, the chat screen will open.
There you have 2 tabs, one for the chat messages and the other for the online clients.

There is also a command-line client for the chat, which you can use for testing:

    $ cd pychat
    $ ./pychatcli <name> <host>
    
where 'name' is the nickname you wish to have in the chat, and 'host' is the server address.





