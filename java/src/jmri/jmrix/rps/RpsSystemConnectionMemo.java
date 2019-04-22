package jmri.jmrix.rps;

import java.util.ResourceBundle;
import javax.annotation.Nonnull;
import jmri.jmrix.SystemConnectionMemo;
import jmri.InstanceManager;
import jmri.SensorManager;
import jmri.jmrix.rps.serial.SerialAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal implementation of SystemConnectionMemo.
 *
 * @author Randall Wood randall.h.wood@alexandriasoftware.com
 */
public class RpsSystemConnectionMemo extends SystemConnectionMemo {

    public RpsSystemConnectionMemo(@Nonnull String prefix, @Nonnull String name) {
        super(prefix, name);
        register(); // registers general type
        InstanceManager.store(this, RpsSystemConnectionMemo.class); // also register as specific type

        // create and register the ComponentFactory
        InstanceManager.store(new jmri.jmrix.rps.swing.RpsComponentFactory(this),
                jmri.jmrix.swing.ComponentFactory.class);

        log.debug("Created RpsSystemConnectionMemo with prefix {}", prefix);
    }

    public RpsSystemConnectionMemo() {
        this("R", "RPS"); // Product name NOI18N
        log.debug("Created nameless RpsSystemConnectionMemo");
    }

    /**
     * No separate TrafficController used in RPS
     */
    void setRpsAdapter(SerialAdapter sa) {
        serialAdapter = sa;
    }

    private SerialAdapter serialAdapter;

    @Override
    protected ResourceBundle getActionModelResourceBundle() {
        return null;
    }

    public void configureManagers() {
        InstanceManager.setSensorManager(getSensorManager());
        InstanceManager.setReporterManager(getReporterManager());
    }

    /**
     * Provide access to the Sensor Manager for this particular connection.
     */
    private RpsSensorManager sensorManager = null;

    public RpsSensorManager getSensorManager() {
        if (getDisabled()) {
            return null;
        }
        if (sensorManager == null) {
            sensorManager = new RpsSensorManager(this); // should we provide serialAdapter as well?
        }
        return sensorManager;
    }

    /**
     * Provide access to the Reporter Manager for this particular connection.
     */
    private RpsReporterManager reporterManager = null;

    public RpsReporterManager getReporterManager() {
        if (getDisabled()) {
            return null;
        }
        if (reporterManager == null) {
            reporterManager = new RpsReporterManager(this); // should we provide serialAdapter as well?
        }
        return reporterManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Class<?> T) {
        if (getDisabled()) {
            return null;
        }
        if (T.equals(jmri.SensorManager.class)) {
            return (T) getSensorManager();
        }
        return super.get(T);
    }

    private final static Logger log = LoggerFactory.getLogger(RpsSystemConnectionMemo.class);

}
