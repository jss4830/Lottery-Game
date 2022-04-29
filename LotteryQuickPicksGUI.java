package gui.lottonetworkgui;
/*
 * Name: Jesse Goldman
 * Date: 4/29/2022
 * Course Number: CSC-112-D01
 * Course Name: Java 2
 * Problem Number: Chapter 33
 * Email: Jgoldman2001@student.stcc.edu
 * Short Description of the Problem: connect QuickPicks gui to server
 */

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LotteryQuickPicksGUI extends Application {
    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(new AppGUI(), 900, 450);
        scene.getStylesheets().add("LotteryQuickPicks.css");
        primaryStage.setTitle("Lottery Quick Picks");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private class AppGUI extends BorderPane {
        public AppGUI() {
            var taResultsPane = new TextAreaPane();
            var infoPane = new InfoPane(taResultsPane);
            var aboutPane = new AboutPane();

            this.setTop(infoPane);
            this.setCenter(taResultsPane);
            this.setBottom(aboutPane);
        }
    }

    private class AboutPane extends HBox {
        public AboutPane() {
            this.setId("aboutPane");
            this.setAlignment(Pos.CENTER);
            this.getChildren().add(new Label("Lottery Qick-Pick System (Jesse Goldman)"));
        }
    }

    private class InfoPane extends HBox {
        private SliderNoOfQP noOfQPPane;
        private GameSelectPane gameSelectPane;
        private TextAreaPane textAreaPane;
        private TextField txtEmail;
        private CheckBox chkEmail;


        public InfoPane(TextAreaPane taResults) {
            this.setSpacing(50);
            this.setAlignment(Pos.CENTER);
            this.setPadding(new Insets(20));

            this.noOfQPPane = new SliderNoOfQP();
            this.gameSelectPane = new GameSelectPane();
            this.textAreaPane = taResults;

            var tmpVbox = new VBox(30);
            var tmpHBox1 = new HBox(50);
            tmpHBox1.setAlignment(Pos.CENTER);

            var btnGenerate = new Button("Generate");
            btnGenerate.setPrefWidth(100);
            btnGenerate.setOnAction(e -> sendInfoToServer());

            var btnReset = new Button("Reset");
            btnReset.setOnAction(e -> resetApp());
            tmpHBox1.getChildren().addAll(btnGenerate, btnReset);

            var tmpHBox2 = new HBox(20);
            txtEmail = new TextField();
            txtEmail.setDisable(true);
            chkEmail = new CheckBox("Email?");
            chkEmail.setSelected(false);
            chkEmail.setOnAction(e -> {
                txtEmail.setDisable(!chkEmail.isSelected());
            });
            tmpHBox2.getChildren().addAll(chkEmail, txtEmail);

            tmpVbox.getChildren().addAll(tmpHBox1, tmpHBox2);

            this.getChildren().addAll(noOfQPPane, gameSelectPane, tmpVbox);
        }

        private void resetApp() {
            this.noOfQPPane.reset();
            this.gameSelectPane.reset();
            this.textAreaPane.reset();
            this.chkEmail.setSelected(false);
            this.txtEmail.setText("");
        }

        private static boolean isEmailValid(String email) {
            final String regex = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
            final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(email);
            return matcher.find();
        }

        private void sendInfoToServer() {
            final String lottoRegex = "(PowerBall|MegaMillions|LuckyForLife)";

            var noOfQP = noOfQPPane.getNoOfQP();
            var game = gameSelectPane.selectedGame();

            try (Scanner sc = new Scanner(System.in);) {
                pStr("Creating Client Socket ");
                try (Socket client = new Socket("localhost", 8000); //connect to server
                     Scanner input = new Scanner(client.getInputStream());
                     PrintWriter output = new PrintWriter(client.getOutputStream());) {
                    pStr("Connected to Server!");

                    // send to server number of QuickPicks
                    if (noOfQP <= 10 && noOfQP > 0) {
                        output.println("" + noOfQP);
                        output.flush();
                        System.out.println("num QPICK sent to server: " + noOfQP);
                    } else {
                        this.textAreaPane.appendText("Invalid number of Quick Picks received!" + "\n");
                        input.close();
                    }

                    // send to server lotto type
                    assert game != null;
                    if (game.matches(lottoRegex)) {
                        output.println("" + game);
                        output.flush();
                        System.out.println("game type sent to server: " + game);
                    } else {
                        this.textAreaPane.appendText("invalid lottery type received!" + "\n");
                        input.close();
                    }

                    String ansStr = input.nextLine();
                    if (Integer.parseInt(ansStr) == -1) { //generates lottery picks from the server
                        this.textAreaPane.appendText("Quick Picks could not be generated from server");// send error message to textArea
                    } else {  //send the quick pick numbers from server to textArea
                        this.textAreaPane.appendText("\n" + ansStr + " " + game + "\n"); //prints lotto numbers to textArea
                        while (input.hasNextLine()) {

                            this.textAreaPane.appendText(input.nextLine() + "\n");
                        }
                    }

                    if (!chkEmail.isSelected()) { // send out the "email"
                        this.textAreaPane.appendText("No Email\n");
                    } else if (isEmailValid(txtEmail.getText())) {
                        this.textAreaPane.appendText("Email To: " + txtEmail.getText() + "\n");
                    } else {
                        this.textAreaPane.appendText("Email To: Illegal\n");
                    }

                }

            } catch (Exception e) {
                this.textAreaPane.appendText(e.getMessage() + "\n");
            }
        }
    }

    private static void pStr(String p) {
        System.out.println(p);
    }

    private class GameSelectPane extends VBox {
        private RadioButton rbPowerBall;
        private RadioButton rbMegaMillions;
        private RadioButton rbLuckyForLife;

        public GameSelectPane() {
            this.setSpacing(10);
            this.setAlignment(Pos.CENTER_LEFT);

            rbPowerBall = new RadioButton("PowerBall");
            rbMegaMillions = new RadioButton("MegaMillions");
            rbLuckyForLife = new RadioButton("LuckyForLife");

            ToggleGroup group = new ToggleGroup();
            rbPowerBall.setToggleGroup(group);
            rbMegaMillions.setToggleGroup(group);
            rbLuckyForLife.setToggleGroup(group);

            this.getChildren().addAll(rbPowerBall, rbMegaMillions, rbLuckyForLife);
        }

        public String selectedGame() {
            if (rbPowerBall.isSelected())
                return rbPowerBall.getText();
            if (rbMegaMillions.isSelected())
                return rbMegaMillions.getText();
            if (rbLuckyForLife.isSelected())
                return rbLuckyForLife.getText();
            return null;
        }

        public void reset() {
            rbPowerBall.setSelected(false);
            rbMegaMillions.setSelected(false);
            rbLuckyForLife.setSelected(false);
        }
    }

    private class SliderNoOfQP extends VBox {
        private Slider slider;

        public SliderNoOfQP() {
            this.setPrefWidth(300);
            this.setSpacing(10);
            this.setAlignment(Pos.CENTER);

            var lblNoOfQP = new Label("Number of Quick Picks");
            slider = new Slider();
            slider.setOrientation(Orientation.HORIZONTAL);
            slider.setMin(1);
            slider.setMax(10);
            slider.setShowTickLabels(true);
            slider.setShowTickMarks(true);
            slider.setMinorTickCount(0);
            slider.setMajorTickUnit(1);
            slider.setSnapToTicks(true);
            slider.setValue(1);

            this.getChildren().addAll(lblNoOfQP, slider);
        }

        public int getNoOfQP() {
            return (int) this.slider.getValue();
        }

        public void reset() {
            this.slider.setValue(1);
        }
    }

    private class TextAreaPane extends StackPane {
        private TextArea taResults;

        public TextAreaPane() {
            this.taResults = new TextArea();
            //this.taResults.setPrefSize(300, 200);
            this.taResults.setId("taResults");
            this.taResults.setEditable(false);
            this.taResults.setPadding(new Insets(10));
            this.getChildren().addAll(this.taResults);
        }

        @SuppressWarnings("unused")
        public String getText() {
            return this.taResults.getText();
        }

        public void appendText(String str) {
            this.taResults.appendText(str);
        }

        public void reset() {
            this.taResults.setText("");
        }

    }

}
