public class GameContext {
    public final GameMap gameMap;
    public final int myID;

    public GameContext(GameMap gameMap, int myID) {
        this.gameMap = gameMap;
        this.myID = myID;
    }
}
