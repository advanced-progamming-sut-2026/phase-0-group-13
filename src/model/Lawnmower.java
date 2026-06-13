package model;

public class Lawnmower {
    private int row;
    private boolean isActive;

    public Lawnmower(int row) {
        this.row = row;
        this.isActive = true;
    }

    public void trigger() {
    }

    public void move() {
    }

    public int getRow() { return row; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
