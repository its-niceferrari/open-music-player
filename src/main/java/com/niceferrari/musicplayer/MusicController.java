package com.niceferrari.musicplayer;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.awt.image.BufferedImage;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

public class MusicController {

    public Stage primaryStage = new Stage();
    @FXML
    public Label songLabel;
    @FXML
    public Label artistLabel;
    @FXML
    ImageView coverArtImageView;
    @FXML
    ListView<String> directoryList;
    @FXML
    Button playButton;
    @FXML
    Slider playheadSlider;

    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private File openFile;

    @FXML
    public void openButtonClicked() {
        // Create a file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Music");

        // Show open file dialog
        openFile = fileChooser.showOpenDialog(primaryStage);

        // Pause the media player if it is playing
        if (isPlaying) {
            // Pause audio
            mediaPlayer.pause();
            isPlaying = false;
            System.out.println("Song paused.");
            playButton.setText("Play");
        }

        // Stop the media player if it is not null
        if (mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer = null;
        }

        // Process the selected file
        if (openFile != null) {
            System.out.println("Selected music file: " + openFile.getAbsolutePath());

            // Extract song and artist information
            String[] songInfo = extractSongInfo(openFile);

            // Update the label
            updateSongInfoLabel(songInfo[0], songInfo[1]);
        } else {
            System.out.println("Music file selection canceled.");
        }

        updatePlaylistFromSameDirectory();
    }

    public String[] extractSongInfo(File file) {
        try {
            // Read audio file metadata using JAudioTagger
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();

            if (tag == null) {
                return new String[]{"Unknown Song", "Unknown Artist"};
            }

            // Extract song and artist information from metadata
            String songName = tag.getFirst(FieldKey.TITLE);
            String artistName = tag.getFirst(FieldKey.ARTIST);

            if (Objects.equals(tag.getFirst(FieldKey.TITLE), "")) {
                songName = file.getName();
            }
            if (Objects.equals(tag.getFirst(FieldKey.ARTIST), "")) {
                artistName = "Unknown Artist";
            }

            setCoverArt(file);

            return new String[]{songName, artistName};
        } catch (Exception e) {
            System.out.println("Error reading audio file metadata: " + e.getMessage());
            return new String[]{"Unknown Song", "Unknown Artist"};
        }
    }

    private void setCoverArt(File file) {
        try {
            // Read audio file metadata using JAudioTagger
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();

            // Extract cover art
            Artwork artwork = tag.getFirstArtwork();

            if (artwork != null) {
                // Convert BufferedImage to JavaFX Image
                java.awt.image.BufferedImage bufferedImage = (BufferedImage) artwork.getImage();
                int width = bufferedImage.getWidth();
                int height = bufferedImage.getHeight();

                WritableImage writableImage = new WritableImage(width, height);
                PixelWriter pixelWriter = writableImage.getPixelWriter();

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        pixelWriter.setArgb(x, y, bufferedImage.getRGB(x, y));
                    }
                }

                // Set the image to the ImageView
                coverArtImageView.setImage(writableImage);
            } else {
                // Set a null image if no cover art is available
                coverArtImageView.setImage(null);
            }
        } catch (Exception e) {
            System.out.println("Error reading cover art: " + e.getMessage());
        }
    }

    private void updatePlaylistFromSameDirectory() {
        if (openFile != null) {
            // Extract the directory from the currently selected song's path
            File currentSelectedSong = new File(openFile.getAbsolutePath());
            File directory = currentSelectedSong.getParentFile();

            if (directory != null && directory.isDirectory()) {
                // List music files in the same directory
                File[] musicFiles = directory.listFiles((dir, name) ->
                        name.toLowerCase().endsWith(".mp3") || name.toLowerCase().endsWith(".wav")
                                || name.toLowerCase().endsWith(".ogg") || name.toLowerCase().endsWith(".flac")
                || name.toLowerCase().endsWith(".aiff") || name.toLowerCase().endsWith(".m4a"));

                if (musicFiles != null) {
                    // Update the playlist ListView
                    directoryList.getItems().clear();

                    // Add file names to the list
                    Arrays.stream(musicFiles)
                            .map(File::getName)
                            .sorted() // Sort the names alphabetically
                            .forEach(directoryList.getItems()::add);
                }
            }
        }
    }

    private void updateSongInfoLabel(String songName, String artistName) {
        songLabel.setText(songName);
        artistLabel.setText(artistName);
    }

    public void playButtonClicked() {
        if (isPlaying) {
            // Pause audio
            mediaPlayer.pause();
            isPlaying = false;
            System.out.println("Song paused.");
            playButton.setText("Play");
        } else {
            if (mediaPlayer == null && openFile != null) {
                // Create a media player
                String audioFilePath = openFile.getAbsolutePath();
                File audioFile = new File(audioFilePath);

                if (audioFile.exists()) {
                    Media media = new Media(audioFile.toURI().toString());
                    mediaPlayer = new MediaPlayer(media);
                } else {
                    System.out.println("File does not exist.");
                    return;
                }
            }

            // Set the maximum value of the slider to the total duration of the media
            assert mediaPlayer != null;
            mediaPlayer.setOnReady(() -> {
                Duration totalDuration = mediaPlayer.getTotalDuration();
                playheadSlider.setMax(totalDuration.toMillis());
            });

            // Update the media player's current time when the slider value changes
            playheadSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (playheadSlider.isValueChanging()) {
                    mediaPlayer.seek(Duration.millis(newValue.doubleValue()));
                }
            });

            // Update the slider position as the media plays
            mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                if (!playheadSlider.isValueChanging()) {
                    playheadSlider.setValue(newValue.toMillis());
                }
            });

            // Reset the slider when the media player finishes playing
            mediaPlayer.statusProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == MediaPlayer.Status.STOPPED) {
                    playheadSlider.setValue(playheadSlider.getMin());
                    System.out.println("Song finished.");
                    isPlaying = false;
                    playButton.setText("Play");
                }
            });

           if (mediaPlayer != null) {
                // Play audio
                mediaPlayer.play();
                isPlaying = true;
                playButton.setText("Pause");
            }
        }
    }
}