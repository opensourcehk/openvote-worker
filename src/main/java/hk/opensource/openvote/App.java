package hk.opensource.openvote;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import com.rabbitmq.client.ShutdownSignalException;
//import java.util.Date;
//import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import hk.opensource.openvote.OpenvoteWorker;

import org.apache.log4j.PropertyConfigurator;

public class App {

    Logger logger = LogManager.getLogger(App.class.getName());
    private Properties configFile;
    private int workerNum = 0;

    public App(String propertyFile) throws Exception {
        configFile = readConfig(propertyFile);
        String log4jConfigPath = configFile.getProperty("log4j_config_file");
        if (log4jConfigPath == null || log4jConfigPath.equals("")) {
            System.out.println("log4j_config_file could not be empty in the config file");
            System.exit(1);
        }
        PropertyConfigurator.configure(log4jConfigPath);
        workerNum = Integer.parseInt(configFile.getProperty("num_of_worker"));
    }

    public void start() {
        try {
            logger.info("Server START");
            this.startOpenvote();
        } catch (Exception e) {
            logger.error("Error: " + e.getMessage(), e);
        } finally {
            logger.info("Server STOP");
        }
    }

    private void startOpenvote() throws Exception {
                for (int i = 0; i < workerNum; i++) {
                    String n = "openvote "+i;
                    //System.out.println(n);
                    OpenvoteWorker p = new OpenvoteWorker(configFile, n);
                    p.connect();
                    new Thread(p).start();
                }
    }

    public Properties readConfig(String propertyFile) throws Exception {
        Properties configFile = new Properties();
        if(!new File(propertyFile).exists()){
                throw new Exception("the config file doesn't exist." +
                                " Make sure the file : \""+propertyFile+"\" exists.");
        }
        configFile.load(new FileInputStream(propertyFile));
        return configFile;
    }

    public static void main(String [] args) throws Exception {
        Logger logger = LogManager.getLogger(App.class.getName());
        App app = null;
        int retrytime = 0;
        try {
            if (args.length != 1) {
                System.out.println("Usage: configfile");
                System.exit(1);
            } else {
                app = new App(args[0]);
                app.start();
            }
        } catch (ShutdownSignalException e) {
            if (retrytime < 10) {
                if (app != null) {
                    Thread.sleep(1000);
                    app.start(); 
                }
            } else {
                System.out.println("Error: 10 Times retry. Stopped");
                System.exit(1);
            }
        } catch (Exception e) {
            logger.error("Error: " + e.getMessage(), e);
            logger.info("Error: " + e.getMessage() + ". Server Stopped!", e);
            System.exit(1);
        }
    }

}
