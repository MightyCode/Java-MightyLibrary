package MightyLibrary.mightylib.resources.map;

public class TilePosition implements Comparable<TilePosition>, Cloneable {
    private int x, y, layer;

    public TilePosition(int x, int y, int layer){
        this.x = x;
        this.y = y;
        this.layer = layer;
    }

    public TilePosition(TilePosition other){
        this.x = other.x;
        this.y = other.y;
        this.layer = other.layer;
    }

    public TilePosition(){
        this(0, 0, 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TilePosition){
            TilePosition other = (TilePosition) obj;

            return other.x == x && other.y == y && other.layer == layer;
        }

        return false;
    }

    @Override
    public int compareTo(TilePosition other) {
        if (other.layer == layer){
            if (other.y == y)
                return x - other.x;

            return y - other.y;
        }

        return layer - other.layer;
    }

    public int x(){
        return x;
    }

    public int y(){
        return y;
    }

    public int layer(){
        return layer;
    }

    public TilePosition setX(int x){
        this.x = x;

        return this;
    }

    public TilePosition setY(int y){
        this.y = y;

        return this;
    }

    public TilePosition setLayer(int layer){
        this.layer = layer;

        return this;
    }

    @Override
    public TilePosition clone() {
        try {
            TilePosition clone = (TilePosition) super.clone();
            clone.setX(x).setY(y).setLayer(layer);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
