import java.util.Random;

public enum Direction {
    STILL, NORTH, EAST, SOUTH, WEST;

    public static final Direction[] DIRECTIONS = new Direction[]{STILL, NORTH, EAST, SOUTH, WEST};
    public static final Direction[] CARDINALS = new Direction[]{NORTH, EAST, SOUTH, WEST};

    public static Direction randomDirection() {
        Direction[] values = values();
        return values[new Random().nextInt(values.length)];
    }

    public static Direction invertDirection(Direction direction) {
        switch (direction) {
            case NORTH:
                return SOUTH;
            case EAST:
                return WEST;
            case SOUTH:
                return NORTH;
            case WEST:
                return EAST;
            default:
                return STILL;
        }
    }

    public static Direction[] getEastWestDirections() {
        return new Direction[]{EAST, WEST};
    }

    public static Direction[] getNorthSouthDirections() {
        return new Direction[]{NORTH, SOUTH};
    }
}
