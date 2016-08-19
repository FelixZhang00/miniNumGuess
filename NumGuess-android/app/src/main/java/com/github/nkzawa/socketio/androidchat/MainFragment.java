package com.github.nkzawa.socketio.androidchat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A chat fragment containing messages view and input form.
 */
public class MainFragment extends Fragment {

  private static final int REQUEST_LOGIN = 0;

  private static final String TAG = "MainFragment";

  private RecyclerView mMessagesView;
  private EditText mInputMessageView;
  private List<Message> mMessages = new ArrayList<Message>();
  private RecyclerView.Adapter mAdapter;
  private String mUsername;
  private boolean mIsKing;
  private Socket mSocket;

  private Boolean isConnected = true;

  public MainFragment() {
    super();
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    mAdapter = new MessageAdapter(activity, mMessages);
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setHasOptionsMenu(true);

    ChatApplication app = (ChatApplication) getActivity().getApplication();
    mSocket = app.getSocket();
    mSocket.on(Socket.EVENT_CONNECT, onConnect);
    mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
    mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
    mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
    mSocket.on("new message", onNewMessage);
    mSocket.on("user joined", onUserJoined);
    mSocket.on("user left", onUserLeft);
    mSocket.connect();

    startSignIn();
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_main, container, false);
  }

  @Override public void onDestroy() {
    super.onDestroy();

    mSocket.disconnect();

    mSocket.off(Socket.EVENT_CONNECT, onConnect);
    mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
    mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
    mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
    mSocket.off("new message", onNewMessage);
    mSocket.off("user joined", onUserJoined);
    mSocket.off("user left", onUserLeft);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    mMessagesView = (RecyclerView) view.findViewById(R.id.messages);
    mMessagesView.setLayoutManager(new LinearLayoutManager(getActivity()));
    mMessagesView.setAdapter(mAdapter);

    mInputMessageView = (EditText) view.findViewById(R.id.message_input);
    mInputMessageView.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override public void afterTextChanged(Editable s) {
      }
    });

    ImageButton sendButton = (ImageButton) view.findViewById(R.id.send_button);
    sendButton.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        if (mIsKing) {
          giveNumber();
        } else {
          guessNumber();
        }
      }
    });
  }


  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (Activity.RESULT_OK != resultCode) {
      getActivity().finish();
      return;
    }

    mUsername = data.getStringExtra("username");
    int numUsers = data.getIntExtra("numUsers", 1);
    mIsKing = data.getBooleanExtra("isKing", false);

    addLog(getResources().getString(R.string.message_welcome));
    addParticipantsLog(numUsers);
    if (mIsKing) {
      addLog("You are 庄家.");
    } else {
      addLog("You are 玩家.");
    }
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    // Inflate the menu; this adds items to the action bar if it is present.
    inflater.inflate(R.menu.menu_main, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_leave) {
      leave();
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  private void addLog(String message) {
    mMessages.add(new Message.Builder(Message.TYPE_LOG).message(message).build());
    mAdapter.notifyItemInserted(mMessages.size() - 1);
    scrollToBottom();
  }

  private void addParticipantsLog(int numUsers) {
    addLog(getResources().getQuantityString(R.plurals.message_participants, numUsers, numUsers));
  }

  private void addMessage(String username, String message) {
    mMessages.add(
        new Message.Builder(Message.TYPE_MESSAGE).username(username).message(message).build());
    mAdapter.notifyItemInserted(mMessages.size() - 1);
    scrollToBottom();
  }

  /**
   * 庄家给服务器一个数字
   */
  private void giveNumber() {
    if (!mSocket.connected()) return;

    String message = mInputMessageView.getText().toString().trim();
    if (TextUtils.isEmpty(message)) {
      mInputMessageView.requestFocus();
      return;
    }

    mInputMessageView.setText("");
    addMessage(mUsername, message);

    // perform the sending message attempt.
    mSocket.emit("give number", Integer.parseInt(message));
  }

  /**
   * 玩家猜一个数字
   */
  private void guessNumber() {
    if (!mSocket.connected()) return;

    String message = mInputMessageView.getText().toString().trim();
    if (TextUtils.isEmpty(message)) {
      mInputMessageView.requestFocus();
      return;
    }

    mInputMessageView.setText("");
    addMessage(mUsername, message);

    // perform the sending message attempt.
    mSocket.emit("guess number", Integer.parseInt(message));
  }

  private void attemptSend() {
    if (null == mUsername) return;
    if (!mSocket.connected()) return;


    String message = mInputMessageView.getText().toString().trim();
    if (TextUtils.isEmpty(message)) {
      mInputMessageView.requestFocus();
      return;
    }

    mInputMessageView.setText("");
    addMessage(mUsername, message);

    // perform the sending message attempt.
    mSocket.emit("new message", message);
  }

  private void startSignIn() {
    mUsername = null;
    Intent intent = new Intent(getActivity(), LoginActivity.class);
    startActivityForResult(intent, REQUEST_LOGIN);
  }

  private void leave() {
    mUsername = null;
    mSocket.disconnect();
    mSocket.connect();
    startSignIn();
  }

  private void scrollToBottom() {
    mMessagesView.scrollToPosition(mAdapter.getItemCount() - 1);
  }

  private Emitter.Listener onConnect = new Emitter.Listener() {
    @Override public void call(Object... args) {
      getActivity().runOnUiThread(new Runnable() {
        @Override public void run() {
          if (!isConnected) {
            if (null != mUsername) mSocket.emit("add user", mUsername);
            Toast.makeText(getActivity().getApplicationContext(), R.string.connect,
                Toast.LENGTH_LONG).show();
            isConnected = true;
          }
        }
      });
    }
  };

  private Emitter.Listener onDisconnect = new Emitter.Listener() {
    @Override public void call(Object... args) {
      getActivity().runOnUiThread(new Runnable() {
        @Override public void run() {
          isConnected = false;
          Toast.makeText(getActivity().getApplicationContext(), R.string.disconnect,
              Toast.LENGTH_LONG).show();
        }
      });
    }
  };

  private Emitter.Listener onConnectError = new Emitter.Listener() {
    @Override public void call(final Object... args) {
      getActivity().runOnUiThread(new Runnable() {
        @Override public void run() {
          Toast.makeText(getActivity().getApplicationContext(), R.string.error_connect,
              Toast.LENGTH_LONG).show();
        }
      });
    }
  };

  private Emitter.Listener onNewMessage = new Emitter.Listener() {
    @Override public void call(final Object... args) {
      getActivity().runOnUiThread(new Runnable() {
        @Override public void run() {
          Log.d(TAG,"onNewMessage");
          JSONObject data = (JSONObject) args[0];
          String username;
          String message;
          boolean isLog = false;
          try {
            isLog = data.getBoolean("isLog");
          } catch (JSONException e) {
            return;
          }

          Log.d(TAG, "isLog = " + isLog);

          if (isLog) {
            try {
              message = data.getString("message");
            } catch (JSONException e) {
              return;
            }
            addLog(message);
          } else {
            try {
              username = data.getString("username");
              message = data.getString("message");
            } catch (JSONException e) {
              return;
            }
            addMessage(username, message);
          }
        }
      });
    }
  };

  private Emitter.Listener onUserJoined = new Emitter.Listener() {
    @Override public void call(final Object... args) {
      getActivity().runOnUiThread(new Runnable() {
        @Override public void run() {
          JSONObject data = (JSONObject) args[0];
          String username;
          int numUsers;
          try {
            username = data.getString("username");
            numUsers = data.getInt("numUsers");
          } catch (JSONException e) {
            return;
          }

          addLog(getResources().getString(R.string.message_user_joined, username));
          addParticipantsLog(numUsers);
        }
      });
    }
  };

  private Emitter.Listener onUserLeft = new Emitter.Listener() {
    @Override public void call(final Object... args) {
      getActivity().runOnUiThread(new Runnable() {
        @Override public void run() {
          JSONObject data = (JSONObject) args[0];
          String username;
          int numUsers;
          try {
            username = data.getString("username");
            numUsers = data.getInt("numUsers");
          } catch (JSONException e) {
            return;
          }

          addLog(getResources().getString(R.string.message_user_left, username));
          addParticipantsLog(numUsers);
        }
      });
    }
  };




}

