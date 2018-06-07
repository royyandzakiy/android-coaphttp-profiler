package efishery.royyandzakiy.showdowncoaphttp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.options.OpaqueOptionValue;
import de.uzl.itm.ncoap.message.options.UintOptionValue;

import static java.sql.Types.NULL;

public class MainActivity extends AppCompatActivity {

    public class ReqResData {
        public int id;
        public float timesend;
        public float timereply;
        public float duration;
        public boolean success;
    }

    long starttime;

    ArrayList<ReqResData> listReqResData = new ArrayList<ReqResData>();
    private RequestQueue queue;

    Button send;
    TextView mRequestType, 
            totalRequestValue, packetLossValue, totalRequestTimeValue, requestTimeValue, cpuProcessingValue, contentLengthValue, totalContentLengthValue,
            responseSuccessValue, responseFailValue, responseMessage, status;
    boolean isProcessing = false;
    int countSuccess = 0, countFail = 0, countRequest = 151;
    String type = "http";
    float durasiAvg, durasiMax = -1, durasiMin = 9999, durasiTotal = 0, durasiTemp;

    private CoapClient clientApplication;
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

        starttime = System.currentTimeMillis();

        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (!isProcessing) {
                    // send Request
                    isProcessing = true;

                    sendRequest();

                    Toast.makeText(getApplicationContext(), "requesting...", Toast.LENGTH_SHORT).show();
                    status.setText("requesting...");
                    stopwatch.start();

                    // calculate: packet loss, response time, cpu processing, payload size
                    // change values
                    totalRequestValue.setText("isProcessing");
                    packetLossValue.setText("isProcessing");
                    totalRequestTimeValue.setText("isProcessing");
                    requestTimeValue.setText("isProcessing");
                    cpuProcessingValue.setText("TBD");
                    contentLengthValue.setText("TBD");

                    responseSuccessValue.setText("isProcessing");
                    responseFailValue.setText("isProcessing");
                    responseMessage.setText("isProcessing");
                }
            }
        });
    }

    protected void sendRequest() {
        // loop send All Asynchronously
        for (int i=0; i<countRequest; i++) {
            if (type == "http") {
                sendHttpRequest(i);
            } else if (type == "coap") {
                sendCoapRequest(i);
                //Log.d("DEBUG::sendRequest::","request ke-" + i + " terkirim!");
                //if (i == countRequest-1) requestDone(); // bruteforce requestDone()
            }
        }
    }

    protected void sendCoapRequest(int n) {
        // do something
        new SendCoapRequest(this).execute();
    }

    protected void sendHttpRequest(int n) {
        // Instantiate the RequestQueue.
        String url ="https://sakernas-api.herokuapp.com/pemutakhiran/5a8239f92cece14688a14f2c";

        final ReqResData temp = new ReqResData();
        temp.id = n;

        //Log.d("DEBUG::GAK_PROCESSING::","sendrequest_sendHTTPRequest - " + n);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display response
                        // responseSuccessValue.setText(response);
                        responseSuccessValue.setText("Success count: "+ ++countSuccess);
                        temp.timereply = stopwatch.getElapsedTimeSecs();
                        temp.success = true;
                        listReqResData.add(temp);

                        if ((countFail + countSuccess) == countRequest) {
                            requestDone();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // responseSuccessValue.setText("That didn't work!");
                responseFailValue.setText("Fail count: "+ ++countFail);
                //temp.timereply = stopwatch.getElapsedTimeSecs();
                temp.timereply = System.currentTimeMillis();
                temp.success = false;
                listReqResData.add(temp);

                if ((countFail + countSuccess) == countRequest) {
                    requestDone();
                }
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
        //temp.timesend = stopwatch.getElapsedTimeSecs();
        temp.timesend = System.currentTimeMillis();
    }

    protected void requestDone() {
        Log.d("DEBUG::","MainActivity::requestDone");
        Log.d("DEBUG::","MainActivity::listReqResData.size() = " + listReqResData.size());
        //=== Get Durasi
        for (int i=0; i<listReqResData.size(); i++) {
            durasiTemp = listReqResData.get(i).timereply - listReqResData.get(i).timesend; // Milli
            if (durasiMax < durasiTemp) durasiMax = durasiTemp;
            if (durasiMin > durasiTemp) durasiMin = durasiTemp;
            durasiTotal += durasiTemp;
        }
        durasiAvg = (float) durasiTotal / listReqResData.size();

        Log.d("durasiTotal:", String.valueOf(durasiTotal));
        Log.d("listReqResData:", String.valueOf(listReqResData.size()));
        Log.d("durasiAvg:", String.valueOf(durasiAvg));

        //=== Change messages
        String elapsedTime = String.valueOf(stopwatch.getElapsedTimeSecs());
        String toastText = (countFail + countSuccess) + " response done! " + elapsedTime  + " secs";
        Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
        status.setText("Response done!");
        totalRequestValue.setText(String.valueOf(countFail + countSuccess) + " packets");
        packetLossValue.setText(String.valueOf(countFail) + " packets");
        totalRequestTimeValue.setText(String.valueOf(elapsedTime) + " secs");
        requestTimeValue.setText(durasiAvg + " / " + (float) durasiMax + " / " + (float) durasiMin + " secs"); // hitung AVG/MAX/MIN
        // cpuProcessingValue.setText(); // hitung AVG/MAX/MIN
        // contentLengthValue.setText(); // apakah bisa didapat? kalau tidak gunakan wireshark
        // totalContentLengthValue.setText(); // apakah bisa didapat? kalau tidak gunakan wireshark

        //=== DEBUG
        // Log.d("DEBUG::", "size of list: " + String.valueOf(listReqResData.size()));
        // Log.d("DEBUG::", "list ke-90, timereply: " + listReqResData.get(90).timereply);

        // RESET ALL
        resetVars();
    }

    protected void resetVars() {
        stopwatch.stop();
        queue.stop();
        isProcessing = false;
        countSuccess = 0; countFail = 0;
        durasiMin = NULL; durasiMax = NULL; durasiAvg = NULL; durasiTemp = NULL; durasiTotal = 0;
        listReqResData.clear();
    }

    protected void resetAll() {
        resetVars();

        totalRequestValue.setText("null");
        packetLossValue.setText("null");
        totalRequestTimeValue.setText("null");
        requestTimeValue.setText("null");
        cpuProcessingValue.setText("null");
        contentLengthValue.setText("null");
        totalContentLengthValue.setText("null");

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
        cpuProcessingValue = (TextView) findViewById(R.id.cpuProcessingValue);
        contentLengthValue = (TextView) findViewById(R.id.contentLengthValue);
        totalContentLengthValue = (TextView) findViewById(R.id.totalContentLengthValue);

        responseSuccessValue = (TextView) findViewById(R.id.responseSuccessValue);
        responseFailValue = (TextView) findViewById(R.id.responseFailValue);
        responseMessage = (TextView) findViewById(R.id.responseMessage);
        status = (TextView) findViewById(R.id.status);

        this.clientApplication = new CoapClient();
        stopwatch = new Stopwatch();
        queue = Volley.newRequestQueue(this);
    }

    public CoapClient getClientApplication(){
        return this.clientApplication;
    }

    public void processResponse(final CoapResponse coapResponse, final URI serviceURI, final long duration) {
        ReqResData temp = new ReqResData();
        temp.duration = duration;
        listReqResData.add(temp);

        // MUNGKIN FAIL DISINI
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                long block2Num = coapResponse.getBlock2Number();
                String text = "Response received";
                if (block2Num != UintOptionValue.UNDEFINED) {
                    text += " (" + block2Num + " blocks in " + duration + " ms)";
                } else {
                    text += " (after " + duration + " ms)";
                }

                totalRequestTimeValue.setText(text);

                //Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();

                responseReceived(serviceURI, coapResponse);
                //Log.d("DEBUG::","response ke-" + countFail + ", running");
            }
        });
    }

    public void responseReceived(URI uri, CoapResponse coapResponse){
        ++countFail;
        Log.d("DEBUG::","MainActivity::responseReceived::countFail = " + countFail + "; stopwatch = " + stopwatch.getElapsedTimeSecs());
        if (countFail == countRequest) {
            requestDone();
        }
        //Log.d("DEBUG::","MainActivity::responseReceived countSuccess = " + countSuccess);
        /*
        TextView txtResponse = (TextView) getActivity().findViewById(R.id.txt_response_payload);
        txtResponse.setText("");
        txtResponse.setText(coapResponse.getContent().toString(CoapMessage.CHARSET));

        //Response Type
        TextView txtResponseType = (TextView) getActivity().findViewById(R.id.txt_type_response);
        txtResponseType.setText(coapResponse.getMessageTypeName());

        //ETAG Option
        TableRow etagRow = (TableRow) clientActivity.findViewById(R.id.tabrow_etag_response);
        byte[] etagValue = coapResponse.getEtag();
        if(etagValue != null) {
            etagRow.setVisibility(View.VISIBLE);
            ((TextView) clientActivity.findViewById(R.id.txt_etag_response)).setText(
                    OpaqueOptionValue.toHexString(etagValue)
            );
        } else {
            etagRow.setVisibility(View.GONE);
        }

        //Observe Option
        TableRow observeRow = (TableRow) clientActivity.findViewById(R.id.tabrow_observe_response);
        long observeValue = coapResponse.getObserve();
        if(observeValue != UintOptionValue.UNDEFINED){
            observeRow.setVisibility(View.VISIBLE);
            ((TextView) clientActivity.findViewById(R.id.txt_observe_response)).setText("" + observeValue);
        } else {
            observeRow.setVisibility(View.GONE);
        }

        //Location-URI Options
        try {
            URI locationURI = coapResponse.getLocationURI();
            TableRow locationPathRow = (TableRow) clientActivity.findViewById(R.id.tabrow_location_path_response);
            TableRow locationQueryRow = (TableRow) clientActivity.findViewById(R.id.tabrow_location_query_response);

            if(locationURI != null) {
                //Location-Path Option
                String locationPath = locationURI.getPath();
                if(locationPath != null) {
                    locationPathRow.setVisibility(View.VISIBLE);
                    ((TextView) clientActivity.findViewById(R.id.txt_location_path_response)).setText(locationPath);
                } else {
                    locationPathRow.setVisibility(View.GONE);
                }

                //Location-Query Option
                String locationQuery = locationURI.getQuery();
                if(locationQuery != null) {
                    locationQueryRow.setVisibility(View.VISIBLE);
                    ((TextView) clientActivity.findViewById(R.id.txt_location_query_response)).setText(locationQuery);
                } else {
                    locationQueryRow.setVisibility(View.GONE);
                }
            } else {
                locationPathRow.setVisibility(View.GONE);
                locationQueryRow.setVisibility(View.GONE);
            }
        } catch(URISyntaxException ex) {
            String message = "ERROR (Malformed 'Location' Options): " + ex.getMessage();
            Toast.makeText(this.clientActivity, message, Toast.LENGTH_LONG).show();
        }

        //Content Format Option
        TableRow contentFormatRow = (TableRow) clientActivity.findViewById(R.id.tabrow_content_format_response);
        long contentFormatValue = coapResponse.getContentFormat();
        if(contentFormatValue != UintOptionValue.UNDEFINED){
            contentFormatRow.setVisibility(View.VISIBLE);
            ((TextView) clientActivity.findViewById(R.id.txt_contenttype_response)).setText("" + contentFormatValue);
        } else {
            contentFormatRow.setVisibility(View.GONE);
        }

        //Max-Age Option
        long maxAgeValue = coapResponse.getMaxAge();
        ((TextView) clientActivity.findViewById(R.id.txt_max_age_response)).setText("" + maxAgeValue);

        //Block2 Option
        long block2Number = coapResponse.getBlock2Number();
        if(block2Number != UintOptionValue.UNDEFINED){
            clientActivity.findViewById(R.id.tabrow_block2_response).setVisibility(View.VISIBLE);
            ((TextView) clientActivity.findViewById(R.id.txt_block2_response)).setText(
                    "No: " + block2Number + " | SZX: " + coapResponse.getBlock2Szx()
            );
        } else {
            clientActivity.findViewById(R.id.tabrow_block2_response).setVisibility(View.GONE);
        }

        //TODO: Size1 Option
        clientActivity.findViewById(R.id.tabrow_size1_response).setVisibility(View.GONE);


        //Response Code
        TextView txtResponseCode = (TextView) clientActivity.findViewById(R.id.txt_code_response);
        int messageCode = coapResponse.getMessageCode();
        txtResponseCode.setText("" + ((messageCode >>> 5) & 7) + "." + String.format("%02d", messageCode & 31));


        if(!coapResponse.isUpdateNotification()){
            RadioButton radStopObservation = (RadioButton) clientActivity.findViewById(R.id.rad_stop_observation);
            radStopObservation.setChecked(true);
            radStopObservation.setEnabled(false);
            radStopObservation.setVisibility(View.INVISIBLE);
        }
        //*/
    }
}
