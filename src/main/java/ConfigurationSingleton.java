import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class ConfigurationSingleton {
    private static ConfigurationSingleton instance;
    private PropertiesConfiguration config;

    private ConfigurationSingleton(){
        Configurations configs = new Configurations();
        try {
            config = configs.properties("config.properties");
        } catch (ConfigurationException e) {
            System.err.println("ERRORE FILE NON ESISTE: " + e.getMessage());
            System.exit(-1);
        }
    }

    public static ConfigurationSingleton getInstance(){
        if(instance == null)
            instance = new ConfigurationSingleton();
        return instance;
    }

    public String getProperty(String key){
        return config.getString(key);
    }
}