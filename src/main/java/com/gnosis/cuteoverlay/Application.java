package com.gnosis.cuteoverlay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Date;

public class Application extends javafx.application.Application {

    private @FXML
    Label label_cpu_temp;

    private @FXML
    Label label_cpu_load;

    private @FXML
    Label label_gpu_fan_load;

    private @FXML
    Label label_gpu_temp;

    private @FXML
    Label label_gpu_load;

    private @FXML
    Label label_mem_load;

    private @FXML
    Label label_cpu_temp_title;

    private @FXML
    Label label_cpu_load_title;

    private @FXML
    Label label_gpu_temp_title;

    private @FXML
    Label label_gpu_load_title;

    private @FXML
    Label label_mem_load_title;

    private @FXML
    Label label_gpu_fan_load_title;

    private @FXML
    Label label_application_title;

    private @FXML
    ImageView image_view_exit;

    private @FXML
    ImageView image_view_switch;

    private @FXML
    VBox vbox_inner_pane;

    private boolean bgActive = true;
    private double initialX;
    private double initialY;

    public static void tieSystemOutAndErrToLog() {
        System.setOut(createLoggingProxy(System.out));
        System.setErr(createLoggingProxy(System.err));
    }

    public static PrintStream createLoggingProxy(final PrintStream realPrintStream) {
        return new PrintStream(realPrintStream) {
            public void print(final String string) {
                realPrintStream.print(string);
                try {
                    File yourFile = new File("log.log");
                    if (!yourFile.exists()) {
                        yourFile.createNewFile();
                    }
                    Files.write(
                            Paths.get("log.log"),
                            (string + System.lineSeparator()).getBytes(),
                            StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

    }

    public static void main(String[] args) {
        System.setProperty("io.netty.tryReflectionSetAccessible", "true");
        tieSystemOutAndErrToLog();
        System.out.println("-----");
        System.out.println("Uptime:" + ManagementFactory.getRuntimeMXBean().getUptime() + "ms");
        System.out.println("StartTime:" + new Date(ManagementFactory.getRuntimeMXBean().getStartTime()));

        javafx.application.Application.launch(Application.class, args);
    }

    public void createServer() {
        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);
        Format tempFormat = new SensorFormat() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                return super.format(obj, toAppendTo, pos).append("C");
            }
        };
        Format loadFormat = new SensorFormat() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                return super.format(obj, toAppendTo, pos).append("%");
            }
        };
        SimpleStringProperty cpuTempProperty = new SimpleStringProperty("0");
        SimpleStringProperty gpuTempProperty = new SimpleStringProperty("0");
        SimpleStringProperty gpuLoadProperty = new SimpleStringProperty("0");
        SimpleStringProperty memLoadProperty = new SimpleStringProperty("0");
        SimpleStringProperty cpuLoadProperty = new SimpleStringProperty("0");
        SimpleStringProperty gpuFanLoadProperty = new SimpleStringProperty("0");


        Bindings.bindBidirectional(label_cpu_temp.textProperty(), cpuTempProperty, tempFormat);
        Bindings.bindBidirectional(label_gpu_temp.textProperty(), gpuTempProperty, tempFormat);
        Bindings.bindBidirectional(label_gpu_load.textProperty(), gpuLoadProperty, loadFormat);
        Bindings.bindBidirectional(label_mem_load.textProperty(), memLoadProperty, loadFormat);
        Bindings.bindBidirectional(label_cpu_load.textProperty(), cpuLoadProperty, loadFormat);
        Bindings.bindBidirectional(label_gpu_fan_load.textProperty(), gpuFanLoadProperty, loadFormat);


        router.get("/readAllData").handler(routingContext -> {
            MultiMap params = routingContext.request().params();
            String data = params.get("data");
            try {
                DataToSendContainer dataToSendContainer = new ObjectMapper().readValue(data, DataToSendContainer.class);
                for (DataToSend datum : dataToSendContainer.getData()) {
                    //System.out.println(datum.toString());
                    String name = datum.getName();
                    String sensorType = datum.getSensorType();
                    String value = datum.getValue();

                    if (name.equals("CPU Package") && sensorType.equals("Temperature")) {
                        Platform.runLater(() -> cpuTempProperty.setValue(value));
                    } else if (name.equals("GPU Core") && sensorType.equals("Temperature")) {
                        Platform.runLater(() -> gpuTempProperty.setValue(value));
                    } else if (name.equals("GPU Core") && sensorType.equals("Load")) {
                        Platform.runLater(() -> gpuLoadProperty.setValue(value));
                    } else if (name.equals("Memory") && sensorType.equals("Load")) {
                        Platform.runLater(() -> memLoadProperty.setValue(value));
                    } else if (name.equals("CPU Total") && sensorType.equals("Load")) {
                        Platform.runLater(() -> cpuLoadProperty.setValue(value));
                    } else if (name.equals("GPU Fan") && sensorType.equals("Control")) {
                        Platform.runLater(() -> gpuFanLoadProperty.setValue(value));
                    }

                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });

        vertx.createHttpServer(new HttpServerOptions().setMaxInitialLineLength(Integer.MAX_VALUE))
                .requestHandler(router)
                .listen(6666);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.initStyle(StageStyle.UTILITY);
        primaryStage.setOpacity(0);
        primaryStage.setHeight(0);
        primaryStage.setWidth(0);
        primaryStage.show();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Parent parent = fxmlLoader.load();
        Stage loadingStage = new Stage();
        loadingStage.initOwner(primaryStage);
        Scene scene = new Scene(parent);
        loadingStage.centerOnScreen();
        loadingStage.initStyle(StageStyle.TRANSPARENT);
        loadingStage.setAlwaysOnTop(true);
        loadingStage.setScene(scene);
        loadingStage.setResizable(false);
        loadingStage.setX(200);
        loadingStage.setY(200);
        loadingStage.show();
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double x = bounds.getMinX() + (bounds.getWidth() - scene.getWidth()) * 0.985;
        double y = bounds.getMinY() + (bounds.getHeight() - scene.getHeight()) * 0.025;
        loadingStage.setX(x);
        loadingStage.setY(y);
        scene.getStylesheets().add(getClass().getResource("/fontStyle.css").toExternalForm());
        scene.setFill(Color.TRANSPARENT);

        addDraggableNode(scene.getRoot());
    }

    public void initialize() {
        createServer();

        try {
            Runtime.getRuntime().exec("cmd /c HWINFOConsole.exe");
        } catch (IOException e) {
            e.printStackTrace();
        }
        DropShadow ds = new DropShadow();
        ds.setRadius(10.0f);
        ds.setOffsetY(0.0f);
        ds.setBlurType(BlurType.GAUSSIAN);
        ds.setColor(Color.color(0.4f, 0.4f, 0.4f));

        label_application_title.setEffect(ds);
        Arrays.asList(label_cpu_temp, label_cpu_load, label_gpu_temp, label_gpu_load, label_mem_load, label_gpu_fan_load,
                label_cpu_temp_title, label_cpu_load_title, label_gpu_temp_title, label_gpu_load_title, label_mem_load_title, label_gpu_fan_load_title)
                .forEach(label -> label.setEffect(ds));
        vbox_inner_pane.setEffect(ds);
        image_view_exit.setOnMouseClicked(mouseEvent -> {
            try {
                System.exit(-1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        image_view_switch.setOnMouseClicked(mouseEvent -> {
            image_view_switch.setImage(new Image(getClass().getResourceAsStream("/switch-" + bgActive + ".png")));
            vbox_inner_pane.setStyle(bgActive ? " -fx-background-color: linear-gradient(from 45px 45px to 50px 50px, reflect,  #0E0E10 50%, black  0%); -fx-border-color: #1aa260;" : "");
            bgActive = !bgActive;
        });

        MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = mbean.getHeapMemoryUsage();
        System.out.println("Max heap memory is " + heapUsage.getMax() / 1024 / 1024 + " MBytes");

        MemoryUsage nonHeapUsage = mbean.getNonHeapMemoryUsage();
        System.out.println("Used non-heap memory is " + nonHeapUsage.getUsed() / 1024 / 1024 + " MBytes");

    }

    private void addDraggableNode(final Node node) {
        node.setOnMousePressed(me -> {
            if (me.getButton() != MouseButton.MIDDLE) {
                initialX = me.getSceneX();
                initialY = me.getSceneY();
            }
        });
        node.setOnMouseDragged(me -> {
            if (me.getButton() != MouseButton.MIDDLE) {
                node.getScene().getWindow().setX(me.getScreenX() - initialX);
                node.getScene().getWindow().setY(me.getScreenY() - initialY);
            }
        });
    }

    public static class SensorFormat extends Format {
        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj == null) {
                obj = "0";
            }
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMinimumFractionDigits(0);
            nf.setMaximumFractionDigits(0);
            nf.setMinimumIntegerDigits(2);
            nf.setMaximumIntegerDigits(2);
            double number = Double.parseDouble(obj.toString());
            if (number > 99) {
                number = 99.0;
            }

            return toAppendTo.append(nf.format(number));
        }

        @Override
        public Object parseObject(String source, ParsePosition pos) {
            return null;
        }
    }
}
