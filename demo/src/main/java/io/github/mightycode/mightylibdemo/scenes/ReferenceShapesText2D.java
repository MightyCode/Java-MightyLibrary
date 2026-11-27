package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.ArcRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.CircleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EPointShape;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeCap;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeJoin;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EllipseRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.HexagonRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.PointsRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.PolylineRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RegularPolygonRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RoundedRectangleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.TriangleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.VectorShape2DRenderer;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.color.ColorList;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import io.github.mightycode.mightylibdemo.main.DemoCamera;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference scene of the image tests (ReferenceImagesGLTest): texts of the fonts of the engine, the seven vector
 * shapes filled and stroked, polylines with their joins and caps, points. Still: no help text, no counter. Do not
 * change it without generating its references again (tools/references.py --update).
 */
public class ReferenceShapesText2D extends Scene {
    private static final Color4f FILL = new Color4f(0.25f, 0.55f, 0.9f, 1f);
    private static final Color4f STROKE = new Color4f(0.95f, 0.85f, 0.3f, 1f);
    private static final Color4f HALF = new Color4f(0.95f, 0.3f, 0.3f, 0.6f);

    private final List<VectorShape2DRenderer> shapes = new ArrayList<>();
    private final List<Text> texts = new ArrayList<>();

    @Override
    public void launch(String[] args) {
        DemoCamera.free(this);
        setClearColor(0.12f, 0.12f, 0.16f, 1f);

        // The seven shapes of the batch, filled with a stroke
        shape(new RoundedRectangleRenderer().setCornerRadius(12f), 50, 50, 80, 56);
        shape(new CircleRenderer(), 135, 50, 60, 60);
        shape(new EllipseRenderer(), 220, 50, 80, 50);
        shape(new TriangleRenderer(), 305, 50, 70, 60);
        shape(new ArcRenderer().setInnerRatio(0.5f).setAngles((float) Math.PI, (float) (Math.PI * 1.75)), 390, 50,
                60, 60);
        shape(new RegularPolygonRenderer(5), 475, 50, 60, 60);
        shape(new HexagonRenderer(), 565, 50, 70, 70 * HexagonRenderer.STRETCH_RATIO);

        // Stroke only, and half transparent over the others
        VectorShape2DRenderer ring = shape(new CircleRenderer(), 50, 130, 60, 60);
        ring.setColorMode(new Color4f(0f, 0f, 0f, 0f));
        VectorShape2DRenderer over = shape(new RoundedRectangleRenderer().setCornerRadius(6f), 90, 130, 70, 40);
        over.setColorMode(HALF);

        // A polyline per join, with a cap each
        EStrokeJoin[] joins = {EStrokeJoin.MITER, EStrokeJoin.ROUND, EStrokeJoin.BEVEL};
        EStrokeCap[] caps = EStrokeCap.values();
        for (int i = 0; i < joins.length; ++i) {
            PolylineRenderer zigzag = new PolylineRenderer();
            zigzag.setPoints(List.of(new Vector2f(-30, 18), new Vector2f(-10, -18), new Vector2f(8, 18),
                    new Vector2f(30, -8)));
            zigzag.setJoin(joins[i]);
            zigzag.setCap(caps[i % caps.length]);
            place(zigzag, 200 + i * 80, 130, 1, 1);
            zigzag.setStrokeColor(STROKE);
            zigzag.setStrokeWidth(8f);
            shapes.add(zigzag);
        }

        PointsRenderer round = new PointsRenderer();
        PointsRenderer square = new PointsRenderer();
        square.setPointShape(EPointShape.SQUARE);
        for (int i = 0; i < 6; ++i) {
            round.addPoint(new Vector2f(i * 18f, 0f));
            square.addPoint(new Vector2f(i * 18f, 20f));
        }
        round.setPointSize(10f);
        round.setColorMode(FILL);
        square.setPointSize(10f);
        square.setColorMode(STROKE);
        round.setPosition(new Vector2f(470, 120));
        square.setPosition(new Vector2f(470, 120));
        shapes.add(round);
        shapes.add(square);

        text(EngineResources.DEFAULT_FONT, 28, ETextAlignment.Left, ColorList.White(), 20, 190,
                "Reference text: AaBbQq 0123");
        text(EngineResources.CONSOLE_FONT, 16, ETextAlignment.Left, new Color4f(0.6f, 0.9f, 0.6f, 1f), 20, 235,
                "mono > fn(x) = [x * 2]; // {}");
        Text wrapped = text(EngineResources.DEFAULT_FONT, 16, ETextAlignment.Center, STROKE, 500, 290,
                "A paragraph wrapped at the spaces and centered, on three lines or so.");
        wrapped.setMaxWidth(220);
        text(EngineResources.DEFAULT_FONT, 48, ETextAlignment.Left, FILL, 20, 280, "Big 48");
    }

    private VectorShape2DRenderer shape(VectorShape2DRenderer shape, float x, float y, float width, float height) {
        place(shape, x, y, width, height);
        shape.setColorMode(FILL);
        shape.setStrokeColor(STROKE);
        shape.setStrokeWidth(3f);
        shapes.add(shape);
        return shape;
    }

    private static void place(VectorShape2DRenderer shape, float x, float y, float width, float height) {
        shape.setReferenceDirection(EDirection.None);
        shape.setSizePix(width, height);
        shape.setPosition(new Vector2f(x, y));
    }

    private Text text(String font, float size, ETextAlignment alignment, Color4f color, float x, float y,
                      String content) {
        Text text = new Text();
        text.setFont(font);
        // Left aligned: (x, y) is the top left corner; centered: the center
        text.setFontSize(size).setAlignment(alignment).setColor(color)
                .setReference(alignment == ETextAlignment.Center ? EDirection.None : EDirection.LeftUp)
                .setPosition(new Vector2f(x, y));
        text.setText(content);
        texts.add(text);
        return text;
    }

    @Override
    public void update() {
        super.update();

        if (context().getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManager().setNewScene(new MenuScene(), new String[]{});
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        for (VectorShape2DRenderer shape : shapes)
            shape.display();
        for (Text text : texts)
            text.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();
        for (VectorShape2DRenderer shape : shapes)
            shape.unload();
        shapes.clear();
        for (Text text : texts)
            text.unload();
        texts.clear();
    }
}
