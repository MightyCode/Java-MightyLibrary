package io.github.mightycode.mightylib.graphics.renderer._2D;

import io.github.mightycode.mightylib.graphics.renderer.RectangularFace;
import io.github.mightycode.mightylib.graphics.renderer.MeshRenderer;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.ViewCullBounds;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.BatchGeometry2D;
import io.github.mightycode.mightylib.resources.animation.Animation;
import io.github.mightycode.mightylib.resources.animation.Animator;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class Animation2DRenderer extends MeshRenderer {
    private static final int[] INDICES = RectangularFace.IndicesForSquare();

    private final Vector2f referencePosition;
    private Animator animator;
    private String currentTextureName;
    private final int positionIndex;
    private final int textureIndex;
    private final Vector4f texturePosition;

    private final Vector2f animationScale;

    private final Vector2f offsetRotation;

    private boolean horizontalFlip, verticalFlip;

    // What the mesh has, kept on the CPU for the batched draws
    private float[] positions;
    private final float[] textures;
    // Box of the positions (view culling), reused
    private final float[] cullBox = new float[4];
    // Reused by each frame change (no allocation)
    private final Vector2i frameSize = new Vector2i();
    private final Vector2i hotPoint = new Vector2i();
    private final Vector3f workVector = new Vector3f();

    public Animation2DRenderer(String shaderName){
        super(shaderName, true);

        animationScale = new Vector2f(1);

        offsetRotation = new Vector2f(0, 0);

        texturePosition = RectangularFace.BasicTexturePosition();

        // The frame changes only write the copy: sent to the GPU only when drawn alone (Mesh.updateVbo)
        getMesh().keepCpuData();
        getMesh().setEboStorage(Mesh.STATIC_STORE);
        getMesh().setEbo(INDICES);
        positions = calculatePosition();
        textures = new float[8];
        texturePos(textures);
        positionIndex = getMesh().addVboFloat(positions, 2, Mesh.DYNAMIC_STORE);
        textureIndex = getMesh().addVboFloat(textures, 2, Mesh.DYNAMIC_STORE);

        referencePosition = new Vector2f();

        horizontalFlip = false;
        verticalFlip = false;
    }


    public void init(Animator animator){
        this.animator = animator;

        updateInfoForFrame();
        updateTextureChannel();
    }

    // The texture of the usable animation: it changes when the animation changes, or is loaded after its fallback
    private void updateTextureChannel() {
        String textureName = animator.getCurrentAnimation().getData().getTextureName();
        if (!textureName.equals(currentTextureName)) {
            currentTextureName = textureName;
            setMainTextureChannel(textureName);
        }
    }


    public void update(){
        animator.update();

        String previousTexture = currentTextureName;
        updateTextureChannel();

        if (animator.animationChanged() || !currentTextureName.equals(previousTexture)) {
            updateInfoForFrame();
        } else if (animator.isFrameChanged()){
            updateInfoForFrame();
        }

        animator.lateUpdate();
    }

    // The texture coordinates, position and scale of the current frame (no allocation)
    public void updateInfoForFrame(){
        animator.getCurrentAnimation().currentTexturePosition(texturePosition);
        updateTextures();

        updatePosition();
        this.setScale(this.animationScale);
    }

    public void setPosition(Vector2f position){
        referencePosition.x = position.x;
        referencePosition.y = position.y;

        updatePosition();
    }

    private void updatePosition(){
        Vector3f temp = workVector.set(referencePosition.x, referencePosition.y, 0.0f);

        // Take into account hot point of each frame of animation
        if (animator != null) {
            Animation animation = animator.getCurrentAnimation();
            animation.currentHotPoint(horizontalFlip, verticalFlip, hotPoint);
            animation.getFrameSize(frameSize);

            temp.x -= hotPoint.x * animationScale.x;
            temp.y -= hotPoint.y * animationScale.y;

            temp.x += frameSize.x * animationScale.x * offsetRotation.x;
            temp.y += frameSize.y * animationScale.y * offsetRotation.y;
        }

        // super call
        setPosition(temp);
    }

    public void setShiftRotation(EDirection direction){
        switch (direction){
            case RightUp:
                setShiftRotation(new Vector2f(0, 0));
                break;
            case Up:
                setShiftRotation(new Vector2f(0.5f, 0));
                break;
            case LeftUp:
                setShiftRotation(new Vector2f(1, 0));
                break;
            case Left:
                setShiftRotation(new Vector2f(1, 0.5f));
                break;
            case LeftDown:
                setShiftRotation(new Vector2f(1, 1));
                break;
            case Down:
                setShiftRotation(new Vector2f(0.5f, 1));
                break;
            case RightDown:
                setShiftRotation(new Vector2f(0, 1));
                break;
            case Right:
                setShiftRotation(new Vector2f(0, 0.5f));
                break;
            case None:
            default:
                setShiftRotation(new Vector2f(0.5f, 0.5f));
                break;
        }
    }

    public void setShiftRotation(Vector2f offsetRotation){
        this.offsetRotation.x = offsetRotation.x;
        this.offsetRotation.y = offsetRotation.y;

        positions = calculatePosition();
        getMesh().updateVbo(positions, positionIndex);
    }

    private void updateTextures() {
        texturePos(textures);
        getMesh().updateVbo(textures, textureIndex);
    }

    // Batched like a sprite when its shader has a batch equivalent and its depth test is off (see Renderer)
    @Override
    protected boolean submitToBatch(DrawBatch2D batch) {
        if (!BatchGeometry2D.CanBatch(isDepthTest(), getMesh().getDrawMode()))
            return false;

        int place = batchPlace(batch);
        if (place < 0)
            return false;
        if (viewCulled(batch, place))
            return true;

        int state = batchState(place);

        BatchGeometry2D.WriteTriangles(batch, state, getLayer(), getSortDepth(), modelMatrix(), positions, textures,
                positions.length / 2, batchColor(), INDICES, INDICES.length);
        return true;
    }

    // The quad of the frame, in the space of the model
    @Override
    protected boolean viewCullBounds(ViewCullBounds out) {
        if (!BatchGeometry2D.Box(positions, positions.length / 2, cullBox))
            return false;

        out.local(cullBox[0], cullBox[1], cullBox[2], cullBox[3]);
        return true;
    }

    private float[] calculatePosition(){
        return new float[]{
                0 - offsetRotation.x, 1 - offsetRotation.y,
                0 - offsetRotation.x, 0 - offsetRotation.y,
                1 - offsetRotation.x, 0 - offsetRotation.y,
                1 - offsetRotation.x, 1 - offsetRotation.y
        };
    }

    // The texture coordinates of the 4 vertices, flips applied, written in out (8 floats)
    private void texturePos(float[] out){
        float left = horizontalFlip ? texturePosition.y : texturePosition.x;
        float right = horizontalFlip ? texturePosition.x : texturePosition.y;
        float top = verticalFlip ? texturePosition.w : texturePosition.z;
        float bottom = verticalFlip ? texturePosition.z : texturePosition.w;

        out[0] = left;
        out[1] = bottom;
        out[2] = left;
        out[3] = top;
        out[4] = right;
        out[5] = top;
        out[6] = right;
        out[7] = bottom;
    }

    public Animation2DRenderer setHorizontalFlip(boolean state){
        horizontalFlip = state;

        updateTextures();

        return this;
    }

    public Animation2DRenderer setVerticalFlip(boolean state){
        verticalFlip = state;

        updateTextures();

        return this;
    }


    public Vector2f getScale(){
        return animationScale;
    }

    public Animator getAnimator() {
        return  animator;
    }

    @Override
    public Vector3f position(){
        return new Vector3f(referencePosition.x, referencePosition.y, super.position().z);
    }

    public void setScale(Vector2f scale){
        this.animationScale.x = scale.x;
        this.animationScale.y = scale.y;

        Vector2i info = animator.getCurrentAnimation().getFrameSize(frameSize);

        super.setScale(workVector.set(info.x * animationScale.x, info.y * animationScale.y, 1f));
    }
}
