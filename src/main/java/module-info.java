module com.example.musicplayer {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires java.xml;
    requires jaudiotagger;
    requires java.desktop;
    requires javafx.media;
    requires java.logging;

    opens com.niceferrari.musicplayer to javafx.fxml;
    exports com.niceferrari.musicplayer;
}