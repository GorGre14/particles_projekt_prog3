module com.example.chargedparticles {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.example.chargedparticles to javafx.fxml;
    exports com.example.chargedparticles;
}