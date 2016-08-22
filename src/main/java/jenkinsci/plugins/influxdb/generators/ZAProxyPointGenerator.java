package jenkinsci.plugins.influxdb.generators;

import jenkins.MasterToSlaveFileCallable;

import org.influxdb.dto.Point;
import org.jdom.JDOMException;
import org.zaproxy.clientapi.core.Alert;
import org.zaproxy.clientapi.core.AlertsFile;

import hudson.FilePath;
import hudson.model.Run;

import java.io.File;
import java.io.IOException;
import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.List;


public class ZAProxyPointGenerator extends AbstractPointGenerator {

    private static final String ZAP_REPORT_FILE = "/target/ZAP/zap_report.html";
    private static final String HIGH_ALERTS = "high_alerts";
    private static final String MEDIUM_ALERTS = "medium_alerts";
    private static final String LOW_ALERTS = "low_alerts";
    private static final String INFORMATIONAL_ALERTS = "informational_alerts";

    private final Run<?, ?> build;
    private final FilePath zapFile;

    public ZAProxyPointGenerator(Run<?, ?> build, FilePath workspace) {
        this.build = build;
        zapFile = new FilePath(workspace, ZAP_REPORT_FILE);
    }

    public boolean hasReport() {
        try {
            return (zapFile != null && zapFile.exists());
        } catch (IOException|InterrupedException e) {
            // NOP
        }
        return false;
    }

    public Point[] generate() {
        try {
            List<int> ls = zapFile.act(new ZAPFileCallable());
            Point point = Point.measurement("zap_data")
                .field(BUILD_NUMBER, build.getNumber())
                .field(PROJECT_NAME, build.getParent().getName())
                .field(HIGH_ALERTS, ls.get(0))
                .field(MEDIUM_ALERTS, ls.get(1))
                .field(LOW_ALERTS, ls.get(2))
                .field(INFORMATIONAL_ALERTS, ls.get(3))
                .build();
            return new Point[] {point};
        } catch (IOException|InterruptedException e) {
            // NOP
        }
        return null;
    }

    private static final class ZAPFileCallable extends MasterToSlaveFileCallable<List<int>> {
        private static final long serialVersionUID = 1;

        @Override
        public List<int> invoke(File f, VirtualChannel channel) {
            List<int> ls = new ArrayList<int>();
            ls.add(AlertsFile.getAlertsFromFile(f, "high").size());
            ls.add(AlertsFile.getAlertsFromFile(f, "medium").size());
            ls.add(AlertsFile.getAlertsFromFile(f, "low").size());
            ls.add(AlertsFile.getAlertsFromFile(f, "informational").size());
            return ls;
        }
}
