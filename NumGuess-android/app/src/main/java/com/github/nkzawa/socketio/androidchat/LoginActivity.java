package com.github.nkzawa.socketio.androidchat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A login screen that offers login via username.
 */
public class LoginActivity extends Activity {

  private static final String TAG = "LoginActivity";
  private EditText mUsernameView;

  private String mUsername;

  private Socket mSocket;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    ChatApplication app = (ChatApplication) getApplication();
    mSocket = app.getSocket();

    // Set up the login form.
    mUsernameView = (EditText) findViewById(R.id.username_input);
    mUsernameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
        if (id == R.id.login || id == EditorInfo.IME_NULL) {
          attemptLogin();
          return true;
        }
        return false;
      }
    });

    Button signInButton = (Button) findViewById(R.id.sign_in_button);
    signInButton.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View view) {
        attemptLogin();
      }
    });

    mSocket.on("login", onLogin);
  }

  @Override protected void onDestroy() {
    super.onDestroy();

    mSocket.off("login", onLogin);
  }

  /**
   * Attempts to sign in the account specified by the login form.
   * If there are form errors (invalid username, missing fields, etc.), the
   * errors are presented and no actual login attempt is made.
   */
  private void attemptLogin() {
    // Reset errors.
    mUsernameView.setError(null);

    // Store values at the time of the login attempt.
    String username = mUsernameView.getText().toString().trim();

    // Check for a valid username.
    if (TextUtils.isEmpty(username)) {
      // There was an error; don't attempt login and focus the first
      // form field with an error.
      mUsernameView.setError(getString(R.string.error_field_required));
      mUsernameView.requestFocus();
      return;
    }

    mUsername = username;

    // perform the user login attempt.
    mSocket.emit("add user", username);
  }

  private Emitter.Listener onLogin = new Emitter.Listener() {
    @Override public void call(Object... args) {
      JSONObject data = (JSONObject) args[0];

      int numUsers;
      boolean isKing;
      try {
        numUsers = data.getInt("numUsers");
        isKing = data.getBoolean("isKing");
      } catch (JSONException e) {
        return;
      }

      Log.d(TAG, "isKing: " + isKing);

      Intent intent = new Intent();
      intent.putExtra("username", mUsername);
      intent.putExtra("numUsers", numUsers);
      intent.putExtra("isKing", isKing);
      setResult(RESULT_OK, intent);
      finish();
    }
  };

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.login, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.config:
        final EditText editText = new EditText(this);

        new AlertDialog.Builder(this).setTitle("请输入")
            .setView(editText)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
              @Override public void onClick(DialogInterface dialog, int which) {
                String ip = editText.getText().toString();
                Log.d(TAG, "IP=" + ip);

                //  保存到sp中
                SharedPreferences sp = getSharedPreferences(Constants.SP_CONFIG_NAME,
                    Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(Constants.CFG_IP,ip);
                editor.commit();

              }
            })
            .setNegativeButton("取消", null)
            .show();
        break;
      default:
        break;
    }

    return super.onOptionsItemSelected(item);
  }
}



