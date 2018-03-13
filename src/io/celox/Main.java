/*
 * Copyright (c) 2017 Martin Pfeffer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.celox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Main extends Application {

    private TextField mTextFieldDirPath;

    private int mNumberOfBuilds;
    private int mDurationTotal;
    private int mDurationLongest;
    private Label mLabelDurationLongest;
    private Label mLabelDurationAvg;
    private Label mLabelDurationTotal;
    private Label mLabelBuilds;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("GradleTimingInspector");
        BorderPane borderPane = new BorderPane();
        Scene scene = new Scene(borderPane, 300, 275);
        primaryStage.setScene(scene);
        primaryStage.show();

        borderPane.setPadding(new Insets(20, 20, 20, 20));

        setUpBorderPane(borderPane);
        borderPane.requestFocus();
    }

    private VBox makeTop() {
        VBox vBoxTop = new VBox(4);
        vBoxTop.setPadding(new Insets(0, 0, 10, 0));
        Label label = new Label("Select directory by drag & drop");
        mTextFieldDirPath = new TextField();
        mTextFieldDirPath.setPromptText("e.g. 'your/project/app/buildings'");

        mTextFieldDirPath.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.ANY);
            }
            event.consume();
        });
        mTextFieldDirPath.setOnDragDropped(event -> {
            List<File> files = event.getDragboard().getFiles();
            for (File f : files) {
                if (f.isDirectory()) {
                    mTextFieldDirPath.setText(f.getPath());
                    return;
                }
            }
            event.consume();
        });
        vBoxTop.getChildren().addAll(label, mTextFieldDirPath);
        return vBoxTop;
    }

    private VBox makeCenter() {
        VBox vBox = new VBox(4);
        Label label = new Label("INFO: ");
        mLabelBuilds = new Label();
        mLabelDurationTotal = new Label();
        mLabelDurationAvg = new Label();
        mLabelDurationLongest = new Label();
        vBox.getChildren().addAll(label, mLabelBuilds, mLabelDurationTotal, mLabelDurationAvg, mLabelDurationLongest);
        return vBox;
    }

    private HBox makeBottom() {
        HBox hBox = new HBox(10);
        Button btnGoBottom = new Button("GO!");
        btnGoBottom.setOnAction(event -> {

            mNumberOfBuilds = 0;
            mDurationTotal = 0;
            mDurationLongest = 0;

            File dir = new File(mTextFieldDirPath.getText());

            for (File file : getFilesFromDirectory(dir.getPath())) {
                processFile(file);
            }

            System.out.println(dir.getPath());
            mLabelBuilds.setText("Builds: " + mNumberOfBuilds);
            mLabelDurationTotal.setText("Total: " + ((float) mDurationTotal / (float) 1000) + " sec");
            mLabelDurationAvg.setText("Average: " + ((float) mDurationTotal / (float) mNumberOfBuilds) + " ms");
            mLabelDurationLongest.setText("Longest: " + mDurationLongest + " ms");
        });

        hBox.getChildren().addAll(btnGoBottom, makeButtonShowDialog());
        return hBox;
    }

    private void processFile(File file) {
        String content = getContent(file);
        if (content == null || content.isEmpty()) return;
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.contains("ms")) {
                try {
                    int duration = Integer.parseInt(line.split("ms")[0].replace(" ", ""));
                    if (duration > mDurationLongest) {
                        mDurationLongest = duration;
                    }
                    mDurationTotal += duration;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        mNumberOfBuilds++;
    }

    private String getContent(File file) {
        try {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
                return sb.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setUpBorderPane(BorderPane borderPane) {
        borderPane.setTop(makeTop());
        borderPane.setCenter(makeCenter());
        borderPane.setBottom(makeBottom());
    }

    private Button makeButtonShowDialog() {
        Button btnShowDialog = new Button("SHOW CODE");
        btnShowDialog.setOnAction(event -> {
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Nest this code in build.gradle");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            Button btnOk = new Button("OK");
            btnOk.setOnAction(event1 -> dialogStage.close());

            TextArea textArea = new TextArea(getGradleCode());
            VBox vbox = new VBox(15, textArea, btnOk);

            vbox.setAlignment(Pos.CENTER);
            vbox.setPadding(new Insets(15));
            dialogStage.setScene(new Scene(vbox));
            dialogStage.show();
            textArea.selectAll();
            textArea.setEditable(false);
        });
        return btnShowDialog;
    }

    private File[] getFilesFromDirectory(String dirPath) {
        File folder = new File(dirPath);
        return folder.listFiles();
    }

    private String getGradleCode() {
        return "class TimingsListener implements TaskExecutionListener, BuildListener {\n" +
                "\n" +
                "    private static final DIR_NAME = \"buildings\"\n" +
                "    private Clock clock\n" +
                "    private timings = []\n" +
                "\n" +
                "    @Override\n" +
                "    void beforeExecute(Task task) {\n" +
                "        clock = new org.gradle.util.Clock()\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    void afterExecute(Task task, TaskState taskState) {\n" +
                "        def ms = clock.timeInMs\n" +
                "        timings.add([ms, task.path])\n" +
                "        task.project.logger.warn \"${task.path} took ${ms}ms\"\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    void buildFinished(BuildResult result) {\n" +
                "        File outDir = new File(DIR_NAME)\n" +
                "        if (!outDir.exists()) {\n" +
                "            outDir.mkdirs();\n" +
                "        }\n" +
                "        File outFileCur = new File(DIR_NAME + \"/out\" + System.currentTimeMillis() + \".txt\");\n" +
                "        String s = \"------BUILD PROTOCOL------\\n\"\n" +
                "\n" +
                "        println \"Task timings:\"\n" +
                "        for (timing in timings) {\n" +
                "            if (timing[0] >= 30) {\n" +
                "\n" +
                "                printf \"%7sms  %s\\n\", timing\n" +
                "\n" +
                "                s += String.format(\"%8s ms \\t %s\\n\", timing[0], timing[1])\n" +
                "\n" +
                "            }\n" +
                "        }\n" +
                "        outFileCur.write(s)\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    void buildStarted(Gradle gradle) {}\n" +
                "\n" +
                "    @Override\n" +
                "    void projectsEvaluated(Gradle gradle) {}\n" +
                "\n" +
                "    @Override\n" +
                "    void projectsLoaded(Gradle gradle) {}\n" +
                "\n" +
                "    @Override\n" +
                "    void settingsEvaluated(Settings settings) {}\n" +
                "}\n" +
                "\n" +
                "gradle.addListener new TimingsListener()";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
