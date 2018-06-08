package efishery.royyandzakiy.showdowncoaphttp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

import java.net.URI;
import java.util.ArrayList;

import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.options.UintOptionValue;

import static java.sql.Types.NULL;

public class MainActivity extends AppCompatActivity {
        // COMPONENTS
    public Button send;
    public TextView mRequestType,
            totalRequestValue, packetLossValue, totalRequestTimeValue, requestTimeValue,
            responseSuccessValue, responseFailValue, status;

    // SYSTEM
    private long startTime;
    private ArrayList<ReqResData> listReqResData = new ArrayList<ReqResData>();
    private RequestQueue queue;
    private CoapClient clientApplication;
    private String type = "http";
    private boolean isProcessing = false;
    private int countSuccess = 0, countFail = 0, countRequest = 130;
    private float durasiAvg;
    private long durasiMax = -1, durasiMin = 9999999, durasiTotal = 0, durasiTemp;

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

        startTime = System.currentTimeMillis();

        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (!isProcessing) {
                    // send Request
                    isProcessing = true;

                    sendRequest();

                    Toast.makeText(getApplicationContext(), "requesting...", Toast.LENGTH_SHORT).show();
                    status.setText("requesting...");

                    // calculate: packet loss, response time
                    // change values
                    totalRequestValue.setText("isProcessing");
                    packetLossValue.setText("isProcessing");
                    totalRequestTimeValue.setText("isProcessing");
                    requestTimeValue.setText("isProcessing");

                    responseSuccessValue.setText("isProcessing");
                    responseFailValue.setText("isProcessing");
                }
            }
        });
    }

    protected void sendRequest() {
        // loop send All Asynchronously
        for (int idRequest=0; idRequest<countRequest; idRequest++) {
            if (type == "http") {
                sendHttpRequest(idRequest);
            } else if (type == "coap") {
                sendCoapRequest(idRequest);
            }
        }
    }

    protected void sendCoapRequest(int idCoapRequest) {
        // do something
        new SendCoapRequest(this, idCoapRequest).execute();
    }

    protected void sendHttpRequest(int idRequest) {
        // Instantiate the RequestQueue.
        final String url ="https://sakernas-api.herokuapp.com/pemutakhiran/5a8239f92cece14688a14f2c";

        final ReqResData tempReqResData = new ReqResData();
        tempReqResData.idRequest = idRequest;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display response
                        responseSuccessValue.setText("Success count: "+ ++countSuccess);

                        tempReqResData.duration = System.currentTimeMillis() - startTime;
                        tempReqResData.success = true;
                        listReqResData.add(tempReqResData);

                        if ((countFail + countSuccess) == countRequest) {
                            requestDone();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                responseFailValue.setText("Fail count: "+ ++countFail);

                tempReqResData.duration = System.currentTimeMillis() - startTime;
                tempReqResData.success = false;
                listReqResData.add(tempReqResData);

                if ((countFail + countSuccess) == countRequest) {
                    requestDone();
                }
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    protected void requestDone() {
        Log.d("DEBUG::","MainActivity::requestDone");
        Log.d("DEBUG::","MainActivity::VARIABLES.listReqResData.size() = " + listReqResData.size());
        //=== Get Durasi
        for (int i=0; i<listReqResData.size(); i++) {
            if (type.equals("http")) durasiTemp = listReqResData.get(i).duration; // Milli
            else if (type.equals("coap")) durasiTemp = listReqResData.get(i).duration;
            if (durasiMax < durasiTemp) durasiMax = durasiTemp;
            if (durasiMin > durasiTemp) durasiMin = durasiTemp;
            durasiTotal += durasiTemp;
        }
        durasiAvg = (float) durasiTotal / listReqResData.size();

        Log.d("DEBUG::","MainActivity::requestDone::VARIABLES.durasiTotal:"+ String.valueOf(durasiTotal));
        Log.d("DEBUG::","MainActivity::requestDone::VARIABLES.durasiMax:"+ String.valueOf(durasiMax));
        Log.d("DEBUG::","MainActivity::requestDone::VARIABLES.durasiMin:"+ String.valueOf(durasiMin));
        Log.d("DEBUG::","MainActivity::requestDone::VARIABLES.listReqResData:"+ String.valueOf(listReqResData.size()));
        Log.d("DEBUG::","MainActivity::requestDone::VARIABLES.durasiAvg:"+ String.valueOf(durasiAvg));

        //=== Change messages
        String elapsedTime = String.valueOf(System.currentTimeMillis() - startTime);

        totalRequestValue.setText(String.valueOf(countFail + countSuccess) + " / " + countRequest + " packets");
        packetLossValue.setText(String.valueOf(countFail) + " packets");
        totalRequestTimeValue.setText(String.valueOf(elapsedTime) + " ms");
        requestTimeValue.setText(String.valueOf(durasiAvg) + " / " + String.valueOf(durasiMax) + " / " + String.valueOf(durasiMin) + " ms"); // hitung AVG/MAX/MIN

        long duration = System.currentTimeMillis() - startTime;
        if ((countFail + countSuccess) == countRequest || duration > 60000) {
            String toastText = (countFail + countSuccess) + " response done! " + elapsedTime  + " ms";
            Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
            status.setText("Response done!");
            resetVars();
        }
    }

    protected void resetVars() {
        queue.stop();
        isProcessing = false;
        countSuccess = 0; countFail = 0;
        durasiMin = 9999999; durasiMax = -1; durasiAvg = NULL; durasiTemp = NULL; durasiTotal = 0;
        listReqResData.clear();
    }

    protected void resetAll() {
        resetVars();

        totalRequestValue.setText("null");
        packetLossValue.setText("null");
        totalRequestTimeValue.setText("null");
        requestTimeValue.setText("null");

        status.setText("Press send!");

        responseFailValue.setText("null");
        responseSuccessValue.setText("null");
    }

    protected void initVariables() {
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mRequestType = (TextView) findViewById(R.id.requestType);
        send = (Button) findViewById(R.id.send);

        totalRequestValue = (TextView) findViewById(R.id.totalRequestValue);
        totalRequestTimeValue = (TextView) findViewById(R.id.totalRequestTimeValue);
        packetLossValue = (TextView) findViewById(R.id.packetLossValue);
        requestTimeValue = (TextView) findViewById(R.id.requestTimeValue);

        responseSuccessValue = (TextView) findViewById(R.id.responseSuccessValue);
        responseFailValue = (TextView) findViewById(R.id.responseFailValue);
        status = (TextView) findViewById(R.id.status);

        this.clientApplication = new CoapClient();
        queue = Volley.newRequestQueue(this);
    }

    public CoapClient getClientApplication(){
        return this.clientApplication;
    }

    public void processResponse(final CoapResponse coapResponse, final URI serviceURI, final long duration) {
        ReqResData temp = new ReqResData();
        temp.duration = duration;
        listReqResData.add(temp);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                long block2Num = coapResponse.getBlock2Number(); // block2Num itu utk apa?
                //String text = "Response received";
                //if (block2Num != UintOptionValue.UNDEFINED) {
                //    text += " (" + block2Num + " blocks in " + duration + " ms)";
                //} else {
                //    text += " (after " + duration + " ms)";
                //}

                String text = duration + " ms)";
                totalRequestTimeValue.setText(text);

                //Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();

                responseReceived(serviceURI, coapResponse);
            }
        });
    }

    public void responseReceived(URI uri, CoapResponse coapResponse){
        Log.d("DEBUG::","MainActivity::responseReceived::countTotal="+(countSuccess+countFail)+";countSuccess="+countSuccess+";countFail="+countFail);
        if ((countSuccess + countFail) >= countRequest/2) {
            requestDone();
        }
    }

    public int getCountSuccess() {
        return countSuccess;
    }

    public int getCountFail() {
        return countFail;
    }

    public void setCountSuccess(int _countSuccess) {
        countSuccess = _countSuccess;
    }

    public void setCountFail(int _countFail) {
        countFail = _countFail;
    }
}
