package MightyLibrary.mightylib.graphics.text;

public class FontChar {
    private int id;

    private float xAtlas, yAtlas;

    private float width, height;

    private float widthAtlas, heightAtlas;

    private float xOffset, yOffset;

    private float xAdvance;

    public int getId() {
        return id;
    }

    public FontChar setId(int id) {
        this.id = id;

        return this;
    }

    public float getxAtlas() {
        return xAtlas;
    }

    public FontChar setxAtlas(float xAtlas) {
        this.xAtlas = xAtlas;

        return this;
    }

    public float getyAtlas() {
        return yAtlas;
    }

    public FontChar setyAtlas(float yAtlas) {
        this.yAtlas = yAtlas;

        return this;
    }

    public float getWidth() {
        return width;
    }

    public FontChar setWidth(float width) {
        this.width = width;

        return this;
    }

    public float getHeight() {
        return height;
    }

    public FontChar setHeight(float height) {
        this.height = height;

        return this;
    }

    public float getWidthAtlas() {
        return widthAtlas;
    }

    public FontChar setWidthAtlas(float widthAtlas) {
        this.widthAtlas = widthAtlas;

        return this;
    }

    public float getHeightAtlas() {
        return heightAtlas;
    }

    public FontChar setHeightAtlas(float heightAtlas) {
        this.heightAtlas = heightAtlas;

        return this;
    }

    public float getxOffset() {
        return xOffset;
    }

    public FontChar setxOffset(float xOffset) {
        this.xOffset = xOffset;

        return this;
    }

    public float getyOffset() {
        return yOffset;
    }

    public FontChar setyOffset(float yOffset) {
        this.yOffset = yOffset;

        return this;
    }

    public float getxAdvance() {
        return xAdvance;
    }

    public FontChar setxAdvance(float xAdvance) {
        this.xAdvance = xAdvance;

        return this;
    }
}
