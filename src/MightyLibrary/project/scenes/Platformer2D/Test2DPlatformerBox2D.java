package MightyLibrary.project.scenes.Platformer2D;

import MightyLibrary.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import MightyLibrary.mightylib.main.utils.GameTime;
import MightyLibrary.mightylib.physics.B2Utils;
import MightyLibrary.mightylib.scenes.Scene;
import MightyLibrary.mightylib.utils.math.color.Color4f;
import MightyLibrary.mightylib.utils.math.color.ColorList;
import MightyLibrary.mightylib.utils.math.geometry.EDirection;
import MightyLibrary.project.scenes.loadingScenes.LoadingSceneImplementation;
import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.contacts.Contact;
import java.util.ArrayList;

public class Test2DPlatformerBox2D extends Scene {
    private World world;
    private PlayerManager player;

    private final static int VELOCITY_ITERATIONS = 6;
    private final static int POSITION_ITERATIONS = 2;

    private final ArrayList<StaticBox> staticBoxes = new ArrayList<>();

    @Override
    public void init(String[] args) {
        super.init(args, new LoadingSceneImplementation());
    }

    @Override
    public void launch(String[] args) {
        super.launch(args);

        B2Utils.setB2ToEngineVector(16);

        world = new World(new Vec2(0, -9.81f));

        setClearColor(120, 200, 255, 1f);

        // === Static environment ===
        staticBoxes.add(createStaticBox(new Vec2(-10, 20), new Vec2(10, 5), ColorList.Red()));
        staticBoxes.add(createStaticBox(new Vec2(-5, 0), new Vec2(30, 5), ColorList.Black()));
        staticBoxes.add(createStaticBox(new Vec2(20, -10), new Vec2(30, 5), ColorList.Black()));

        // === Player ===
        PlayerController controller = new PlayerController(mainContext.getInputManager());
        player = new PlayerManager(world, controller, new Vec2(0, 5), main2DCamera, mainContext.getWindow().getInfo().getSizeCopy());

        setContactListener();
    }

    private StaticBox createStaticBox(Vec2 pos, Vec2 size, Color4f color) {
        return new StaticBox(world, pos, size, color);
    }

    @Override
    public void update() {
        super.update();

        // Fixed step keeps the physics deterministic
        world.step(GameTime.FixedDeltaTime(), VELOCITY_ITERATIONS, POSITION_ITERATIONS);

        player.update();
    }


    private void setContactListener() {
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {

                System.out.println(contact.getFixtureA().getBody().getUserData() + " " +  contact.getFixtureB().getBody().getUserData());

                PlayerManager playerContact = B2Utils.getContactObject(contact, player);
                StaticBox box = B2Utils.getContactObject(contact, StaticBox.class);

                System.out.println(playerContact + " " + box);

                if (playerContact != null && box != null) {
                    playerContact.getValues().setGroundCount(1);
                }
            }

            @Override
            public void endContact(Contact contact) {
                PlayerManager playerContact = B2Utils.getContactObject(contact, player);
                StaticBox box = B2Utils.getContactObject(contact, StaticBox.class);

                if (playerContact != null && box != null) {
                    playerContact.getValues().setGroundCount(-1);
                }
            }

            @Override
            public void preSolve(Contact contact, Manifold manifold) {

            }

            @Override
            public void postSolve(Contact contact, ContactImpulse contactImpulse) {

            }
        });
    }

    @Override
    public void display() {
        super.setVirtualScene();
        clear();

        // === Draw static world ===
        for (StaticBox box : staticBoxes) {
            box.display();
        }

        // === Draw player ===
        player.display();

        super.display();
        super.setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        player.unload();
        for (StaticBox box : staticBoxes)
            box.unload();
    }

    // ============================================ //
    // ============ INNER CLASSES ================= //
    // ============================================ //


    /** Simple static platform or wall */
    private static class StaticBox extends Box2DActor {
        private final RectangleRenderer renderer;

        public StaticBox(World world, Vec2 pos, Vec2 size, Color4f color) {
            setStatic();
            setShapeRectangle(pos, size);
            initializePhysics(world);

            renderer = new RectangleRenderer("colorShape2D");
            renderer.load(0);
            renderer.setColorMode(color);
            renderer.setReferenceDirection(EDirection.None);
            renderer.setSizePix(B2Utils.b2ToEngine(size));
            renderer.setPosition(B2Utils.b2ToEngine(pos));
        }

        public void display() {
            renderer.display();
        }

        public void unload() {
            renderer.unload();
        }
    }
}
