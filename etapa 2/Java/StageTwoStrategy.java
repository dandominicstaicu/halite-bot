import java.util.*;

public class StageTwoStrategy extends GameStrategy {
    private static final double ATTACK_BONUS = 20;
    // owned locations on the map ordered by strength -> stronger pieces will be moved first
    private PriorityQueue<Location> ownedLocations;
    // territories around the map to be processed
    private PriorityQueue<Territory> strategicTerritories;
    // map between a location and how much power was added to it by a previous move
    private Map<Location, Double> movePlan;
    // key = location, value = score of the location based on the neighbours and the location itself
    private Map<Location, Double> locationScoreMap;
    // unique Random instance
    private static final Random rand = new Random();
    // max count of turns
    private int turnsLeft = 400;
    // how many turns left are considered endgame
    private static final int ENDGAME_THRESHOLD = 100;
    // factor to decrease the score, so it's more attractive to attack
    private static final double ENDGAME_FACTOR = 0.5;

    // class used to keep track of territories and how they are scored
    public static class Territory implements Comparable<Territory> {
        public double score;
        public Location location;
        // all owned frontier locations have friendlyDistance = 1
        // the deeper the friendly location, the higher the distance
        public int friendlyDistance;

        public Territory(Location location) {
            this.location = location;
            this.score = 0;
            this.friendlyDistance = 0;
        }

        @Override
        public int compareTo(Territory o) {
            return Double.compare(this.score, o.score);
        }
    }

    // class used to keep track of possible moves
    public static class MoveCandidate implements Comparable<MoveCandidate> {
        // location on game map for the future move
        public Location location;
        // direction towards the location
        public Direction direction;
        // score of the location
        public double score;

        public MoveCandidate(Location location, Direction dir) {
            this.location = location;
            this.direction = dir;
            this.score = 0;
        }

        @Override
        public int compareTo(MoveCandidate o) {
            int scoreComparison = Double.compare(this.score, o.score);
            return scoreComparison != 0 ? scoreComparison : rand.nextInt(2) - 1;
        }
    }

    private static final double INFINITY = Double.MAX_VALUE;
    private static final int MAX_HALITE = 255;
    private static final double SCORE_DISTRIBUTION = 0.5;

    @Override
    public List<Move> computeBestMoves(GameContext gameContext) {
        --turnsLeft;

        gameMap = gameContext.gameMap;
        myID = gameContext.myID;

        // set up
        moves = new ArrayList<>();
        ownedLocations = new PriorityQueue<Location>((a, b) -> -Double.compare(a.getSite().strength, b.getSite().strength));
        locationScoreMap = new HashMap<>();
        strategicTerritories = new PriorityQueue<Territory>();
        movePlan = new HashMap<>();


        initialize();
        computeScores();

        // move all owned locations starting from stronger to weaker
        while (!ownedLocations.isEmpty()) {
            Location location = ownedLocations.poll();
            Direction moveDir = assignMove(location);
            Location target = gameMap.getLocation(location, moveDir);
            // we save our move in the move location plan
            movePlan.put(target, movePlan.getOrDefault(target, 0.0) + location.getSite().strength);
        }
        return moves;
    }


    private void computeScores() {
        while (locationScoreMap.size() < gameMap.width * gameMap.height) {
            Territory territory = strategicTerritories.poll();

            assert territory != null;
            if (!locationScoreMap.containsKey(territory.location)) {
                updateScoreMap(territory);
                analyzeNeighbours(territory);
            }
        }
    }

    private void updateScoreMap(Territory territory) {
        // Incorporate friendly distance into the score and update the map
        double updatedScore = territory.score + territory.friendlyDistance;
        locationScoreMap.put(territory.location, updatedScore);
    }

    private void analyzeNeighbours(Territory territory) {
        for (Location neighbour : getNeighbours(territory.location)) {
            int owner = neighbour.getSite().owner;
            Ownership ownership = Ownership.findOwnership(owner, myID);
            Territory neighbourTerritory = createNeighbourTerritory(territory, neighbour, ownership);

            strategicTerritories.add(neighbourTerritory);
        }
    }

    private Territory createNeighbourTerritory(Territory territory, Location neighbour, Ownership ownership) {
        Territory neighbourTerritory = new Territory(neighbour);
        int production = neighbour.getSite().production;

        switch (ownership) {
            case FRIENDLY:
                neighbourTerritory.score = territory.score + territory.friendlyDistance + 1;
                neighbourTerritory.friendlyDistance = territory.friendlyDistance + 1;
                break;

            case ENEMY:
                neighbourTerritory.score = INFINITY;
                neighbourTerritory.friendlyDistance = territory.friendlyDistance;
                break;

            case NEUTRAL:
                neighbourTerritory.score = (production == 0) ? INFINITY : weightedScore(neighbour, territory.score);
                neighbourTerritory.friendlyDistance = territory.friendlyDistance;
                break;

            default:
                throw new IllegalArgumentException("Invalid ownership");
        }
        return neighbourTerritory;
    }

    // we modify the score of a location based on the score of its neighbour
    private double weightedScore(Location loc, double neighScore) {
        return (1 - SCORE_DISTRIBUTION) * getScore(loc) + SCORE_DISTRIBUTION * neighScore;
    }

    // computes the score of a location
    private double getScore(Location location) {
        if (location.getSite().production == 0) {
            return INFINITY;
        } else {
            return 1.0 * location.getSite().strength / location.getSite().production + 1;
        }
    }

    // all owned locations are added to a set, and all others to the heap for further processing
    private void initialize() {
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                if (gameMap.getLocation(x, y).getSite().owner == myID) {
                    ownedLocations.add(gameMap.getLocation(x, y));
                } else {
                    Territory territory = new Territory(gameMap.getLocation(x, y));
                    territory.score = getScore(territory.location);
                    strategicTerritories.add(territory);
                }
            }

        }
    }

    private Direction assignMove(Location myLocation) {
        // if location's strength + production + strength assigned to it by other moves
        // is over the limit, we must move to not waste halite
        Site mySite = myLocation.getSite();

        boolean isMoveNeeded = (mySite.strength + mySite.production
                + movePlan.getOrDefault(myLocation, 0.0) > MAX_HALITE);

        // find all possible moves and their score
        List<MoveCandidate> moveCandidates = findCandidates(myLocation);
        // sort by score to find the best move
        Collections.sort(moveCandidates);
        MoveCandidate bestMove = moveCandidates.get(0);

        // if best move is INFINITY, it means all possible moves are bad choices
        if (bestMove.score == INFINITY) {
            if (isMoveNeeded) {
                // move towards the area with the lowest strength
                MoveCandidate safestMove = Collections.min(moveCandidates, Comparator.comparing(mc -> mc.location.getSite().strength));
                moves.add(new Move(myLocation, safestMove.direction));
                return safestMove.direction;

            } else {
                // stay still
                moves.add(new Move(myLocation, Direction.STILL));
                return Direction.STILL;
            }
        }

        // if we need to move, we move to the best direction
        if (isMoveNeeded) {
            moves.add(new Move(myLocation, bestMove.direction));
            return bestMove.direction;
        }

        // if no moves are needed and for another piece it was beneficial to
        // move to this location, we stay STILL
        if (isPartOfThePlan(myLocation)) {
            moves.add(new Move(myLocation, Direction.STILL));
            return Direction.STILL;
        }

        // we do not necessarily need to move, however attack if possible
        Site bestMoveSite = bestMove.location.getSite();

        // if our best option is an opponent attack if it can be conquered
        if (isAttackOpportunity(bestMoveSite, mySite)) {
            moves.add(new Move(myLocation, bestMove.direction));
            return bestMove.direction;
        }

        // otherwise, move if strong enough
        if (isStrongEnoughToMove(mySite)) {
            moves.add(new Move(myLocation, bestMove.direction));
            return bestMove.direction;
        }

        // if no condition for moving is satisfied, stay STILL
        moves.add(new Move(myLocation, Direction.STILL));
        return Direction.STILL;
    }

    private List<MoveCandidate> findCandidates(Location myLocation) {
        List<MoveCandidate> moveCandidates = new ArrayList<>();

        // check all neighbours
        for (Direction dir : Direction.CARDINALS) {
            Location neighbour = gameMap.getLocation(myLocation, dir);
            MoveCandidate moveCandidate = new MoveCandidate(neighbour, dir);

            // AVOID COLLISIONS and waste of halite
            if (isEndgame()) {
                // Calculate a more dynamic score based on endgame strategy
                // Higher scores for attacking weak enemies or closing gaps
                moveCandidate.score = calculateEndgameScore(neighbour, myLocation);
            } else if (movePlan.getOrDefault(neighbour, 0.0) + myLocation.getSite().strength > MAX_HALITE) {
                moveCandidate.score = INFINITY;
            } else {
                moveCandidate.score = locationScoreMap.get(neighbour);
            }

            moveCandidates.add(moveCandidate);
        }

        return moveCandidates;
    }

    private boolean isEndgame() {
        return turnsLeft < ENDGAME_THRESHOLD;
    }

    private double calculateEndgameScore(Location neighbour, Location myLocation) {
        double baseScore = locationScoreMap.getOrDefault(neighbour, 0.0);
        Site neighbourSite = neighbour.getSite();

        if (neighbourSite.owner == 0 || neighbourSite.strength < myLocation.getSite().strength) {
            // Prioritize attacking or filling gaps
            return baseScore * ENDGAME_FACTOR; // Decrease score to make these moves more attractive
        }
        return baseScore;
    }

    private boolean isPartOfThePlan(Location loc) {
        return movePlan.getOrDefault(loc, 0.0) > 0.0;
    }

    private boolean isAttackOpportunity(Site targetSite, Site mySite) {
        return targetSite.owner != myID &&
                (mySite.strength == MAX_HALITE || mySite.strength > targetSite.strength);
    }

    private boolean isStrongEnoughToMove(Site mySite) {
        return mySite.strength >= 6 * mySite.production;
    }
}