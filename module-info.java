module healthcare.system {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    exports healthcare.gui;
    exports healthcare.model;
    exports healthcare.database;
    exports healthcare.exceptions;
    exports healthcare.utils;

    opens healthcare.gui to javafx.fxml;
}
