package MightyLibrary.mightylib.graphics.texture;

import org.joml.Vector2f;

import java.util.HashMap;

public class Animator {
    private Vector2f referencePosition;

    private HashMap<String, Animation> animations;
    private Animation currentAnimation;
    private String currentAnimationName;

    public Animator(Vector2f position){
        this.referencePosition = position;
        animations = new HashMap<>();

        clear();
    }


    public void clear(){

        animations.clear();
        currentAnimation = null;
    }


    public void update(){
        if (currentAnimation == null) return;

        currentAnimation.update();
    }


    public void addAndInitAnimation(String name, AnimationData animationData, boolean isLooping){
        Animation animation = new Animation(referencePosition);
        animation.init(animationData, isLooping);

        addAnimation(name, animation);
    }


    public void addAnimation(String name, Animation animation){
        if (animations.containsKey(name)){
            System.err.println("Animator contains already an animation called : " + animation);
            return;
        }

        animations.put(name, animation);

        if (currentAnimation != null) return;
        currentAnimation = animation;
        currentAnimationName = name;
    }


    public void setCurrentAnimation(String animation){
        Animation anim = animations.get(animation);
        if (anim == null) {
            System.err.println("No animation named : " + animation);
            return;
        }

        currentAnimation.restart();
        currentAnimation = anim;
        currentAnimationName = animation;
    }


    public Animation getCurrentAnimation(){
        return currentAnimation;
    }
    public String getCurrentName(){
        return currentAnimationName;
    }


    public boolean isCurrentAnimationFinished(){
        if (currentAnimation == null)
            return false;

        return currentAnimation.isAnimationFinished();
    }


    public void setCurrentAnimationSpeed(float newSpeed){
        if (currentAnimation == null) return;

        currentAnimation.setAnimationSpeed(newSpeed);
    }
    public float getCurrentAnimationSpeed(){
        if (currentAnimation == null) return 0.0f;

        return currentAnimation.getAnimationSpeed();
    }
    public void setAllAnimationSpeed(float newSpeed){
        for (Animation animation : animations.values()){
            animation.setAnimationSpeed(newSpeed);
        }
    }


    public void setCurrentAnimationScale(float newScale){
        if (currentAnimation == null) return;

        currentAnimation.setAnimationScale(newScale);
    }
    public float getCurrentAnimationScale(){
        if (currentAnimation == null) return 0.0f;

        return currentAnimation.getAnimationScale();
    }
    public void setAllAnimationScale(float scale){
        for (Animation animation : animations.values()){
            animation.setAnimationScale(scale);
        }
    }
}
