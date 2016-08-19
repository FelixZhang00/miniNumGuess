package com.github.nkzawa.socketio.androidchat;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;

public class ChatApplication extends Application {

  private static final String TAG = "ChatApplication";
  private Socket mSocket;

  @Override public void onCreate() {
    try {
      SharedPreferences sharedPreferences =
          getSharedPreferences(Constants.SP_CONFIG_NAME, Context.MODE_PRIVATE);
      String ip = Constants.CHAT_SERVER_URL;
      if (sharedPreferences != null) {
        ip =sharedPreferences.getString(Constants.CFG_IP, Constants.CHAT_SERVER_URL).trim();
        if(!ip.contains("http://")){
          ip = "http://"+ip;
        }
        Log.d(TAG,"IP="+ip);
      }
      //ip = "http://test.tv.video.qq.com/guess_number/echo";
      ip = "http://test.tv.video.qq.com/guess_number";
      mSocket = IO.socket(ip);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    super.onCreate();
  }

  public Socket getSocket() {
    return mSocket;
  }
}
