package sample;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Stream;

//added for setRandomWord()


public class Game {
    public int difficultyLevel = 3; //starting difficulty (word length)
    private int maxDifficulty = 12; //max difficulty (word length)
    private final int PARTS = 5;

    private String answer;
    public String tmpAnswer;
    public String[] letterAndPosArray;
    private String missedLetters;

    private int badGuessCount = 0;
    public int index = 0;
    private int score = 0;

    private final ReadOnlyObjectWrapper<GameStatus> gameStatus;

    private ObjectProperty<Boolean> gameState = new ReadOnlyObjectWrapper<Boolean>();
    private Line body = new Line(); //body line
    private Line[] partArray = new Line[PARTS];

    // TODO: Scoreboard
    private int wonCount;
    private int lostCount;

    @FXML
    private Pane board;

    @FXML
    private TilePane badGuess;

    @FXML
    private TextField textField ;

    public enum GameStatus {
        GAME_OVER {
            @Override
            public String toString() {
                return "Game over!";
            }
        },
        BAD_GUESS {
            @Override
            public String toString() {
                return "Bad guess...";
            }
        },
        REPEATED_WORD{
            @Override
            public String toString(){
                return "Try Again...";
            }
        },
        GOOD_GUESS {
            @Override
            public String toString() {
                return "Good guess!";
            }
        },
        WON {
            @Override
            public String toString() {
                return "You won!";
            }
        },
        OPEN {
            @Override
            public String toString() {
                return "Game on, let's go!";
            }
        }
    }

    public Game() {
        gameStatus = new ReadOnlyObjectWrapper<GameStatus>(this, "gameStatus", GameStatus.OPEN);
        gameStatus.addListener(new ChangeListener<GameStatus>() {
            @Override
            public void changed(ObservableValue<? extends GameStatus> observable,
                                GameStatus oldValue, GameStatus newValue) {

                if (gameStatus.get() == GameStatus.WON) {
                    //Victory Smile
                    GameController.emoteArray[4].setVisible(true);

                    // Increment wonCount
                    ++wonCount;
                    GameController.wonCount.setText(String.valueOf(wonCount));

                    log("in Game: in  WON");
                } else if (gameStatus.get() == GameStatus.GAME_OVER) {
                    // Changes Alive Face to Dead Face
                    GameController.emoteArray[0].setVisible(false);
                    GameController.emoteArray[1].setVisible(false);

                    GameController.emoteArray[2].setVisible(true);
                    GameController.emoteArray[3].setVisible(true);
                    for(int i = 0; i < answer.length(); i++)
                        GameController.text[i].setVisible(true);

                    // Increment Lost Count
                    ++lostCount;
                    GameController.lostCount.setText(String.valueOf(lostCount));

                    log("in Game: in GAME OVER");
                    textField.setEditable(false);
                } else if (gameStatus.get() == GameStatus.OPEN) {
                    log("in Game: in OPEN");
                    textField.setEditable(true);
                }
            }
        });

        setRandomWord();
        prepTmpAnswer();
        prepLetterAndPosArray();

        badGuessCount = 0;
        gameState.setValue(false); // initial state
        missedLetters = "";
        //score = 0;
        createGameStatusBinding();
    }

    public void createGameStatusBinding() {
        ObjectBinding<GameStatus> gameStatusBinding = new ObjectBinding<GameStatus>() {
            {
                super.bind(gameState);
            }
            @Override
            public GameStatus computeValue() {
                log("in computeValue");
                GameStatus check = checkForWinner(index);

                if(check != null ) {
                    return check;
                }

                if(tmpAnswer.trim().length() == 0 && index == 0){
                    log("new game");
                    return GameStatus.OPEN;
                }

                else if (index != -1 && index != -2){
                    log("good guess");
                    return GameStatus.GOOD_GUESS;
                }

                else if(index == -2){
                    log("Repeated Word");
                    return GameStatus.REPEATED_WORD;
                }

                else {
                    log("bad guess");
                    //Check if player loses
                    check = checkForWinner(index);

                    if(check != null ) {
                        return check;
                    }

                    return GameStatus.BAD_GUESS;
                }
            }
        };
        gameStatus.bind(gameStatusBinding);
    }

    public ReadOnlyObjectProperty<GameStatus> gameStatusProperty() {
        return gameStatus.getReadOnlyProperty();
    }

    public GameStatus getGameStatus() {
        return gameStatus.get();
    }

    private void setRandomWord() { //selects a random word from a dictionary and saves it into answer
        String line = "";

        try{
            //reads the file dictionary_full.txt
            BufferedReader br = new BufferedReader(new FileReader(new File("words.txt").getAbsolutePath()));
            int n = 0;

            //reads the number of lines
            while(br.readLine() != null) n++;
            br.close();

            boolean continueSearching = true;

            //Searches for the word to match the difficulty level
            while(continueSearching){
                line = getRandomWordFromDictionary(n);
                if((difficultyLevel == maxDifficulty && line.length() >= maxDifficulty) || line.length() == difficultyLevel){
                    continueSearching = false;
                }
            }

            log("current answer = " + answer);

        }catch(Exception e){
            e.printStackTrace();
        }
        answer = line;
    }

    //returns a random word from the dictionary
    private String getRandomWordFromDictionary(int n){
        String line;
        //Selects a random line
        int getLine = (int) (Math.random() * n);
        try{
            Path p = Paths.get("words.txt");
            //selects the line
            try(Stream<String> lines = Files.lines(p)){
                line = lines.skip(getLine).findFirst().get();
                //log(line);

                return line.toLowerCase();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private void prepTmpAnswer() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < answer.length(); i++) {
            sb.append(" ");
        }
        tmpAnswer = sb.toString();
    }

    private void prepLetterAndPosArray() {
        letterAndPosArray = new String[answer.length()];
        for(int i = 0; i < answer.length(); i++) {
            letterAndPosArray[i] = answer.substring(i,i+1);
        }
    }

    public boolean getValidIndex(String input) {
        boolean good = false;
        for (int i = 0; i < answer.length(); i++) {
            //Checking if letter appears in each position of the word
            if (input.equalsIgnoreCase(answer.substring(i, i + 1))) {
                //Letter was found, reveal letter
                GameController.text[i].setVisible(true);
                //User guess was good
                good = true;

                //Updating tmpAnswer
                StringBuilder sb = new StringBuilder(tmpAnswer);
                sb.setCharAt(i, input.charAt(0));
                tmpAnswer = sb.toString();
            }
        }

        //Checks if hint knows you have used a letter already
        for(int i = 0; i < letterAndPosArray.length; i++) {
            if(letterAndPosArray[i].equals(input)) {
                letterAndPosArray[i] = "";
            }
        }

        return good;
    }

    //Checks if letter has been repeated
    private boolean checkForRepeats(String input)
    {
        //Second+ guess
        if(missedLetters.length() > 0)
        {
            //Check if user has already guessed the letter
            if (missedLetters.contains(input)) {
                return true;
            }
            //New bad guess
            else {
                missedLetters = missedLetters.concat(", " + input);
            }
        }
        //First guess, since we already know it was a bad one
        else {
            missedLetters = missedLetters.concat(input);
        }
        return false;
    }

    //Does necessary updates to game
    public int update(String input) {
        boolean good = getValidIndex(input);

        if (!good) {
            if(!checkForRepeats(input)) {
                //Draw a hangman part
                GameController.partArray[badGuessCount].setVisible(true);
                //Decreases # of guesses
                badGuessCount++;
                return index = -1;
            }
            //This sets "Try Again" in GameStatus
            else
                return index = -2;
        }
        return index;
    }

    public void makeMove(String letter) {
        log("\nin makeMove: " + letter);
        index = update(letter);
        //This will toggle the state of the game
        gameState.setValue(!gameState.getValue());
    }

    public void reset() {
        //Increment the difficulty level
        if (difficultyLevel < 12)
            difficultyLevel++;

        log("\nin reset");
        //Get a new random word ready
        setRandomWord();
        prepTmpAnswer();
        prepLetterAndPosArray();

        //Resetting hangman parts, answer, and missed letters
        for(int i = 0; i < 5; i++) {
            GameController.partArray[i].setVisible(false);
        }

        for(int i = 0; i < GameController.text.length; i++) {
            GameController.text[i].setVisible(false);
        }

        //Then reset the badGuessCount and others
        badGuessCount = 0;
        index = 0;
        missedLetters = "";
        //prepTmpAnswer();
        gameState.setValue(false);

        //Resetting emote back to alive
        GameController.emoteArray[0].setVisible(true);
        GameController.emoteArray[1].setVisible(true);
        GameController.emoteArray[2].setVisible(false);
        GameController.emoteArray[3].setVisible(false);
        GameController.emoteArray[4].setVisible(false);

        createGameStatusBinding();
    }

    private int numOfTries() {
        return 5;
    }

    public String getAnswer() {
        return answer;
    }

    public int getBadGuessCount() {
        return badGuessCount;
    }

    public int getDifficulty() {
        return difficultyLevel;
    }

    public static void log(String s) {
        System.out.println(s);
    }

    private GameStatus checkForWinner(int status) {
        if(GameController.text == null)
            return GameStatus.OPEN;

        boolean solved = true;
        for (int i = 0; i < answer.length(); i++) {
            if (!GameController.text[i].isVisible()) {
                solved = false;
                //No need to check further
                break;
            }
        }

        if(solved){
            log("won");
            return GameStatus.WON;
        }

        else if(badGuessCount == numOfTries()) {
            log("game over");
            return GameStatus.GAME_OVER;
        }

        else {
            return null;
        }
    }

    public void setBoard(Pane input) {
        board = input;
    }

    public void setTextField(TextField input) {
        textField = input;
    }

    public String getMissedLetters()
    {
        return missedLetters;
    }

    public int getWonCount(){ return wonCount; }

    public int getLostCount(){ return lostCount; }

    public int getScore(){return score;}

}