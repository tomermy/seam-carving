package edu.cg;

public class MinimalCost {
    private long cost;
    private int parentXIndex;

    public MinimalCost(long cost, int parentXIndex) {
        this.cost = cost;
        this.parentXIndex = parentXIndex;
    }

    public long getCost() {
        return cost;
    }

    public int getParentXIndex() {
        return parentXIndex;
    }
}
