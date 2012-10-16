package beit.skn.pingmeagent;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import beit.skn.classes.PushableMessage;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.widget.Toast;

public class AgentCommunicatorService extends Service
{
	private static final String IP_ADDRESS = "192.168.0.101";
	private static final int AGENT_PORT_NUMBER = 9976;	
	public static final String INTENT_TO_SERVICE = "PingMeIntentToService";
	public static final String INTENT_TO_ACTIVITY = "PingMeIntentToActivity";	
	
	private static boolean isAuthentic = false;
	private static Socket socket = null;
	private static String uname = null;	
	private static BroadcastReceiver brSendRequested = null;
	private static IntentFilter ifSendRequested = null;
	
	private Thread incomingMessageReader = new Thread()
	{
		
		@Override
		public void run() 
		{
			PushableMessage m;
			while(true)
			{
				m = AgentTalker.readMessage();
				Intent iReadRequested = new Intent();
				iReadRequested.setAction(INTENT_TO_ACTIVITY);
				iReadRequested.putExtra("pushablemessage", m);
				sendOrderedBroadcast(iReadRequested, null, new BroadcastReceiver()
				{
					@Override
					public void onReceive(Context context, Intent intent)
					{
						int result = getResultCode();
						switch(result)
						{
						case Activity.RESULT_OK:						
							break;
							
						default:
							showTempNotification((PushableMessage)intent.getSerializableExtra("pushablemessage"));
							break;
						}
					}				
				}, null, Activity.RESULT_CANCELED, null, null);
			}
		}
		
	};
	
	public static String getUname() 
	{
		return uname;
	}

	public static void setUname(String uname) 
	{
		AgentCommunicatorService.uname = uname;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		return START_STICKY;
	}

	@Override
	public void onCreate() 
	{
		try 
		{
			socket = new Socket(IP_ADDRESS, AGENT_PORT_NUMBER);			
		} 
		catch (UnknownHostException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		AgentTalker.setSocket(socket);
		brSendRequested = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context arg0, Intent arg1) 
			{
				PushableMessage m = (PushableMessage)arg1.getSerializableExtra("pushablemessage");
				if(m.getControl().contentEquals("hello") && !isAuthentic)
				{				
					AgentTalker.pushMessage(m);
					m = AgentTalker.readMessage();
					if(m.getControl().contentEquals("authentic"))
					{
						Toast.makeText(getApplicationContext(), "Authenticated and registered on server", Toast.LENGTH_LONG).show();
						Intent iIsAuthentic = new Intent();
						iIsAuthentic.setAction(INTENT_TO_ACTIVITY);
						sendBroadcast(iIsAuthentic);
						showPersistentNotification();	
						isAuthentic = true;
						incomingMessageReader.start();						
					}
					else
					{
						Toast.makeText(getApplicationContext(), "Could not authenticate.", Toast.LENGTH_LONG).show();
						stopSelf();
					}
										
				}
				else if(m.getControl().contentEquals("hello") && isAuthentic)
				{
					Intent iIsAuthentic = new Intent();
					iIsAuthentic.setAction(INTENT_TO_ACTIVITY);
					sendBroadcast(iIsAuthentic);
				}
				else
					AgentTalker.pushMessage(m);
			}			
		};
		ifSendRequested = new IntentFilter();
		ifSendRequested.addAction(INTENT_TO_SERVICE);
		registerReceiver(brSendRequested, ifSendRequested);	
		super.onCreate();
	}
	
	
	
	@Override
	public void onDestroy() 
	{
		try
		{
			unregisterReceiver(brSendRequested);
		}
		catch(IllegalArgumentException iae)
		{
			// Do nothing
		}		
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent arg0) 
	{
		return null;
	}
	
	private void showPersistentNotification()
	{
		NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        CharSequence text = getText(R.string.servicetext);
        Notification notification = new Notification(R.drawable.icon, text, 0);
        Intent showActivity = new Intent(this, DashboardActivity.class);
        showActivity.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);   
        showActivity.setAction(INTENT_TO_ACTIVITY);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, showActivity, 0);
        notification.setLatestEventInfo(this, getText(R.string.servicename), text, contentIntent);
        notification.flags = Notification.FLAG_NO_CLEAR ^ Notification.FLAG_ONGOING_EVENT;        
        nm.notify(R.string.servicetext, notification);
	}
	
	private void showTempNotification(PushableMessage m)
	{
		NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        CharSequence text = getText(R.string.notificationtext);
        Notification notification = new Notification(R.drawable.icon, text, 0);
        Intent showActivity = new Intent(this, DashboardActivity.class);
        showActivity.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        showActivity.putExtra("pushablemessage", m);
        showActivity.setAction(INTENT_TO_ACTIVITY);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, showActivity, 0);
        notification.setLatestEventInfo(this, getText(R.string.servicename), text, contentIntent);
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        nm.notify(R.string.notificationtext, notification);
	}
	

	
}
