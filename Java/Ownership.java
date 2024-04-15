public enum Ownership {
    NEUTRAL, FRIENDLY, ENEMY;
    public static Ownership findOwnership(int owner, int myID) {
        if (owner == myID) {
            return FRIENDLY;
        }
        if (owner == 0) {
            return NEUTRAL;
        }
        return ENEMY;
    }
}
