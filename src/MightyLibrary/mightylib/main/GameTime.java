package MightyLibrary.mightylib.main;

public class GameTime {
    private static GameTime gametime;

    private long currentTime;
    private long temp;

    private float elapsedTime;

    private static float NANO_TO_SEC = 1000000000.0f;

    public GameTime(){
        elapsedTime = 0;
        currentTime = System.nanoTime();
    }

    public static float DeltaTime(){
        return gametime.elapsedTime;
    }


    public static float getDeltaTSinceLastUpdate(){
        return gametime.deltaTimeSinceLastUpdate();
    }


    private float deltaTimeSinceLastUpdate(){
        temp = System.nanoTime();

        return ((float)(temp - currentTime)) / NANO_TO_SEC;
    }


    public static void update(){
        if (gametime == null){
            gametime = new GameTime();
        }

        gametime.updateGameTime();

    }


    private void updateGameTime(){
        temp = System.nanoTime();
        elapsedTime = ((float)(temp - currentTime)) / NANO_TO_SEC;
        currentTime = temp;
    }
}
