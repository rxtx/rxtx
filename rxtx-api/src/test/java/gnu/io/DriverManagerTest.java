package gnu.io;

import gnu.io.spi.CommDriver;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.net.URL;
import junit.framework.TestCase;

/**
 * @author Alexander Graf [alex -at- antistatix.de]
 */
public class DriverManagerTest extends TestCase {

    public DriverManagerTest(String testName) {
        super(testName);
    }

    /**
     * Registers a service implementation for the CommDriver interface. This is
     * done by placing a temporary file into META-INF/services.
     *
     * @param implementation the implementation class to register
     * @throws Exception various io stuff
     */
    public static void installMockServiceImpl(Class implementation)
            throws Exception {
        URL location = implementation
                .getProtectionDomain().getCodeSource().getLocation();
        File file = new File(
                location.getPath()
                + "META-INF/services/"
                + CommDriver.class.getName());
        file.getParentFile().mkdirs();

        Writer output = new FileWriter(file);
        output.write(implementation.getName());
        output.close();

        file.deleteOnExit();
    }

    /**
     * Some mock driver implementation which can tell whether it was
     * initialized.
     */
    public static class MockCommDriver implements CommDriver {

        private boolean initializeCalled = false;
        public static MockCommDriver lastInstance;

        public MockCommDriver() {
            lastInstance = this;
        }

        @Override
        public CommPort getCommPort(String portName, int portType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void initialize(DriverContext context) {
            initializeCalled = true;
        }

        public boolean isInitializeCalled() {
            return initializeCalled;
        }

        public void resetInitialized() {
            initializeCalled = false;
        }
    }

    /**
     * Another mock driver implementation which can tell whether it was
     * initialized.
     */
    public static class MockCommDriver2 implements CommDriver {

        private boolean initializeCalled = false;
        public static MockCommDriver2 lastInstance;

        public MockCommDriver2() {
            lastInstance = this;
        }

        @Override
        public CommPort getCommPort(String portName, int portType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void initialize(DriverContext context) {
            initializeCalled = true;
        }

        public boolean isInitializeCalled() {
            return initializeCalled;
        }

        public void resetInitialized() {
            initializeCalled = false;
        }
    }

    /**
     * A test case which tests the installation of CommDrivers via the java SE6
     * ServiceLoader mechanism.
     *
     * @throws Exception
     */
    public void testLoadDrivers() throws Exception {
        // two untouched mockups
        assertNull(MockCommDriver.lastInstance);
        assertNull(MockCommDriver2.lastInstance);

        // loading drivers if none are available should silently return
        DriverManager.getInstance().loadDrivers();

        // install the first and reload drivers
        installMockServiceImpl(MockCommDriver.class);
        DriverManager.getInstance().loadDrivers();

        // the first driver should now be available and initialized
        assertNotNull(MockCommDriver.lastInstance);
        assertTrue(MockCommDriver.lastInstance.isInitializeCalled());

        // remember the installed driver
        MockCommDriver firstInstance = MockCommDriver.lastInstance;
        MockCommDriver.lastInstance.resetInitialized();

        // load the drivers again
        DriverManager.getInstance().loadDrivers();

        // the driver should NOT be reinitialized
        assertFalse(MockCommDriver.lastInstance.isInitializeCalled());

        // now register the second driver and reload
        installMockServiceImpl(MockCommDriver2.class);
        DriverManager.getInstance().loadDrivers();

        // a second driver is now available and initialized
        assertNotNull(MockCommDriver2.lastInstance);
        assertTrue(MockCommDriver2.lastInstance.isInitializeCalled());
    }
}
