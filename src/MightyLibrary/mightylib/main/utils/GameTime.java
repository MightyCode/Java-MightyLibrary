package MightyLibrary.mightylib.main.utils;

public class GameTime {
    private static GameTime gametime;

    private long currentTime;
    private long temp;

    private float elapsedTime;

    private static final float NANO_TO_SEC = 1000000000.0f;

    // Duration of one tick (1 / TPS), to use for deterministic simulations like physics
    private static float fixedDeltaTime = 1f / 60f;

    public static float FixedDeltaTime(){
        return fixedDeltaTime;
    }

    public static void setFixedDeltaTime(float newFixedDeltaTime){
        fixedDeltaTime = newFixedDeltaTime;
    }

    public GameTime(){
        elapsedTime = 0;
        currentTime = System.nanoTime();
    }

    public static float DeltaTime(){
        if (gametime == null){
            gametime = new GameTime();
        }

        return gametime.elapsedTime;
    }


    public static float getDeltaTSinceLastUpdate(){
        if (gametime == null){
            gametime = new GameTime();
        }

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

    public static float currentTime(){
        if (gametime == null){
            gametime = new GameTime();
        }

        return gametime.getCurrentTIme();
    }

    public float getCurrentTIme(){
        return currentTime / NANO_TO_SEC;
    }

    private void updateGameTime(){
        temp = System.nanoTime();
        elapsedTime = ((float)(temp - currentTime)) / NANO_TO_SEC;
        currentTime = temp;
    }
}