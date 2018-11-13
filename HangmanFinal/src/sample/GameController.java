package sample;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javafx.animation.*;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import static sample.Game.log;

public class GameController {

    private final ExecutorService executorService;
    private final Game game;

    int hintCount;

    public static Text wonCount;
    public static Text lostCount;

    public GameController(Game game) {
        this.game = game;
        hintCount = (game.difficultyLevel/4);
        executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    @FXML
    private Pane board = new Pane();
    @FXML
    private VBox topBox;
    @FXML
    private TilePane bg = new TilePane(Orientation.VERTICAL);
    @FXML
    private Label statusLabel ;
    @FXML
    private Label NumberOfBadGuesses ;
    @FXML
    private Label difficultyLabel ;
    @FXML
    private Label enterALetterLabel ;
    @FXML
    private Label MissedLettersLabel;
    @FXML
    private Label NumberOfMovesLabel;
    //@FXML
    //private TextField answerTF;
    @FXML
    private TextField textField ;
    //Adding HintButton
    @FXML
    private Button hintButton;
    @FXML
    private Label ScoreLabel;

    private Group root;
    private SequentialTransition swayTransition;

    public static Text[] text;
    private Line[] blanks;
    public static Line[] partArray = new Line[5];
    // Alive eyes, dead eyes, smile
    public static Node[] emoteArray = new Node[5];
    private Line body = new Line();

    public void initialize() throws IOException {
        System.out.println("in initialize");
        drawHangman();
        addTextBoxListener();
        addHintButtonListener();
        setUpStatusLabelBindings();
        game.setBoard(board);

        hintButton.setText("User Hint (" + hintCount + ")");

        game.setTextField(textField);
        setLines();
        setAnswer();
        setHangman();
        setAnimation();
    }

    //TODO: Added Hint Button
    private void addHintButtonListener(){
        hintButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                //Decrement hintCount
                --hintCount;
                hintButton.setText("User Hint (" + hintCount + ")");

                //setting hintButton off
                if(hintCount == 0) {
                    hintButton.setDisable(true);
                }
                log("HintButton Pressed");

                //Getting correct letter and displaying it
                for(int i = 0; i < game.letterAndPosArray.length; i++) {
                    if(!game.letterAndPosArray[i].equals("")){
                        game.index = i;
                        StringBuilder sb = new StringBuilder(game.tmpAnswer);
                        sb.setCharAt(game.index, game.letterAndPosArray[i].charAt(0));
                        game.tmpAnswer = sb.toString();
                        game.makeMove(game.letterAndPosArray[i]);
                        game.letterAndPosArray[i] = "";
                        break;
                    }
                }

            }
        });
    }

    private void addTextBoxListener() {
        textField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(final ObservableValue<? extends String> ov, final String oldValue, final String newValue) {
                if(newValue.length() > 0 && newValue.matches("[a-z]")) {
                    System.out.print(newValue);
                    game.makeMove(newValue);
                    textField.clear();
                    NumberOfBadGuesses.setText((5-game.getBadGuessCount()) + " guesses remaining.            ");
                    MissedLettersLabel.setText("Missed Letters: " + game.getMissedLetters());
                    //ScoreLabel.setText("Score: ");
                    if(game.getBadGuessCount()==5)
                        swayTransition.play();
                }
                else
                    textField.clear();
            }
        });
    }

    private void setUpStatusLabelBindings() {
        System.out.println("in setUpStatusLabelBindings");
        NumberOfBadGuesses.setText("You have " + (5-game.getBadGuessCount()) + " guesses left.            ");
        GridPane.setHalignment(difficultyLabel, HPos.RIGHT);
        difficultyLabel.setText("Level: " + (game.getDifficulty() - 2));
        statusLabel.textProperty().bind(Bindings.format("%s", game.gameStatusProperty()));
        enterALetterLabel.textProperty().bind(Bindings.format("%s", "Enter a letter:"));
        MissedLettersLabel.setText("Missed Letters: " + game.getMissedLetters());
    }

    private void setAnimation()
    {
        TranslateTransition translateRightTransition = new TranslateTransition();
        translateRightTransition.setDuration(Duration.millis(1000));
        translateRightTransition.setNode(root);
        translateRightTransition.setFromX(0);
        translateRightTransition.setByX(25);

        TranslateTransition translateLeftTransition = new TranslateTransition();
        translateLeftTransition.setDuration(Duration.millis(1000));
        translateLeftTransition.setNode(root);
        translateLeftTransition.setFromX(25);
        translateLeftTransition.setByX(-50);

        RotateTransition rotateLeft = new RotateTransition(Duration.millis(1000));
        rotateLeft.setByAngle(20);

        RotateTransition rotateRight = new RotateTransition(Duration.millis(1000));
        rotateRight.setByAngle(-10);

        ParallelTransition right = new ParallelTransition();
        right.getChildren().addAll(translateRightTransition, rotateRight);
        ParallelTransition left = new ParallelTransition();
        left.getChildren().addAll(translateLeftTransition, rotateLeft);

        swayTransition = new SequentialTransition(root, right, left);
        swayTransition.setCycleCount(Timeline.INDEFINITE);
        swayTransition.setAutoReverse(true);
    }

    //Initial Hangman drawing
    private void drawHangman() {
        //Creating our center positions for drawings
        double centerX = 200.0f;
        double centerY = 80.0f;

        Line topArm = new Line();
        topArm.setStartX(centerX - 20);
        topArm.setStartY(centerY - 50.0f);
        topArm.setEndX(centerX + 120.0f);
        topArm.setEndY(centerY - 50.0f);
        topArm.setStrokeWidth(10.0f);

        Line pole = new Line();
        pole.setStartX(centerX + 120.0f);
        pole.setStartY(centerY - 50.0f);
        pole.setEndX(centerX + 120.0f);
        pole.setEndY(centerY + 250.0f);
        pole.setStrokeWidth(10.0f);

        Line base = new Line();
        base.setStartX(centerX - 70.0f);
        base.setStartY(centerY + 250.0f);
        base.setEndX(centerX + 170.0f);
        base.setEndY(centerY + 250.0f);
        base.setStrokeWidth(10.0f);

        // TODO: Create scoreboard
        Color MED_BURGUNDY_BG = Color.web("#7D3549", 1);

        Rectangle scoreboard = new Rectangle();
        scoreboard.setLayoutX(370.0f);
        scoreboard.setLayoutY(-10.0f);
        scoreboard.setWidth(150.0f);
        scoreboard.setHeight(70.0f);
        scoreboard.setArcWidth(30.0);
        scoreboard.setArcHeight(20.0);
        scoreboard.setFill(MED_BURGUNDY_BG);

        Text wonText = new Text("Wins");
        wonText.setLayoutX(centerX + 190);
        wonText.setLayoutY(centerY - 70);
        wonText.setFont(Font.font("Arial Rounded MT Bold", 16));
        wonText.setFill(Color.WHITE);

        Text lostText = new Text("Losses");
        lostText.setLayoutX(centerX + 250);
        lostText.setLayoutY(centerY - 70);
        lostText.setFont(Font.font("Arial Rounded MT Bold", 16));
        lostText.setFill(Color.WHITE);

        wonCount = new Text(String.valueOf(game.getWonCount()));
        wonCount.setLayoutX(centerX + 190);
        wonCount.setLayoutY(centerY - 40);
        wonCount.setFont(Font.font("Arial Rounded MT Bold", 16));
        wonCount.setFill(Color.WHITE);

        lostCount = new Text(String.valueOf(game.getWonCount()));
        lostCount.setLayoutX(centerX + 250);
        lostCount.setLayoutY(centerY - 40);
        lostCount.setFont(Font.font("Arial Rounded MT Bold", 16));
        lostCount.setFill(Color.WHITE);


        //draw them all on the board
        board.getChildren().add(topArm);
        board.getChildren().add(pole);
        board.getChildren().add(base);

        board.getChildren().add(scoreboard);
        board.getChildren().add(wonText);
        board.getChildren().add(lostText);
        board.getChildren().add(wonCount);
        board.getChildren().add(lostCount);
    }

    //Draws blanks for # of letters in word
    private void setLines()
    {
        blanks = new Line[game.getAnswer().length()];
        int xStart = 200;
        int lineLength = 25;
        int lineSpacing = 35;
        for (int i = 0; i < blanks.length; i++) {
            //Calculate the starting point of the line
            int xcoord = xStart + (lineSpacing * i);
            //Create the line
            blanks[i] = new Line(xcoord, 225, xcoord - lineLength, 225);
            blanks[i].setStroke(Color.BLACK);
            blanks[i].setStrokeWidth(3);
            blanks[i].setLayoutY(170);
            board.getChildren().add(blanks[i]);
        }
    }

    //Texts that gets revealed once player gets a good guess
    private void setAnswer() {
        String answer = game.getAnswer();
        text = new Text[answer.length()];
        int xStartw = 180;
        int lineSpacingw = 35;
        for (int i = 0; i < text.length; i++) {
            //Calculate the starting point of the line
            int xcoordw = xStartw + (lineSpacingw * i);
            text[i] = new Text(answer.substring(i, i + 1));
            text[i].setFont(new Font(30));
            text[i].setX(xcoordw);
            text[i].setY(170);
            text[i].setLayoutY(215);
            text[i].setVisible(false);
            board.getChildren().add(text[i]);
        }
    }

    //Drawing the hangman parts that will get animated
    private void setHangman()
    {
        double centerX = 200.0f;
        double centerY = 80.0f;
        //Stroke width of the man
        double manStrokeWidth = 6.0f;

        Circle head = new Circle();
        head.setRadius(25);
        head.setCenterX(centerX);
        head.setCenterY(centerY);

        Line hook = new Line();
        hook.setStartX(centerX);
        hook.setStartY(centerY);
        hook.setEndX(centerX);
        hook.setEndY(centerY - 50.0f);
        hook.setStrokeWidth(5.0f);

        body.setStartX(centerX);
        body.setStartY(centerY);
        body.setEndX(centerX);
        body.setEndY(centerY + 120.0f);
        body.setStrokeWidth(manStrokeWidth);

        Line leftArm = new Line();
        leftArm.setStartX(centerX);
        leftArm.setStartY(centerY + 30.0f);
        leftArm.setEndX(centerX - 40.0f);
        leftArm.setEndY(centerY + 80.0f);
        leftArm.setStrokeWidth(manStrokeWidth);

        Line rightArm = new Line();
        rightArm.setStartX(centerX);
        rightArm.setStartY(centerY + 30.0f);
        rightArm.setEndX(centerX + 40.0f);
        rightArm.setEndY(centerY + 80.0f);
        rightArm.setStrokeWidth(manStrokeWidth);

        Line leftLeg = new Line();
        leftLeg.setStartX(centerX);
        leftLeg.setStartY(centerY + 120.0f);
        leftLeg.setEndX(centerX - 40.0f);
        leftLeg.setEndY(centerY + 200.0f);
        leftLeg.setStrokeWidth(manStrokeWidth);

        Line rightLeg = new Line();
        rightLeg.setStartX(centerX);
        rightLeg.setStartY(centerY + 120.0f);
        rightLeg.setEndX(centerX + 40.0f);
        rightLeg.setEndY(centerY + 200.0f);
        rightLeg.setStrokeWidth(manStrokeWidth);


        //Emote eyes and smile
        Circle leftAliveEye = new Circle();
        leftAliveEye.setRadius(4);
        leftAliveEye.setFill(Color.SKYBLUE);
        leftAliveEye.setCenterX(centerX - 7);
        leftAliveEye.setCenterY(centerY - 3);

        Circle rightAliveEye = new Circle();
        rightAliveEye.setRadius(4);
        rightAliveEye.setFill(Color.SKYBLUE);
        rightAliveEye.setCenterX(centerX + 7);
        rightAliveEye.setCenterY(centerY - 3);

        Text leftDeadEye = new Text("X");
        leftDeadEye.setFont(Font.font("Arial Rounded MT Bold", 15));
        leftDeadEye.setFill(Color.MAROON);
        leftDeadEye.setLayoutX(centerX - 12);
        leftDeadEye.setLayoutY(centerY + 2);

        Text rightDeadEye = new Text("X");
        rightDeadEye.setFont(Font.font("Arial Rounded MT Bold", 15));
        rightDeadEye.setFill(Color.MAROON);
        rightDeadEye.setLayoutX(centerX + 2);
        rightDeadEye.setLayoutY(centerY + 2 );

        Arc smile = new Arc();
        smile.setCenterX(centerX);
        smile.setCenterY(centerY - 3);
        smile.setType(ArcType.CHORD);
        smile.setRadiusX(18.0f);
        smile.setRadiusY(18.0f);
        smile.setStartAngle(227.0f);
        smile.setLength(80.0f);
        smile.setType(ArcType.CHORD);
        smile.setFill(Color.SALMON);


        //Put all the lines into an array so we can reference them later
        partArray[0] = body;
        partArray[1] = leftArm;
        partArray[2] = rightArm;
        partArray[3] = leftLeg;
        partArray[4] = rightLeg;

        //Setting parts to invisible for game
        partArray[0].setVisible(false);
        partArray[1].setVisible(false);
        partArray[2].setVisible(false);
        partArray[3].setVisible(false);
        partArray[4].setVisible(false);

        //Eyes and smile to emoteArray
        emoteArray[0] = leftAliveEye;
        emoteArray[1] = rightAliveEye;
        emoteArray[2] = leftDeadEye;
        emoteArray[3] = rightDeadEye;
        emoteArray[4] = smile;


        //Set alive eyes to visible and dead eyes to invisible
        emoteArray[0].setVisible(true);
        emoteArray[1].setVisible(true);
        emoteArray[2].setVisible(false);
        emoteArray[3].setVisible(false);
        emoteArray[4].setVisible(false);

        //Creating group of parts that will get animated
        root = new Group();
        root.getChildren().add(head);
        root.getChildren().add(hook);

        for(int i = 0; i < 5; i++) {
            root.getChildren().addAll(partArray[i], emoteArray[i]);
        }

        //Draw all parts to board
        board.getChildren().add(root);
    }

    @FXML
    private void newHangman() {
        game.reset();
        difficultyLabel.setText("Level: " + (game.getDifficulty() - 2) + "                    ");
        NumberOfBadGuesses.setText((5-game.getBadGuessCount()) + " guesses remaining.");
        MissedLettersLabel.setText("Missed Letters: " + game.getMissedLetters());
        //ScoreLabel.setText("Score: ");

        //Resetting hint count
        hintCount = (game.difficultyLevel / 4);
        hintButton.setDisable(false);
        hintButton.setText("Use Hint (" + hintCount + "}");

        //Stopping animation
        swayTransition.stop();

        //Redraw lines and answer
        board.getChildren().remove(text);
        board.getChildren().remove(blanks);
        setLines();
        setAnswer();
        //Redrawing parts and resetting animation node
        board.getChildren().remove(root);
        setHangman();
        setAnimation();
    }

    @FXML
    private void quit() {
        board.getScene().getWindow().hide();
    }
}