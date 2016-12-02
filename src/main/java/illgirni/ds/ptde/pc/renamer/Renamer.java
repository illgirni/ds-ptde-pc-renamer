package illgirni.ds.ptde.pc.renamer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Locale;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class Renamer extends Application {
    private static int[] slotOffsets = new int[10];
    
    static {
        for (int i = 0; i < 10; i++) {
            slotOffsets[i] = 704 + (i * 393248);
        }
    }
    
    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        Application.launch(Renamer.class);
    }
    
    private SimpleStringProperty saveFilePath = new SimpleStringProperty();
    
    private ComboBox<SaveSlot> slotsField;
    
    private TextField nameField;
    
    private Text renameConfirmText = new Text();
    
    private byte[] saveFileData;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        final VBox content = new VBox();
        content.setSpacing(20);
        content.setPadding(new Insets(20));
        
        final HBox fileBox = new HBox(10);
        final TextField fileNameField = new TextField();
        fileNameField.setEditable(false);
        fileNameField.setPrefWidth(500);
        fileNameField.textProperty().bind(saveFilePath);
        fileBox.getChildren().add(fileNameField);
        
        final FileChooser saveFileChooser = new FileChooser();
        saveFileChooser.getExtensionFilters().add(new ExtensionFilter("DS1 PTDE Save File", "*.sl2"));
        
        final Button selectSaveFileButton = new Button("Select...");
        selectSaveFileButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(final ActionEvent event) {
                final File fileSelection = saveFileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
                
                if (fileSelection != null) {
                    saveFilePath.set(fileSelection.getAbsolutePath());
                }
            }
        });
        
        fileBox.getChildren().add(selectSaveFileButton);
        
        final Button loadSaveFileButton = new Button("Load");
        loadSaveFileButton.setDisable(true);
        loadSaveFileButton.setOnAction((event) -> load());
        loadSaveFileButton.disableProperty().bind(saveFilePath.isEmpty());
        fileBox.getChildren().add(loadSaveFileButton);
        
        content.getChildren().add(fileBox);
        
        final HBox slotBox = new HBox();
        slotBox.setSpacing(10);
        
        slotsField = new ComboBox<>();
        slotsField.setCellFactory(listView -> new SaveSlotComboBoxCell());
        slotsField.setButtonCell(slotsField.getCellFactory().call(null)); //this is important!
        slotsField.getSelectionModel().selectedItemProperty().addListener((obs, oldSlot, newSlot) -> {
            if (newSlot == null) {
                nameField.setText("");
            } else {
                nameField.setText(newSlot.getCharacterName());
            }
        });
        slotBox.getChildren().add(slotsField);
        
        
        content.getChildren().add(slotBox);
        
        content.getChildren().add(new Text("Load the renamed character in game to show new name on the character selection screen!"));
        
        final HBox nameBox = new HBox(10);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        nameBox.getChildren().add(new Text("New Name: "));
        
        nameField = new TextField();
        nameBox.getChildren().add(nameField);
        
        final Text nameLengthText = new Text("0 / 13");
        nameField.textProperty().addListener((obs, oldVal, newVal) -> { 
            if(newVal == null) { 
                nameLengthText.setText("0 / 13"); 
            } else { 
                nameLengthText.setText(newVal.length() + " / 13"); 
            }
        });
        nameBox.getChildren().add(nameLengthText);
        
        content.getChildren().add(nameBox);
        
        final HBox renameBox = new HBox(10);
        renameBox.setAlignment(Pos.CENTER_LEFT);
        
        final Button renameButton = new Button("Rename");
        renameButton.setDisable(true);
        renameButton.setOnAction((event) -> rename());
        renameButton.disableProperty().bind(slotsField.getSelectionModel().selectedItemProperty().isNull()
                                                      .or(nameField.textProperty().isEmpty()
                                                                   .or(nameField.textProperty().length().greaterThan(13))));
        renameBox.getChildren().add(renameButton);
        renameBox.getChildren().add(renameConfirmText);
        
        content.getChildren().add(renameBox);
        
        final Scene scene = new Scene(content);
        primaryStage.setTitle("DS:PTDE [PC] Character Renamer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private void load() {
        try {
            slotsField.getSelectionModel().clearSelection();
            slotsField.getItems().clear();
            saveFileData = Files.readAllBytes(new File(saveFilePath.get()).toPath());
            nameField.setText(null);
            
            for (int i = 1; i <= 10; i++) {
                final SaveSlot saveSlot = new SaveSlot(i, slotOffsets[i-1], saveFileData);
                
                if (saveSlot.getCharacterName() != null && !saveSlot.getCharacterName().isEmpty()) {
                    slotsField.getItems().add(saveSlot);
                    //System.out.println(saveSlot.getCharacterName());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading.", e);
        }
        
    }
    
    private void rename() {
        try {
            final SaveSlot selectedSlot = slotsField.getSelectionModel().getSelectedItem();
            selectedSlot.rename(saveFileData, nameField.getText());
            Files.write(new File(saveFilePath.get()).toPath(), saveFileData);
            renameConfirmText.setText("Renamed to '" + nameField.getText() + "'");
            load();
            
        } catch (NoSuchAlgorithmException | IOException  e) {
            throw new RuntimeException("Error saving.", e);
        }
    }

    private class SaveSlotComboBoxCell extends ListCell<SaveSlot> {
        @Override
        protected void updateItem(SaveSlot item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            
            if (empty) {
                setGraphic(null);
            } else if (item == null) {
                setGraphic(new Text("Please choose..."));
            } else {
                final Text label = new Text();
                final String displayedIndex = new DecimalFormat("00").format(item.getIndex());
                
                label.setText(displayedIndex + ". " + item.getCharacterName());
                
                setGraphic(label);
            }
            
        }
    }
}
