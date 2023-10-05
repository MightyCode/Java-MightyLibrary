package MightyLibrary.mightylib.network;

import MightyLibrary.mightylib.main.GameTime;

public final class ClientThread extends CommunicationThread {
    public static final int SLEEP_TIME = 10; // MILLISECONDS

    public static final int TIME_OUT_TIME = 40;

    public static final String HEADER_SEPARATION = "|";

    public ClientTcp client;
    public boolean shouldTryConnection;

    public float timeOutCounter;

    public ClientThread() {
        timeOutCounter = GameTime.currentTime();
    }

    public void shouldTryConnection(){
        shouldTryConnection = true;
    }

    @Override
    public void setup(String configurationName) {
        super.setup(configurationName);
        System.out.println("[Debug] Setting up the client...");
        client = new ClientTcp(this.configuration);
    }


    public ClientTcp getClient() {
        return this.client;
    }

    public void reset(){
        super.reset();

        receivedMessageNumber = -1;
        processedMessageNumber = 0;

        shouldTryConnection = false;
        if (client.isConnected())
            client.closeConnection();
    }

    public void run(String configurationName) {
        this.setup(configurationName);
        reset();
        this.running = true;

        try {
            while (this.running){
                Thread.sleep(SLEEP_TIME);
                //System.out.println(messageProcessed + " " + messageReceived + " " + messageToSend.size());

                if (client.isConnected()){
                    if (messageToSend.size() > 0 && receivedMessageNumber == processedMessageNumber) {
                        String toSend = processedMessageNumber + HEADER_SEPARATION + messageToSend.get(processedMessageNumber);

                        System.out.println("Messaged sent(" + receivedMessageNumber + ") :" + toSend);
                        client.sendMessage(toSend);

                        processedMessageNumber += 1;
                    }

                    String result = client.readMessage();
                    if (result != null) {
                        /*if (result.contains("Connection lost")){
                            this.running = false;
                        }*/
                        timeOutCounter = GameTime.currentTime();

                        if (result.contains("|")) {
                            int numberMessage = Integer.parseInt(result.substring(0, result.indexOf(HEADER_SEPARATION)));
                            if (numberMessage >= receivedMessageNumber)
                                receivedMessageNumber = numberMessage + 1;

                            System.out.println("Messaged received(" + receivedMessageNumber + ") :" + result);

                            result = result.substring(result.indexOf(HEADER_SEPARATION) + 1);

                            if (numberMessage == SPECIAL_ID){
                                receivedMessages.add(new NetworkMessage(SPECIAL_ID, result));
                            } else {
                                receivedMessages.add(new NetworkMessage(receivedMessageNumber - 1, result));
                                messageToSend.remove(receivedMessageNumber - 1);
                            }
                        } else {
                            System.err.println("Message without header : \n" + result);
                        }
                    }
                } else if (shouldTryConnection) {
                    client.tryCreateConnection();
                    shouldTryConnection = false;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("End of thread client");
    }

    public boolean hasTimeOut(){
        return GameTime.currentTime() - timeOutCounter >= TIME_OUT_TIME;
    }

    @Override
    public void doStop() {
        this.running = false;
        if (client != null && client.isConnected()) {
            client.closeConnection();
        }
    }
}
