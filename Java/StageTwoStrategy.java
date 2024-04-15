import java.util.*;


public class StageTwoStrategy extends GameStrategy {
    private List<Territory> territories;
    private PriorityQueue<Territory> strategicTerritories;
    private static final double INFINITY = Double.MAX_VALUE;
    private static final int NEUTRAL_ID = 0;
    public class Territory {
        protected double initalScore;
        protected double finalScore;
        protected Location location;
        protected int distanceTo;
        protected Ownership ownership;

        public Territory(Location location) {
            this.location = location;
            this.initalScore = 0;
            this.finalScore = 0;
            this.distanceTo = 0;
            this.ownership = setOwnership(location.getSite().owner);
        }

        private Ownership setOwnership(int owner) {
            if (location.getSite().owner == myID) {
                return Ownership.FRIENDLY;
            }

            if (location.getSite().owner == NEUTRAL_ID) {
                return Ownership.NEUTRAL;
            }

            return Ownership.ENEMY;
        }

        public double getInitalScore() {
            return initalScore;
        }

        public void setInitalScore(double initalScore) {
            this.initalScore = initalScore;
        }

        public double getFinalScore() {
            return finalScore;
        }

        public void setFinalScore(double finalScore) {
            this.finalScore = finalScore;
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public int getDistanceTo() {
            return distanceTo;
        }

        public void setDistanceTo(int distanceTo) {
            this.distanceTo = distanceTo;
        }

    }

    // possible improvements for neutral territories
    private double getScore(Location location) {
        if (location.getSite().production == 0) {
            return INFINITY;
        } else {
            return 1.0 * location.getSite().strength / location.getSite().production + 1;
        }
    }

    private void initializeTerritories() {
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Territory territory = new Territory(gameMap.getLocation(x, y));
                territory.setInitalScore(getScore(territory.getLocation()));
                territories.add(territory);
            }
            
        }
    }

    @Override
    public List<Move> computeBestMoves(GameContext gameContext) {
        gameMap = gameContext.gameMap;
        myID = gameContext.myID;
        moves = new ArrayList<>();
        territories = new ArrayList<>();
        strategicTerritories = new PriorityQueue<Territory>((a,b) -> Double.compare(a.getInitalScore(), b.getInitalScore()));

        initializeTerritories();
        for (Territory territory : territories) {
            strategicTerritories.add(territory);
        }

    }

}
