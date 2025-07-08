package app.model;

import java.util.Objects;

/**
 * Encapsula un movimiento en la intersección: entrada + giro.
 */
public class Movement {
    public enum Entry  { NORTE, ESTE, SUR, OESTE }
    public enum Turn   { STRAIGHT, LEFT, RIGHT, U_TURN }

    private final Entry entry;
    private final Turn  turn;

    public Movement(Entry entry, Turn turn) {
        this.entry = entry;
        this.turn  = turn;
    }

    public Entry getEntry() { return entry; }
    public Turn  getTurn()  { return turn;  }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Movement)) return false;
        Movement that = (Movement)o;
        return entry == that.entry && turn == that.turn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entry, turn);
    }

    @Override
    public String toString() {
        return entry + "→" + turn;
    }
}
