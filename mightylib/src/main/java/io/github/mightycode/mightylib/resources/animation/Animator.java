package io.github.mightycode.mightylib.resources.animation;

import io.github.mightycode.mightylib.resources.Resources;

import java.util.HashMap;
import java.util.Objects;

public final class Animator {
    private final HashMap<String, Animation> animations;
    // Given to the animations it creates; null: those of the engine of the current view
    private final Resources resources;
    private Animation currentAnimation;
    private String currentAnimationName;

    private boolean animationChanged;

    // Its animations read the resources of the engine of the current view (the usual case)
    public Animator(){
        this.resources = null;
        animations = new HashMap<>();

        clear();
    }

    // Its animations read the given resources: outside of an engine (tests, tools)
    public Animator(Resources resources){
        this.resources = Objects.requireNonNull(resources, "resources");
        animations = new HashMap<>();

        clear();
    }


    public void clear(){
        animations.clear();
        currentAnimation = null;
    }


    // The current animation moves by the time of the tick (GameTime.DeltaTime)
    public void update(){
        if (currentAnimation == null)
            return;

        currentAnimation.update();
    }

    // The current animation moves by this time in seconds
    public void update(float seconds){
        if (currentAnimation == null)
            return;

        currentAnimation.update(seconds);
    }

    public void lateUpdate(){

        animationChanged = false;
    }

    public void addAndInitAnimation(String name, AnimationData animationData, boolean isLooping){
        Animation animation = resources != null ? new Animation(resources) : new Animation();
        animation.init(animationData, isLooping);

        addAnimation(name, animation);
        animationChanged = true;
    }


    private void addAnimation(String name, Animation animation){
        if (animations.containsKey(name)){
            System.err.println("Animator already contains an animation called : " + name);
            return;
        }

        animations.put(name, animation);

        if (currentAnimation != null)
            return;


        currentAnimation = animation;
        currentAnimationName = name;
    }


    public void setCurrentAnimation(String name, boolean resetOldAnimation){
        Animation animation = animations.get(name);
        if (animation == null) {
            System.err.println("No animation named : " + name);
            return;
        }

        if (resetOldAnimation)
            currentAnimation.restart();

        currentAnimation = animation;
        currentAnimationName = name;
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

    public boolean isFrameChanged(){
        if (currentAnimation == null)
            return false;

        return currentAnimation.isCurrentFrameChanged();
    }
}
