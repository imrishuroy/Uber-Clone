/*
 * Copyright (c) 2015-present, Parse, LLC.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.parse.starter;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

import com.parse.LogInCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class MainActivity extends AppCompatActivity {
  public void redirectActivity(){
    if(ParseUser.getCurrentUser().get("riderOrDriver").equals("rider") ){

      Intent intent = new Intent(getApplicationContext(),RiderActivity.class);
      startActivity(intent);

    }else {
      Intent intent = new Intent(getApplicationContext(),ViewRequestsActivity.class);
      startActivity(intent);

    }
  }

  public void getStarted(View view){

    Switch userTypeSwitch = findViewById(R.id.userTypeSwitch);
    Log.i("Switch Value",String.valueOf(userTypeSwitch.isChecked()));

    String userType = "rider";

    if(userTypeSwitch.isChecked()){
      userType = "driver";
    }
    ParseUser.getCurrentUser().put("riderOrDriver",userType);

    ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
      @Override
      public void done(ParseException e) {
        redirectActivity();
      }
    });

   // Log.i("Info","Redirecting as " + userType);


  }


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    getSupportActionBar().hide(); // to hide action bar

    if(ParseUser.getCurrentUser() == null){                 // login Process Start
      ParseAnonymousUtils.logIn(new LogInCallback() {
        @Override
        public void done(ParseUser user, ParseException e) {
          if(e == null){
            Log.i("Info", "Anonymous Login Succussful");
          }else {
            Log.i("Info", "Anonymous Login Failed");
          }
        }
      });
    }else {
      if(ParseUser.getCurrentUser().get("riderOrDriver") != null){
        Log.i("Info","Redirecting as "+ ParseUser.getCurrentUser().get("riderOrDriver"));
        redirectActivity();
      }
    }
    
    ParseAnalytics.trackAppOpenedInBackground(getIntent());
  }

}