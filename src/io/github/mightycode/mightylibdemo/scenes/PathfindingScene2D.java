package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.algorithms.dijkstra.Dijkstra;
import io.github.mightycode.mightylib.algorithms.graph.Graph;
import io.github.mightycode.mightylib.algorithms.graph.Node;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Graph and Dijkstra : a grid of cells is turned into a Graph (one Node per free cell, linked to its four
 * neighbours), then Dijkstra.findShortestPath looks for the shortest path between the start and the end cells.
 * Left click paints or erases walls, shift + left click moves the start, right click moves the end.
 */
public class PathfindingScene2D extends Scene {
    private static final int COLUMNS = 32;
    private static final int ROWS = 15;
    private static final float CELL = 40f;
    private static final float TOP = 100f;

    private static final Color4f FLOOR = new Color4f(0.18f, 0.2f, 0.25f, 1f);
    private static final Color4f WALL = new Color4f(0.03f, 0.03f, 0.05f, 1f);
    private static final Color4f PATH = new Color4f(0.95f, 0.8f, 0.2f, 1f);
    private static final Color4f START = new Color4f(0.2f, 0.85f, 0.35f, 1f);
    private static final Color4f END = new Color4f(0.9f, 0.25f, 0.25f, 1f);

    private final boolean[][] walls = new boolean[ROWS][COLUMNS];
    private int startX = 2, startY = 2;
    private int endX = COLUMNS - 3, endY = ROWS - 3;

    private Node[][] nodes;
    private Graph graph;
    private List<Node> path = new ArrayList<>();

    private RectangleRenderer rectangle;
    private Text info;
    private Text result;

    // true when the current drag adds walls, false when it removes them
    private boolean painting;
    private boolean dragging;

    @Override
    public void launch(String[] args) {
        setClearColor(0.1f, 0.1f, 0.12f);
        mainContext.getMouseManager().setCursor(true);

        rectangle = new RectangleRenderer("colorShape2D");

        info = new Text();
        info.setFont("bahnschrift")
                .setFontSize(24)
                .setReference(EDirection.LeftUp)
                .setAlignment(ETextAlignment.Left)
                .setColor(new Color4f(1, 1, 1, 1))
                .setPosition(new Vector2f(20, 15))
                .setText("Left click : walls    Shift + left click : start    Right click : end");

        result = new Text();
        result.setFont("bahnschrift")
                .setFontSize(24)
                .setReference(EDirection.LeftUp)
                .setAlignment(ETextAlignment.Left)
                .setColor(PATH)
                .setPosition(new Vector2f(20, 55));

        // A few walls to make the path bend
        for (int y = 3; y < ROWS - 2; ++y)
            walls[y][10] = true;
        for (int y = 2; y < ROWS - 3; ++y)
            walls[y][20] = true;

        rebuild();
    }

    /** Builds the graph from the free cells and computes the path again. */
    private void rebuild() {
        graph = new Graph();
        nodes = new Node[ROWS][COLUMNS];

        for (int y = 0; y < ROWS; ++y) {
            for (int x = 0; x < COLUMNS; ++x) {
                if (walls[y][x])
                    continue;

                nodes[y][x] = new Node(y * COLUMNS + x, x * CELL + CELL / 2f, TOP + y * CELL + CELL / 2f);
                graph.add(nodes[y][x]);
            }
        }

        for (int y = 0; y < ROWS; ++y) {
            for (int x = 0; x < COLUMNS; ++x) {
                if (nodes[y][x] == null)
                    continue;

                if (x + 1 < COLUMNS && nodes[y][x + 1] != null) {
                    nodes[y][x].add(nodes[y][x + 1]);
                    nodes[y][x + 1].add(nodes[y][x]);
                }

                if (y + 1 < ROWS && nodes[y + 1][x] != null) {
                    nodes[y][x].add(nodes[y + 1][x]);
                    nodes[y + 1][x].add(nodes[y][x]);
                }
            }
        }

        Node start = nodes[startY][startX];
        Node end = nodes[endY][endX];

        path = new ArrayList<>();
        if (start == null || end == null) {
            result.setText("Start or end is inside a wall");
            return;
        }

        path = Dijkstra.findShortestPath(graph, start, end);

        if (path.isEmpty())
            result.setText("No path : the end is closed off");
        else
            result.setText("Path : " + path.size() + " nodes, " + graph.size() + " nodes in the graph");
    }

    @Override
    public void update() {
        super.update();

        InputManager input = mainContext.getInputManager();

        if (input.inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});

        Vector2f mouse = main2DCamera.transformPosition(mainContext.getMouseManager().pos());
        int x = (int) Math.floor(mouse.x / CELL);
        int y = (int) Math.floor((mouse.y - TOP) / CELL);
        boolean inside = x >= 0 && x < COLUMNS && y >= 0 && y < ROWS;

        if (input.inputReleased(ActionId.LEFT_CLICK))
            dragging = false;

        if (!inside)
            return;

        if (input.inputPressed(ActionId.LEFT_CLICK)) {
            if (input.getState(ActionId.SHIFT)) {
                if (!(x == endX && y == endY)) {
                    walls[y][x] = false;
                    startX = x;
                    startY = y;
                    rebuild();
                }
            } else {
                painting = !walls[y][x];
                dragging = true;
            }
        } else if (input.inputPressed(ActionId.RIGHT_CLICK) && !(x == startX && y == startY)) {
            walls[y][x] = false;
            endX = x;
            endY = y;
            rebuild();
        }

        // Start and end cells are never walls
        if (dragging && walls[y][x] != painting && !(x == startX && y == startY) && !(x == endX && y == endY)) {
            walls[y][x] = painting;
            rebuild();
        }
    }

    private void drawRectangle(float x, float y, float width, float height, Color4f color) {
        rectangle.setColorMode(color);
        rectangle.setPosition(new Vector2f(x, y));
        rectangle.setSizePix(width, height);
        rectangle.display();
    }

    private void drawCell(int x, int y, float margin, Color4f color) {
        drawRectangle(x * CELL + margin, TOP + y * CELL + margin, CELL - 2 * margin, CELL - 2 * margin, color);
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        for (int y = 0; y < ROWS; ++y) {
            for (int x = 0; x < COLUMNS; ++x)
                drawCell(x, y, 1, walls[y][x] ? WALL : FLOOR);
        }

        // The path : a thin bar between two consecutive nodes (the links of the grid are horizontal or vertical)
        float bar = 8f;
        for (int i = 0; i + 1 < path.size(); ++i) {
            Vector2f a = path.get(i).getPosition();
            Vector2f b = path.get(i + 1).getPosition();
            drawRectangle(Math.min(a.x, b.x) - bar / 2f, Math.min(a.y, b.y) - bar / 2f,
                    Math.abs(a.x - b.x) + bar, Math.abs(a.y - b.y) + bar, PATH);
        }

        drawCell(startX, startY, 6, START);
        drawCell(endX, endY, 6, END);

        info.display();
        result.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        if (rectangle == null)
            return;

        rectangle.unload();
        info.unload();
        result.unload();
    }
}
