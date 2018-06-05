package efishery.royyandzakiy.showdowncoaphttp;

import android.os.AsyncTask;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

public class CoAP {
    // FOR PUT ::

    public class RDUData extends AsyncTask<Void,Void,Void> {

        @Override    protected Void doInBackground(Void... params) {
            try {
                CoapClient coapClient = new CoapClient();
                //Create socket address from server name and port
                InetSocketAddress remoteEndpoint = new InetSocketAddress
                        (InetAddress.getByName(rduserverName), portNumber);

                URI serviceURI = new URI("coap", null, serverName,
                        remoteEndpoint.getPort(), "<the path of uri>", null, null);
                //Create initial CoAP request
                CoapRequest coapRequest = new CoapRequest(MessageType.CON, 3, serviceURI);
                String RDUMsg = "#@0002,12.34,12.00,34.00,23.00,0620.00";
                byte[] b = RDUMsg.getBytes(Charset.forName("UTF-8"));
                coapRequest.setContent(b, ContentFormat.TEXT_PLAIN_UTF8);
                coapClient.sendCoapRequest(coapRequest, remoteEndpoint, new ClientCallback() {
                    @Override                public void processCoapResponse(CoapResponse coapResponse) {
                        showToast("RDU Data displayed ");
                    }
                });

            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    // FOR GET:::

    public class GetWeightData extends AsyncTask<Void,Void,Void>{

        private int retransmissionCounter = 0;

        @Override    protected Void doInBackground(Void... params) {
            try {
                coapClient = new CoapClient();

                //Create socket address from server name and port
                final InetSocketAddress remoteEndpoint = new InetSocketAddress(
                        InetAddress.getByName(serverName), portNumber);

                URI serviceURI = new URI("coap", null, serverName, remoteEndpoint.getPort(),
                        "/weighing/scale", null, null);
                //Create initial CoAP request
                final CoapRequest coapRequest = new CoapRequest(MessageType.CON, 1, serviceURI);
                coapRequest.setObserve(0);
                long data =  coapRequest.getObserve();
                coapClient.sendCoapRequest(coapRequest, remoteEndpoint, new ClientCallback() {
                    @Override
                    public void processCoapResponse(CoapResponse coapResponse) {
                        showToast("Weight Data :: " + coapResponse.getContent().toString(CoapMessage.CHARSET));
                   /* coapRequest.setObserve(0);
 coapResponse.getObserve();*/
                        boolean isObs = coapRequest.isObservationRequest();
                        Token token = coapResponse.getToken();


                    }

                    @Override
                    public boolean continueObservation() {
                        showToast("Weight Data Call Again");

                        return true;
                    }

                    @Override
                    public void processRetransmission() {

                    }

                    @Override
                    public void processContinueResponseReceived(BlockSize block1Size) {
                        super.processContinueResponseReceived(block1Size);
                    }
                    @Override
                    public void processEmptyAcknowledgement(){
                    }
                    @Override
                    public void processReset(){
                    }
                    @Override
                    public void processTransmissionTimeout(){
                    }
                    @Override
                    public void processResponseBlockReceived(final long receivedLength, final long expectedLength) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String expected = expectedLength == -1 ? "UNKNOWN" : ("" + expectedLength);
                            }
                        });
                    }
                    @Override
                    public void processMiscellaneousError(final String description) {
                    }

                });

            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            //   coapClient.shutdown();        return null;
        }
    }
}
