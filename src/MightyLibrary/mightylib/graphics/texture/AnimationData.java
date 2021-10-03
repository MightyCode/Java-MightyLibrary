package MightyLibrary.mightylib.graphics.texture;

public class AnimationData {
    private static final int TEXTURE_POS = 0;
    private static final int SIZE_FRAME_POS = 1;
    private static final int FRAME_ENUM_START_POS = 2;

    private FrameData[] framesData;
    private String textureName;

    public AnimationData(){
        textureName = "";
    }

    public void init(String data){
        String[] parts = data.split("\n");
        textureName = parts[TEXTURE_POS].trim();

        int numberFrames = Integer.parseInt(parts[SIZE_FRAME_POS].trim());
        for (int i = 0; i < numberFrames; ++i){
            framesData[i] = new FrameData();
            framesData[i].init(parts[FRAME_ENUM_START_POS + i].trim());
        }
    }

    public FrameData getFrame(int index){
        if (framesData == null)
            return null;
        if (0 > index || index > frameNumber())
            return null;

        return framesData[index];
    }

    public int frameNumber(){
        return framesData.length;
    }
}
