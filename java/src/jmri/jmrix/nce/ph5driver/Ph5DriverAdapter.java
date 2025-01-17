package jmri.jmrix.nce.ph5driver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import jmri.jmrix.nce.NcePortController;
import jmri.jmrix.nce.NceSystemConnectionMemo;
import jmri.jmrix.nce.NceTrafficController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import purejavacomm.CommPortIdentifier;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.SerialPort;
import purejavacomm.UnsupportedCommOperationException;

/**
 * Implements SerialPortAdapter for the NCE system.
 * <p>
 * This connects an NCE command station via a serial com port. Normally
 * controlled by the SerialDriverFrame class.
 *
 * @author Bob Jacobsen Copyright (C) 2001, 2002
 * @author Ken Cameron Copyright (C) 2013, 2023
 */
public class Ph5DriverAdapter extends NcePortController {

    SerialPort activeSerialPort = null;

    public Ph5DriverAdapter() {
        super(new NceSystemConnectionMemo());
        option1Name = "Eprom"; // NOI18N
        // the default is 2023 or later
        options.put(option1Name, new Option("Command Station EPROM", new String[]{"2023 or later"}));
        // TODO I18N
        setManufacturer(jmri.jmrix.nce.NceConnectionTypeList.NCE);
    }

    @Override
    public String openPort(String portName, String appName) {
        // open the port, check ability to set moderators
        try {
            // get and open the primary port
            CommPortIdentifier portID = CommPortIdentifier.getPortIdentifier(portName);
            try {
                activeSerialPort = (SerialPort) portID.open(appName, 2000);  // name of program, msec to wait
            } catch (PortInUseException p) {
                return handlePortBusy(p, portName, log);
            }

            // try to set it for communication via SerialDriver
            try {
                // find the baud rate value, configure comm options
                int baud = currentBaudNumber(mBaudRate);
                activeSerialPort.setSerialPortParams(baud, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            } catch (UnsupportedCommOperationException e) {
                log.error("Cannot set serial parameters on port {}: {}", portName, e.getMessage());
                return "Cannot set serial parameters on port " + portName + ": " + e.getMessage();
            }

            // disable flow control; hardware lines used for signaling, XON/XOFF might appear in data
            configureLeadsAndFlowControl(activeSerialPort, 0);
            activeSerialPort.enableReceiveTimeout(50);  // 50 mSec timeout before sending chars

            // set timeout
            // activeSerialPort.enableReceiveTimeout(1000);
            log.debug("Serial timeout was observed as: {} {}", activeSerialPort.getReceiveTimeout(), activeSerialPort.isReceiveTimeoutEnabled());

            // get and save stream
            serialStream = activeSerialPort.getInputStream();

            // purge contents, if any
            purgeStream(serialStream);

            // report status
            if (log.isInfoEnabled()) {
                log.info("NCE {} port opened at {} baud", portName, activeSerialPort.getBaudRate());
            }
            opened = true;

        } catch (NoSuchPortException p) {
            return handlePortNotFound(p, portName, log);
        } catch (UnsupportedCommOperationException | IOException ex) {
            log.error("Unexpected exception while opening port {}", portName, ex);
            return "Unexpected error while opening port " + portName + ": " + ex;
        }

        return null; // indicates OK return
    }

    /**
     * Set up all of the other objects to operate with an NCE command station
     * connected to this port.
     */
    @Override
    public void configure() {
        NceTrafficController tc = new NceTrafficController();
        this.getSystemConnectionMemo().setNceTrafficController(tc);
        tc.setAdapterMemo(this.getSystemConnectionMemo());

        this.getSystemConnectionMemo().configureCommandStation(NceTrafficController.OPTION_PH5);
        this.getSystemConnectionMemo().setNceCmdGroups(~NceTrafficController.CMDS_USB);
        tc.connectPort(this);

        this.getSystemConnectionMemo().configureManagers();
        tc.csm = new Ph5CmdStationMemory();
    }

    // base class methods for the NcePortController interface

    @Override
    public DataInputStream getInputStream() {
        if (!opened) {
            log.error("getInputStream called before load(), stream not available");
            return null;
        }
        return new DataInputStream(serialStream);
    }

    @Override
    public DataOutputStream getOutputStream() {
        if (!opened) {
            log.error("getOutputStream called before load(), stream not available");
        }
        try {
            return new DataOutputStream(activeSerialPort.getOutputStream());
        } catch (java.io.IOException e) {
            log.error("getOutputStream exception", e);
        }
        return null;
    }

    @Override
    public boolean status() {
        return opened;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] validBaudRates() {
        return Arrays.copyOf(validSpeeds, validSpeeds.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] validBaudNumbers() {
        return Arrays.copyOf(validSpeedValues, validSpeedValues.length);
    }

    private String[] validSpeeds = new String[]{Bundle.getMessage("Baud9600")};
    private int[] validSpeedValues = new int[]{9600};

    @Override
    public int defaultBaudIndex() {
        return 0;
    }

    // private control members
    private boolean opened = false;
    InputStream serialStream = null;

    private final static Logger log = LoggerFactory.getLogger(Ph5DriverAdapter.class);

}
