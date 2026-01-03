import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class SingletonConfiguration {

    private static SingletonConfiguration instance;
    private Configuration configuration;

    private SingletonConfiguration() {
        Configurations configurations = new Configurations();
        try {
            configuration = configurations.properties("config.properties");
        } catch (ConfigurationException e) {
            System.err.println("Nonexistent file.");
            System.exit(-1);
        }
    }

    public static SingletonConfiguration getInstance() {
        if (instance == null) {
            instance = new SingletonConfiguration();
        }
        return instance;
    }

    public String getProperty(String key) {
        return configuration.getString(key);
    }
}
