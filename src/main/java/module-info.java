module com.example.chargedparticles {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.rmi;

    opens com.example.chargedparticles to javafx.fxml;
    exports com.example.chargedparticles;
    exports com.example.chargedparticles.distributed;
}