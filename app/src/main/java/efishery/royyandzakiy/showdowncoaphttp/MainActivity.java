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
            totalRequestValue, packetLossValue, totalRequestTimeValue, requestTimeValue, cpuProcessingValue, contentLengthValue, totalContentLengthValue,
            responseSuccessValue, responseFailValue, responseMessage, status;

    // SYSTEM
    private long startTime;
    private ArrayList<ReqResData> listReqResData = new ArrayList<ReqResData>();
    private RequestQueue queue;
    private CoapClient clientApplication;
    private String type = "http";
    private boolean isProcessing = false;
    private int countSuccess = 0, countFail = 0, countRequest = 10;
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

        Log.d("durasiTotal:", String.valueOf(durasiTotal));
        Log.d("durasiMax:", String.valueOf(durasiMax));
        Log.d("durasiMin:", String.valueOf(durasiMin));
        Log.d("listReqResData:", String.valueOf(listReqResData.size()));
        Log.d("durasiAvg:", String.valueOf(durasiAvg));

        //=== Change messages
        String elapsedTime = String.valueOf(System.currentTimeMillis() - startTime);
        String toastText = (countFail + countSuccess) + " response done! " + elapsedTime  + " ms";
        Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
        status.setText("Response done!");
        totalRequestValue.setText(String.valueOf(countFail + countSuccess) + " packets");
        packetLossValue.setText(String.valueOf(countFail) + " packets");
        totalRequestTimeValue.setText(String.valueOf(elapsedTime) + " ms");
        requestTimeValue.setText(String.valueOf(durasiAvg) + " / " + String.valueOf(durasiMax) + " / " + String.valueOf(durasiMin) + " ms"); // hitung AVG/MAX/MIN
        resetVars();
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
                long block2Num = coapResponse.getBlock2Number();
                String text = "";
                //text = "Response received";
                //if (block2Num != UintOptionValue.UNDEFINED) {
                //    text += " (" + block2Num + " blocks in " + duration + " ms)";
                //} else {
                    text += " (after " + duration + " ms)";
                //}

                totalRequestTimeValue.setText(text);

                //Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();

                responseReceived(serviceURI, coapResponse);
            }
        });
    }

    public void responseReceived(URI uri, CoapResponse coapResponse){
        Log.d("DEBUG::","MainActivity::responseReceived::countTotal="+(countSuccess+countFail)+";countSuccess="+countSuccess+";countFail="+countFail);
        if ((countSuccess + countFail) == countRequest) {
            requestDone();
        }
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
