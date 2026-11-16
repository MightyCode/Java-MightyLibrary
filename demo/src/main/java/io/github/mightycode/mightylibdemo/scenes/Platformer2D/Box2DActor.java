package io.github.mightycode.mightylibdemo.scenes.Platformer2D;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;

public class Box2DActor {
    protected Body body;
    protected final BodyDef bodyDef;
    protected final FixtureDef fixtureDef;

    protected Float maxSpeed;
    protected Float maxSpeedX;
    protected Float maxSpeedY;

    public Box2DActor() {
        body = null;
        bodyDef = new BodyDef();
        fixtureDef = new FixtureDef();

        maxSpeed = null;
        maxSpeedX = null;
        maxSpeedY = null;
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

    public void update() {
        // cap max speeds, if they have been set
        if (maxSpeedX != null)
        {
            Vec2 v = getVelocity();
            v.x = MathUtils.clamp(v.x, -maxSpeedX, maxSpeedX);
            setVelocity(v);
        }
        if (maxSpeedY != null)
        {
            Vec2 v = getVelocity();
            v.y = MathUtils.clamp(v.y, -maxSpeedY, maxSpeedY);
            setVelocity(v);
        }
        if (maxSpeed != null)
        {
            float s = getSpeed();
            if (s > maxSpeed)
                setSpeed(maxSpeed);
        }
    }
}
