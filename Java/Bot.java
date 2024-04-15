import java.io.IOException;

public class Bot {
    private BotExecutor botExecutor;
    private GameContext gameContext;

    public Bot(GameStrategy gameStrategy) {
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
        // Bot bot = new Bot(new StageOneStrategy());
        Bot bot = new Bot(new StageTwoStrategy());
        bot.gameLoop();
    }
}  
