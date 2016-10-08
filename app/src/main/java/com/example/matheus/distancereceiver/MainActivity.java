package com.example.matheus.distancereceiver;

import android.content.Context;
import android.content.Intent;
import android.net.http.RequestQueue;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;


public class MainActivity extends AppCompatActivity implements RecognitionListener{

    private static final String KWS_SEARCH = "wakeup";

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "refresh";

    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;

    private boolean isFirstListen = true;
    private boolean isListening = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Refreshing...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                refreshMessages(view);
            }
        });


        // Prepare the data for UI
        captions = new HashMap<String, Integer>();
        captions.put(KWS_SEARCH, R.string.kws_caption);

        FloatingActionButton fabStart = (FloatingActionButton) findViewById(R.id.fabStart);
        fabStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (isListening) {
                        Toast.makeText(getApplicationContext(), "I'm already listening.", Toast.LENGTH_SHORT).show();
                    } else {
                        if (isFirstListen) {
                            initializeRecognizer();
                            isFirstListen = false;
                        } else switchSearch(KWS_SEARCH);
                        isListening = true;
                    }
                }

                catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Sumthin is wrong: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        FloatingActionButton fabStop = (FloatingActionButton) findViewById(R.id.fabStop);
        fabStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isListening) {
                    recognizer.stop();
                    isListening = false;
                }
                else {
                    Toast.makeText(getApplicationContext(), "I've already stopped listening.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        setInputTexts();
        Log.d("MATH HERE", "onResume called");
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int result = apiAvailability.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(result)) {
                apiAvailability.getErrorDialog(this, result,
                        12).show();
            }

        }
        Log.d("MATH HERE", "Finished checking for Google Play Services availability");

        Log.d("MATH HERE", "Refreshing messages.");
        refreshMessages();
    }

    public void setInputTexts(){
        EditText ipField = (EditText) findViewById(R.id.EditTextIp);
        EditText authField = (EditText) findViewById(R.id.EditTextAuth);

        ipField.setText(Constants.serverIp);
        authField.setText(Constants.auth);
    }

    public void saveCredentials(View button) {
        final EditText ipField = (EditText) findViewById(R.id.EditTextIp);
        final EditText authField = (EditText) findViewById(R.id.EditTextAuth);

        Constants.auth = authField.getText().toString();
        Constants.serverIp = ipField.getText().toString();;
    }


    public void refreshMessages(View v) {

        if (Constants.token.equals("")) {
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
            Snackbar.make(v, Constants.lastError, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
        else {
            //send request to Ardu
            sendTokenToArdu(Constants.token, v);

        }

        String distance = Constants.distance;
        setValuesToScreen(distance);

    }

    public void refreshMessages(){
        if (Constants.token.equals("")) {
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
        else {
            //send request to Ardu
            sendTokenToArdu(Constants.token);

        }

        String distance = Constants.distance;
        setValuesToScreen(distance);
    }

    public void setValuesToScreen(String distance){
        TextView blaze = (TextView) findViewById(R.id.textDistance);
        if (distance != null) {
            RelativeLayout container = (RelativeLayout) findViewById(R.id.container);
            blaze.setText(distance);
            Log.d("MATH HERE", "Set textDistance's text to '" + distance + "'");
            if (Integer.parseInt(distance) < 80) {
                //set the color to light green
                container.setBackgroundColor(0xFF50AA50);
            } else container.setBackgroundColor(0xFFAA5050);
            Log.d("MATH HERE", "Changed the main container's background color.");
        }
        else Log.d("MATH HERE", "The distance is null.");
    }

    public void sendTokenToArdu(String token, final View v){
        com.android.volley.RequestQueue queue = Volley.newRequestQueue(this);
        String url = Constants.serverIp.equals("") ? "192.168.0.3:3000" : Constants.serverIp;
        url = url + ":3000/check";

// Request a string response from the provided URL.
        JsonObjectRequest stringRequest = null;
        try {
            stringRequest = new JsonObjectRequest(Request.Method.POST, "http://"+url, new JSONObject("{\"token\":\""+token+"\"}"),
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    try {
                                        Constants.distance = ""+response.getInt("distance");
                                        setValuesToScreen(Constants.distance);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    Snackbar.make(v, response.toString(), Snackbar.LENGTH_LONG)
                                            .setAction("Action", null).show();
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                            generateNoteOnSD( "LogRequest.txt", "error on req "+ error.getMessage());
                            if (error.networkResponse.statusCode == 401) {
                                Snackbar.make(v, "Invalid Authorization; got 401", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                            else Snackbar.make(v, error.getMessage(), Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();

                        }
                    }){
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String,String> params = new HashMap<>();
                    params.put("Authorization",Constants.auth);
                    //..add other headers
                    return params;
                }
            };
        } catch (JSONException e) {
            e.printStackTrace();
            generateNoteOnSD( "LogRequest.txt", "error on req "+ e.getMessage());

        }
// Add the request to the RequestQueue.

        queue.add(stringRequest);
    }

    public void sendTokenToArdu(String token){
        com.android.volley.RequestQueue queue = Volley.newRequestQueue(this);
        String url =Constants.serverIp;

// Request a string response from the provided URL.
        JsonObjectRequest stringRequest = null;
        try {
            stringRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject("{\"token\":\""+token+"\"}"),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {


                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    generateNoteOnSD( "LogRequest.txt", "error on req "+ error.getMessage());

                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            generateNoteOnSD( "LogRequest.txt", "error on req "+ e.getMessage());

        }
// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        recognizer.cancel();
        recognizer.shutdown();
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            refreshMessages();
            switchSearch(KWS_SEARCH);
        }
        else
            Toast.makeText(getApplicationContext(), "I don't recognize '"+ text+"'", Toast.LENGTH_SHORT).show();
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
        }

    }

    @Override
    public void onBeginningOfSpeech() {
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);

        String caption = getResources().getString(captions.get(searchName));
        Toast.makeText(getApplicationContext(), caption, Toast.LENGTH_SHORT).show();
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        try {

            recognizer = defaultSetup()
                    .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                    .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

                            // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                            // .setRawLogDir(assetsDir)

                            // Threshold to tune for keyphrase to balance between false alarms and misses
                    .setKeywordThreshold(1e-45f)

                            // Use context-independent phonetic search, context-dependent is too slow for mobile
                    .setBoolean("-allphone_ci", true)

                    .getRecognizer();
            recognizer.addListener(this);


            // Create keyword-activation search.
            recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        }

        catch (Exception e) {
            generateNoteOnSD("Log.txt", e.getMessage());
        }

        /*
        // Create grammar-based search for selection between demos
        File menuGrammar = new File(assetsDir, "menu.gram");
        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);

        // Create grammar-based search for digit recognition
        File digitsGrammar = new File(assetsDir, "digits.gram");
        recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);

        // Create language model search
        File languageModel = new File(assetsDir, "weather.dmp");
        recognizer.addNgramSearch(FORECAST_SEARCH, languageModel);

        // Phonetic search
        File phoneticModel = new File(assetsDir, "en-phone.dmp");
        recognizer.addAllphoneSearch(PHONE_SEARCH, phoneticModel);
 */
    }

    @Override
    public void onError(Exception error) {
        generateNoteOnSD("Log.txt", error.getMessage());

    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }


    public void initializeRecognizer() {




        // Recognizer initialization is a time-consuming task and it involves IO,
        // so we execute it in an async task


        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {

                    generateNoteOnSD( "Log.txt", "Creating assets... ");

                    Assets assets = new Assets(MainActivity.this);

                    generateNoteOnSD("Log.txt", "Syncing assets... ");
                    File assetDir = assets.syncAssets();

                    generateNoteOnSD( "Log.txt", "Setting up the recognizer... ");
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    generateNoteOnSD( "Log.txt",  e.getMessage());
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    Toast.makeText(getApplicationContext(), "Failed to initialize the recognizer.", Toast.LENGTH_LONG).show();
                } else {
                    switchSearch(KWS_SEARCH);
                }
            }
        }.execute();
    }

    public void generateNoteOnSD(String sFileName, String sBody) {
        String baseFolder;
        try {
// check if external storage is available
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            baseFolder = getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
        }
// revert to using internal storage
        else {
            baseFolder = getApplicationContext().getFilesDir().getAbsolutePath();
        }


        File file = new File(baseFolder + sFileName);

            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write(sBody.getBytes());
            fos.close();

        }

        catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show(); }
    }

}
