package efishery.royyandzakiy.showdowncoaphttp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.message.CoapResponse;

import static java.sql.Types.NULL;

public class MainActivity extends AppCompatActivity {
    // COMPONENTS
    private Button send;
    private TextView mRequestType,
            totalRequestValue, packetLossValue, totalRequestTimeValue, requestTimeValue,
            responseSuccessValue, responseFailValue, status;
    private EditText nRequestsValue;

    // SYSTEM
    private long startTime;
    private ArrayList<ReqResData> listReqResData = new ArrayList<ReqResData>();
    private RequestQueue queue;
    private CoapClient clientApplication;
    private String type = "http";
    private boolean isProcessing = false;
    private int countSuccess = 0, countFail = 0, countRequest = 130, countRequestMinimumSuccess;
    private double countRequestErrorMargin = 0.02;
    private float durasiAvg;
    private long durasiMax = -1, durasiMin = 9999999, durasiTotal = 0, durasiTemp;
    final String url ="http://ec2-54-169-136-164.ap-southeast-1.compute.amazonaws.com:5675";

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

                resetVars();

                if (!nRequestsValue.getText().toString().matches("")) {
                    if (!isProcessing) {
                        // send Request
                        isProcessing = true;

                        countRequest = Integer.valueOf(nRequestsValue.getText().toString());
                        //countRequestMinimumSuccess = countRequest- (int) Math.floor(countRequest*countRequestErrorMargin);
                        countRequestMinimumSuccess = countRequest;

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
            }
        });
    }

    protected void sendRequest() {
        // loop send All Asynchronously
        startTime = System.currentTimeMillis();
        for (int idRequest=0; idRequest<countRequest; idRequest++) {
            if (type == "http") {
                sendHttpRequest(idRequest);
            } else if (type == "coap") {
                sendCoapRequest(idRequest);
            }
        }
    }

    protected void sendHttpRequest(int idRequest) {
        // Instantiate the RequestQueue.

        final ReqResData tempReqResData = new ReqResData();
        tempReqResData.messageID = idRequest;
        JSONObject jsonBody=new JSONObject();
        try {
            jsonBody.put("id", idRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("DEBUG::","MainActivity::sendHttpRequest::VARIABLES.jsonBody=" + jsonBody.toString());

        // Request a string response from the provided URL.
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("DEBUG::","MainActivity::sendHttpRequest::VARIABLES.response=" + response);
                        // Display response
                        responseSuccessValue.setText("Success count: "+ ++countSuccess);

                        tempReqResData.duration = System.currentTimeMillis() - startTime;
                        tempReqResData.success = true;
                        listReqResData.add(tempReqResData);

                        if ((countFail + countSuccess) >= countRequestMinimumSuccess) {
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

                if ((countFail + countSuccess) >= countRequestMinimumSuccess) {
                    requestDone();
                }
            }
        }) {

            /** Passing some request headers* */
            @Override
            public Map<String,String> getHeaders() throws AuthFailureError {
                HashMap<String,String> headers = new HashMap();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        // Add the request to the RequestQueue.
        jsonObjectRequest.setTag("sendHttpRequest");
        queue.add(jsonObjectRequest);
    }


    protected void sendCoapRequest(int idCoapRequest) {
        // Discover
        // GET
        // POST
        // PUT
        // DELETE
        // Ping
        long method = 2; // POST
        new SendCoapRequest(this, idCoapRequest).execute(method);
    }

    protected void requestDone() {
        // FUNGSI:
        // 1. melakukan update terhadap UI
        // 2. melakukan reset terhadap variabel (jika semua request telah terhitung baik Success atau Fail)
        Log.d("DEBUG::","MainActivity::requestDone");
        Log.d("DEBUG::","MainActivity::VARIABLES.listReqResData.size() = " + listReqResData.size());
        //=== Get Durasi
        for (int i=0; i<listReqResData.size(); i++) {
            durasiTemp = listReqResData.get(i).duration; // Milli

            if (durasiMax < durasiTemp) {
                durasiMax = durasiTemp;
            }
            if (durasiMin > durasiTemp) {
                durasiMin = durasiTemp;
            }
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
        if ((countFail + countSuccess) >= (countRequestMinimumSuccess) || duration > 60000) { // duration tidak pernah tertrigger
            String toastText = (countFail + countSuccess) + " response done! " + elapsedTime  + " ms";
            Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
            status.setText("Response done!");
            resetVars();
        }
    }

    protected void resetVars() {
        Log.d("DEBUG::","MainActivity::resetVars()");
        queue.cancelAll("sendHttpRequest");
        isProcessing = false;
        countSuccess = 0; countFail = 0;
        durasiMin = 9999999; durasiMax = -1; durasiAvg = NULL; durasiTemp = NULL; durasiTotal = 0;
        listReqResData.clear();
        queue.stop();
        queue.start();
        Log.d("DEBUG::","MainActivity::resetVars():VARIABLES.durasiTotal=" + durasiTotal);
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
        nRequestsValue = (EditText) findViewById(R.id.nRequestValue);
        nRequestsValue.setText(String.valueOf(countRequest));

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
        final ReqResData temp = new ReqResData();
        temp.messageID = coapResponse.getMessageID();
        temp.duration = duration;
        //temp.success = false;
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

                String text = duration + " ms";
                // totalRequestTimeValue.setText(text); // digunakan sementara, karena totalRequestTimeValue baru terisi setelah terpanggil requestDone()

                //Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();

                Log.d("DEBUG::","MainAcitivty::processResponse::VARIABLES.getContent="+coapResponse.getContent().toString(CoapResponse.CHARSET));
                Log.d("DEBUG::","MainAcitivty::processResponse{messageID("+coapResponse.getMessageID()+")}::VARIABLES.getMessageTypeName="+coapResponse.getMessageTypeName());
                //temp.success = true;
                //listReqResData.set(listReqResData.indexOf(temp), temp); // set bahwa pesan sukses

                responseReceived(serviceURI, coapResponse);
            }
        });
    }

    public void processResponseFailed(final int idCoapRequest, final long duration) {
        ReqResData temp = new ReqResData();
        temp.duration = duration;
        listReqResData.add(temp);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String text = duration + " ms";

                Log.d("DEBUG::","MainAcitivty::processResponseFailed");
                Log.d("DEBUG::","MainAcitivty::processResponse{messageID("+idCoapRequest+")}");
                responseReceivedFailed();
            }
        });
    }

    public void responseReceivedFailed() {
        if ((countSuccess + countFail) >= countRequestMinimumSuccess) {
            requestDone();
        }
    }

    public void responseReceived(URI uri, CoapResponse coapResponse){
        Log.d("DEBUG::","MainActivity::responseReceived::countTotal="+(countSuccess+countFail)+";countSuccess="+countSuccess+";countFail="+countFail+";countRequestMinimumSuccess=" + countRequestMinimumSuccess);
        if ((countSuccess + countFail) >= countRequestMinimumSuccess) {
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
