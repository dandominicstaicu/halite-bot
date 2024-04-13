import java.util.List;

public class BotExecutor {
    private final GameStrategy gameStrategy;

    public BotExecutor(GameStrategy gameStrategy) {
        this.gameStrategy = gameStrategy;
    }

    public void run(GameContext gameContext) {
        List<Move> moves = gameStrategy.computeBestMoves(gameContext);
        Networking.sendFrame(moves);
    }
}
