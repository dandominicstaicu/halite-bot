import java.util.*;

public abstract class GameStrategy {
    protected GameMap gameMap;
    protected int myID;
    protected List<Move> moves;

    public GameStrategy() {

    }
    
    public abstract List<Move> computeBestMoves(GameContext gameContext);

    protected  boolean isNeighbour(Location location) {
        for (Direction dir : Direction.DIRECTIONS) {
            Location newLocation = gameMap.getLocation(location, dir);
            if (newLocation.getSite().owner == myID) {
                return true;
            }
        }
        return false;
    }

    protected List<Location> getNeighbours(Location location) {
        List<Location> neighbours = new ArrayList<>();
        for (Direction dir : Direction.DIRECTIONS) {
            Location newLocation = gameMap.getLocation(location, dir);
            if (newLocation.getSite().owner == myID) {
                neighbours.add(newLocation);
            }
        }
        return neighbours;
    }

    protected  boolean isInnerLoc(int x, int y) {
        Location location = gameMap.getLocation(x, y);
        Site site = location.getSite();

        List<Location> neighbours = new ArrayList<>();
        for (Direction dir : Direction.DIRECTIONS) {
            Location newLocation = gameMap.getLocation(location, dir);
            neighbours.add(newLocation);
        }

        for (Location neighbour : neighbours) {
            if (neighbour.getSite().owner != myID) {
                return false;
            }
        }

        return true;
    }

    protected  Location findFarthestBoundary(Location start, Direction direction, int limit) {
        int distance = 0;
        Location current = start;
        while (current.getSite().owner == start.getSite().owner && distance < limit) {
            current = gameMap.getLocation(current, direction);
            distance++;
        }
        return current;
    }
}
