package MightyLibrary.mightylib.graphics.texture;

import java.util.HashMap;

public class Animator {
    private final HashMap<String, Animation> animations;
    private Animation currentAnimation;
    private String currentAnimationName;

    private boolean animationChanged;

    public Animator(){
        animations = new HashMap<>();

        clear();
    }


    public void clear(){
        animations.clear();
        currentAnimation = null;
    }


    public void update(){
        if (currentAnimation == null)
            return;

        currentAnimation.update();
        animationChanged = false;
    }


    public void addAndInitAnimation(String name, AnimationData animationData, boolean isLooping){
        Animation animation = new Animation();
        animation.init(animationData, isLooping);

        addAnimation(name, animation);
        animationChanged = true;
    }


    private void addAnimation(String name, Animation animation){
        if (animations.containsKey(name)){
            System.err.println("Animator already contains an animation called : " + animation);
            return;
        }

        animations.put(name, animation);

        if (currentAnimation != null)
            return;


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
        animationChanged = true;
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


    public boolean animationChanged(){
        return animationChanged;
    }
}
