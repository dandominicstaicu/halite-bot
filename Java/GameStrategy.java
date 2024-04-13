import java.util.List;

public interface GameStrategy {
    List<Move> computeBestMoves(GameContext gameContext);
}
