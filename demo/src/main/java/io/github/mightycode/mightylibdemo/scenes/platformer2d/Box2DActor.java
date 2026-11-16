package io.github.mightycode.mightylibdemo.scenes.platformer2d;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;

public class Box2DActor {
    protected Body body;
    protected final BodyDef bodyDef;
    protected final FixtureDef fixtureDef;

    public Box2DActor() {
        body = null;
        bodyDef = new BodyDef();
        fixtureDef = new FixtureDef();
    }

    public void setStatic() {
        bodyDef.type = BodyType.STATIC;
    }

    public void setDynamic() {
        bodyDef.type = BodyType.DYNAMIC;
    }

    public void setFixedRotation() {
        bodyDef.fixedRotation = true;
    }

    public void setShapeRectangle(Vec2 pos, Vec2 size) {
        bodyDef.position.set(pos);

        PolygonShape rect = new PolygonShape();
        rect.setAsBox(size.x / 2.f, size.y / 2.f);
        fixtureDef.shape = rect;
    }

    public void setPhysicsProperties(float density, float friction, float restitution) {
        fixtureDef.density = density;
        fixtureDef.friction = friction;
        fixtureDef.restitution = restitution;
    }

    public Vec2 getVelocity() {
        return body.getLinearVelocity();
    }

    public float getSpeed() {
        return getVelocity().length();
    }

    public void setVelocity(Vec2 v) {
        body.setLinearVelocity(v);
    }

    public void setSpeed(float s) {
        //setVelocity(getVelocity().setLength(s));
        setVelocity(getVelocity().mul(s));
    }

    public void initializePhysics(World w) {
        body = w.createBody(bodyDef);
        Fixture f = body.createFixture(fixtureDef);
        f.setUserData("main");
        body.setUserData(this);
    }

    public void applyForce(Vec2 force) {
        body.applyForceToCenter(force);
    }

    public void applyImpulse(Vec2 impulse) {
        body.applyLinearImpulse(impulse, body.getPosition());
    }

}
