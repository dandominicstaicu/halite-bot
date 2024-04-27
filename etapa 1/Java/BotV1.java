import java.io.IOException;

public class BotV1 {
    private BotExecutor botExecutor;
    private GameContext gameContext;

    public BotV1(GameStrategy gameStrategy) {
        this.botExecutor = new BotExecutor(gameStrategy);
    }

    public void gameLoop() throws IOException {

        final InitPackage iPackage = Networking.getInit();
        gameContext = new GameContext(iPackage.map, iPackage.myID);

        Networking.sendInit("Chess.com");

        while (true) {
            Networking.updateFrame(gameContext.gameMap);
            botExecutor.run(gameContext);
        }
    }

    public static void main(String[] args) throws IOException {
        BotV2 bot = new BotV2(new StageOneStrategy());
        bot.gameLoop();
    }
}  
