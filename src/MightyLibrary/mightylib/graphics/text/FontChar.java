package MightyLibrary.mightylib.graphics.text;

public class FontChar {
    private int id;

    private double xAtlas, yAtlas;

    private double width, height;

    private double widthAtlas, heightAtlas;

    private double xOffset, yOffset;

    private double xAdvance;

    public int getId() {
        return id;
    }

    public FontChar setId(int id) {
        this.id = id;

        return this;
    }

    public double getxAtlas() {
        return xAtlas;
    }

    public FontChar setxAtlas(double xAtlas) {
        this.xAtlas = xAtlas;

        return this;
    }

    public double getyAtlas() {
        return yAtlas;
    }

    public FontChar setyAtlas(double yAtlas) {
        this.yAtlas = yAtlas;

        return this;
    }

    public double getWidth() {
        return width;
    }

    public FontChar setWidth(double width) {
        this.width = width;

        return this;
    }

    public double getHeight() {
        return height;
    }

    public FontChar setHeight(double height) {
        this.height = height;

        return this;
    }

    public double getWidthAtlas() {
        return widthAtlas;
    }

    public FontChar setWidthAtlas(double widthAtlas) {
        this.widthAtlas = widthAtlas;

        return this;
    }

    public double getHeightAtlas() {
        return heightAtlas;
    }

    public FontChar setHeightAtlas(double heightAtlas) {
        this.heightAtlas = heightAtlas;

        return this;
    }

    public double getxOffset() {
        return xOffset;
    }

    public FontChar setxOffset(double xOffset) {
        this.xOffset = xOffset;

        return this;
    }

    public double getyOffset() {
        return yOffset;
    }

    public FontChar setyOffset(double yOffset) {
        this.yOffset = yOffset;

        return this;
    }

    public double getxAdvance() {
        return xAdvance;
    }

    public FontChar setxAdvance(double xAdvance) {
        this.xAdvance = xAdvance;

        return this;
    }
}
