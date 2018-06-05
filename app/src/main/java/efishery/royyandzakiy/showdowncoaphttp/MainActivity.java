package efishery.royyandzakiy.showdowncoaphttp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class MainActivity extends AppCompatActivity {

    Button send;
    TextView mRequestType, packetLossValue, responseTimeValue, cpuProcessingValue, payloadSizeValue, responseSuccessValue, responseFailValue, status;
    boolean clicked = false;
    int countSuccess = 0, countFail = 0, countRequest = 150;
    String type = "http";

    Stopwatch stopwatch;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_dashboard:
                    mRequestType.setText("HTTP Request");
                    type = "http";
                    resetAll();
                    return true;
                case R.id.navigation_notifications:
                    mRequestType.setText("CoAP Request");
                    type = "coap";
                    resetAll();
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initVariables();

        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!clicked) {
                    clicked = !clicked;
                    Toast.makeText(getApplicationContext(), "requesting...", Toast.LENGTH_SHORT).show();
                    status.setText("requesting...");
                    stopwatch.start();

                    // send HTTP Request
                    for (int i=0; i<countRequest; i++) {
                        if (type == "http") {
                            sendHttpRequest();
                        } else if (type == "coap") {
                            sendCoapRequest();
                            if (i == countRequest-1) requestDone();
                        }
                    }

                    // calculate: packet loss, response time, cpu processing, payload size
                    // change values
                    packetLossValue.setText("clicked");
                    responseTimeValue.setText("clicked");
                    cpuProcessingValue.setText("TBD");
                    payloadSizeValue.setText("TBD");
                }
            }
        });
    }

    protected void sendCoapRequest() {
        // do something
        // ...
    }

    protected void sendHttpRequest() {
        // Instantiate the RequestQueue.
        final RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://sakernas-api.herokuapp.com/pemutakhiran/5a8239f92cece14688a14f2c";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display response
                        // responseSuccessValue.setText(response);
                        responseSuccessValue.setText("Success count: "+ ++countSuccess);
                        queue.stop();

                        if ((countFail + countSuccess) == countRequest) {
                            requestDone();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // responseSuccessValue.setText("That didn't work!");
                responseFailValue.setText("Fail count: "+ ++countFail);
                queue.stop();

                if ((countFail + countSuccess) == countRequest) {
                    requestDone();
                }
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    protected void requestDone() {
        String elapsedTime = String.valueOf(stopwatch.getElapsedTimeSecs());
        String toastText = (countFail + countSuccess) + " response done! " + elapsedTime  + " secs";
        Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
        status.setText("Response done!");

        packetLossValue.setText(String.valueOf(countFail) + " packets");
        responseTimeValue.setText(String.valueOf(elapsedTime) + " secs");
        // cpuProcessingValue.setText();
        // payloadSizeValue.setText();

        // RESET ALL
        resetVars();
    }

    protected void resetVars() {
        stopwatch.stop();
        clicked = false;
        countSuccess = 0; countFail = 0;
    }

    protected void resetAll() {
        resetVars();
        packetLossValue.setText("null");
        responseTimeValue.setText("null");
        cpuProcessingValue.setText("null");
        payloadSizeValue.setText("null");
        status.setText("Press send!");

        responseFailValue.setText("null");
        responseSuccessValue.setText("null");
    }

    protected void initVariables() {
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mRequestType = (TextView) findViewById(R.id.requestType);
        send = (Button) findViewById(R.id.send);
        packetLossValue = (TextView) findViewById(R.id.packetLossValue);
        responseTimeValue = (TextView) findViewById(R.id.responseTimeValue);
        cpuProcessingValue = (TextView) findViewById(R.id.cpuProcessingValue);
        payloadSizeValue = (TextView) findViewById(R.id.payloadSizeValue);
        responseSuccessValue = (TextView) findViewById(R.id.responseSuccessValue);
        responseFailValue = (TextView) findViewById(R.id.responseFailValue);
        status = (TextView) findViewById(R.id.status);

        stopwatch = new Stopwatch();
    }

}
